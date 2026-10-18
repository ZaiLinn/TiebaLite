package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.FlowDebouncer
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

interface OnFullScreenModeChangedListener {
    fun onFullScreenModeChanged(isFullScreen: Boolean)
}

internal class DefaultVideoPlayerController(
    private val context: Context,
    private val initialState: VideoPlayerState,
    private val coroutineScope: CoroutineScope,
    private val fullScreenModeChangedListener: OnFullScreenModeChangedListener? = null
) : VideoPlayerController {
    private val released = AtomicBoolean(false)

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<VideoPlayerState>
        get() = _state.asStateFlow()

    /**
     * Some properties in initial state are not applicable until player is ready.
     * These are kept in this container. Once the player is ready for the first time,
     * they are applied and removed.
     */
    private var initialStateRunner: (() -> Unit)? = {
        exoPlayer.seekTo(initialState.currentPosition)
    }

    fun <T> currentState(filter: (VideoPlayerState) -> T): T {
        return filter(_state.value)
    }

    @Composable
    fun collect(): State<VideoPlayerState> {
        return _state.collectAsState()
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun <T> collect(filter: VideoPlayerState.() -> T): State<T> {
        return remember(filter) {
            _state.map { it.filter() }
        }.collectAsState(
            initial = _state.value.filter()
        )
    }

    var videoPlayerBackgroundColor: Int = DefaultVideoPlayerBackgroundColor.value.toInt()
        set(value) {
            field = value
            playerView?.setBackgroundColor(value)
        }

    private lateinit var source: VideoPlayerSource
    private var playerView: PlayerView? = null

    private var updateDurationAndPositionJob: Job? = null
    private var autoHideControllerJob: Job? = null

    private val playerListener = object : Player.Listener {
        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (PlaybackState.of(playbackState) == PlaybackState.READY) {
                initialStateRunner = initialStateRunner?.let {
                    it.invoke()
                    null
                }

                // 暂停时不轮询进度，只刷新一次时长和位置；播放中的轮询由 onIsPlayingChanged 开关
                updateDurationAndPosition()
            }

            _state.set {
                copy(
                    playbackState = PlaybackState.of(playbackState),
                    startedPlay = playbackState != STATE_IDLE
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateDurationAndPositionJob?.cancel()
            updateDurationAndPositionJob = if (isPlaying) {
                coroutineScope.launch {
                    while (this.isActive) {
                        updateDurationAndPosition()
                        delay(250)
                    }
                }
            } else {
                updateDurationAndPosition()
                null
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _state.set {
                copy(isPlaying = playWhenReady)
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _state.set {
                copy(videoSize = videoSize.width.toFloat() to videoSize.height.toFloat())
            }
        }
    }

    /**
     * Internal exoPlayer instance
     */
    private var _exoPlayer: ExoPlayer? = null
    private val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                _exoPlayer = createExoPlayer()
            }
            return _exoPlayer!!
        }

    private fun createExoPlayer() = ExoPlayer.Builder(context)
        .build()
        .apply {
            playWhenReady = initialState.isPlaying
            addListener(playerListener)
        }

    /**
     * Not so efficient way of showing preview in video slider.
     */
    private var _previewExoPlayer: ExoPlayer? = null
    private val previewExoPlayer: ExoPlayer
        get() {
            if (_previewExoPlayer == null) {
                _previewExoPlayer = createPreviewExoPlayer()
            }
            return _previewExoPlayer!!
        }

    private fun createPreviewExoPlayer() = ExoPlayer.Builder(context)
        .build()
        .apply {
            playWhenReady = false
        }

    /** 预览播放器是否已载入当前 [source]；它只在拖动进度条时才创建和缓冲 */
    private var previewSourcePrepared = false

    private val previewSeekDebouncer = FlowDebouncer<Long>(200L)

    init {
        exoPlayer.playWhenReady = initialState.isPlaying

        coroutineScope.launch {
            previewSeekDebouncer.collect { position ->
                if (!released.get()) preparedPreviewPlayer().seekTo(position)
            }
        }
    }

    fun initialize() {
        Log.i("VideoPlayerController", "$this initialize")
        val currentState = _state.value
        exoPlayer.playWhenReady = currentState.isPlaying
        initialStateRunner = {
            exoPlayer.seekTo(currentState.currentPosition)
        }
        if (this::source.isInitialized) {
            setSource(source)
        }
        if (playerView != null) {
            playerViewAvailable(playerView!!)
        }
    }

    /**
     * A flag to indicate whether source is already set and waiting for
     * playerView to become available.
     */
    private val waitPlayerViewToPrepare = AtomicBoolean(false)

    override fun play() {
        _state.set { copy(startedPlay = true) }
        if (exoPlayer.playbackState == STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.playWhenReady = true
        autoHideControls()
    }

    override fun pause() {
        exoPlayer.playWhenReady = false
    }

    override fun togglePlaying() {
        if (exoPlayer.isPlaying) pause()
        else play()
    }

    override fun quickSeekForward() {
        if (_state.value.quickSeekAction.direction != QuickSeekDirection.None) {
            // Currently animating
            return
        }
        val target = (exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(target)
        updateDurationAndPosition()
        _state.set { copy(quickSeekAction = QuickSeekAction.forward()) }
    }

    override fun quickSeekRewind() {
        if (_state.value.quickSeekAction.direction != QuickSeekDirection.None) {
            // Currently animating
            return
        }
        val target = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
        exoPlayer.seekTo(target)
        updateDurationAndPosition()
        _state.set { copy(quickSeekAction = QuickSeekAction.rewind()) }
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        updateDurationAndPosition()
    }

    override fun setSource(source: VideoPlayerSource) {
        this.source = source
        if (playerView == null) {
            waitPlayerViewToPrepare.set(true)
        } else {
            prepare()
        }
    }

    fun enableGestures(isEnabled: Boolean) {
        _state.set { copy(gesturesEnabled = isEnabled) }
    }

    fun enableControls(enabled: Boolean) {
        _state.set { copy(controlsEnabled = enabled) }
    }

    fun showControls(autoHide: Boolean = true) {
        _state.set { copy(controlsVisible = true) }
        if (autoHide) {
            autoHideControls()
        } else {
            cancelAutoHideControls()
        }
    }

    private fun cancelAutoHideControls() {
        Log.i("VideoPlayerController", "cancelAutoHideControls")
        autoHideControllerJob?.cancel()
    }

    private fun autoHideControls() {
        cancelAutoHideControls()
        Log.i("VideoPlayerController", "autoHideControls")
        autoHideControllerJob = coroutineScope.launch {
            delay(5000)
            hideControls()
        }
    }

    fun hideControls() {
        _state.set { copy(controlsVisible = false) }
    }

    fun setDraggingProgress(draggingProgress: DraggingProgress?) {
        _state.set { copy(draggingProgress = draggingProgress) }
    }

    fun setQuickSeekAction(quickSeekAction: QuickSeekAction) {
        _state.set { copy(quickSeekAction = quickSeekAction) }
    }

    private fun updateDurationAndPosition() {
        if (exoPlayer.playbackState == STATE_READY || exoPlayer.playbackState == STATE_ENDED) {
            _state.set {
                copy(
                    duration = exoPlayer.duration.coerceAtLeast(0),
                    currentPosition = exoPlayer.currentPosition.coerceAtLeast(0),
                    secondaryProgress = exoPlayer.bufferedPosition.coerceAtLeast(0),
                    isPlaying = exoPlayer.isPlaying
                )
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun createVideoSource(): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)

        return when (val source = source) {
            is VideoPlayerSource.Raw -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(
                        MediaItem.fromUri(
                            RawResourceDataSource.buildRawResourceUri(
                                source.resId
                            )
                        )
                    )
            }

            is VideoPlayerSource.Network -> {
                // 贴吧视频链接多为 http 明文，升级为 https 避免被网络安全策略拦截
                val url = source.url.replaceFirst("http://", "https://")
                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .build()
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    private fun prepare() {
        exoPlayer.setMediaSource(createVideoSource())
        exoPlayer.prepare()
        // 预览播放器若已存在，下次拖动时再换成新的 source
        previewSourcePrepared = false
    }

    /** 拖动进度条时才创建并缓冲预览播放器，避免每个视频都多拉一路流、多占一个解码器 */
    private fun preparedPreviewPlayer(): ExoPlayer {
        val player = previewExoPlayer
        if (!previewSourcePrepared && this::source.isInitialized) {
            player.setMediaSource(createVideoSource())
            player.prepare()
            previewSourcePrepared = true
        }
        return player
    }

    fun playerViewAvailable(playerView: PlayerView) {
        // 全屏切换会换一个 PlayerView，先把旧的解绑，画面切到新 Surface 上继续播
        if (this.playerView !== playerView) {
            this.playerView?.player = null
        }
        this.playerView = playerView
        playerView.player = exoPlayer
        playerView.setBackgroundColor(videoPlayerBackgroundColor)

        if (waitPlayerViewToPrepare.compareAndSet(true, false)) {
            prepare()
        }
    }

    fun previewPlayerViewAvailable(playerView: PlayerView) {
        playerView.player = preparedPreviewPlayer()
    }

    fun previewSeekTo(position: Long) {
        // position is very accurate. Thumbnail doesn't have to be.
        // Roll to the nearest "even" integer.
        val seconds = position.toInt() / 1000
        val nearestEven = (seconds - seconds.rem(2)).toLong()
        coroutineScope.launch {
            previewSeekDebouncer.put(nearestEven * 1000)
        }
    }

    override fun reset() {
        exoPlayer.stop()
        _previewExoPlayer?.stop()
    }

    override fun release() {
        Log.i("VideoPlayerController", "$this release")
        if (released.compareAndSet(false, true)) {
            // 进度轮询会访问 exoPlayer getter，不停掉会在 release 后重新建一个播放器
            updateDurationAndPositionJob?.cancel()
            autoHideControllerJob?.cancel()
            exoPlayer.release()
            _previewExoPlayer?.release()
            _exoPlayer = null
            _previewExoPlayer = null
        }
    }

    override fun supportFullScreen(): Boolean {
        return fullScreenModeChangedListener != null
    }

    override fun toggleFullScreen() {
        require(fullScreenModeChangedListener != null) { "Full screen mode is not supported" }
        fullScreenModeChangedListener.onFullScreenModeChanged(!currentState { it.isFullScreen })
        _state.set { copy(isFullScreen = !isFullScreen) }
    }
}

val DefaultVideoPlayerBackgroundColor = Color.Black