package com.huanchengfly.tieba.post.update

import android.content.Context
import com.huanchengfly.tieba.post.R
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 更新检查来源。
 */
enum class UpdateSource {
    /** 启动时自动检查（节流，失败静默，弹窗提示）。 */
    STARTUP,

    /** 关于页手动检查（不节流，失败可见）。 */
    MANUAL
}

/**
 * 更新流程状态（单一数据源，启动弹窗与关于页内联卡片共用）。
 */
sealed class UpdateUiState {
    data object Idle : UpdateUiState()

    data class Checking(val source: UpdateSource) : UpdateUiState()

    data class Available(
        val release: GitHubRelease,
        val force: Boolean,
        val source: UpdateSource
    ) : UpdateUiState()

    data class UpToDate(val source: UpdateSource) : UpdateUiState()

    data class Failed(val message: String, val source: UpdateSource) : UpdateUiState()

    data class Downloading(
        val release: GitHubRelease,
        val downloaded: Long,
        val total: Long,
        val source: UpdateSource
    ) : UpdateUiState()

    data class DownloadFailed(val release: GitHubRelease, val message: String, val source: UpdateSource) : UpdateUiState()

    data class ReadyToInstall(
        val release: GitHubRelease,
        val file: File,
        /** true = SHA256 与 release 资产 digest 比对通过；false = 远端未提供 digest，未校验。 */
        val verified: Boolean,
        val installError: String? = null,
        val source: UpdateSource
    ) : UpdateUiState()
}

/**
 * 更新模块状态中枢（单例）：启动检查与关于页手动检查共用同一份
 * 检查 / 比对 / 元数据解析 / 下载 / 安装逻辑。
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val checking = AtomicBoolean(false)
    private val downloading = AtomicBoolean(false)

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    /**
     * debug 构建专用：本地 mock 的 API 根地址覆盖。MainActivityV2 从启动 intent extra
     * （tieba.update.mock / tieba.update.mockRoot，仅 debug 生效）读取后经 [check] 的
     * mockRoot/enableMock 注入；未传覆盖值时回落到 gradle 注入的 MOCK_UPDATE_API_ROOT。
     * 显式开关 mockUpdateEnabled：true 时 debug 构建走 mock 源；默认 false 走官方来源
     * （用户选择的平台，默认 Codeberg），保证 debug 构建的日常更新检查/真机验证走真实
     * 链路（mock 只在验收时打开）。
     */
    private var mockApiRoot: String? = null
    private var mockUpdateEnabled: Boolean = false

    /** 用户选择的更新检查平台来源（关于页设置，DataStore 持久化，默认 Codeberg）。 */
    private val _origin = MutableStateFlow(DEFAULT_ORIGIN)
    val origin: StateFlow<UpdateOrigin> = _origin.asStateFlow()

    init {
        scope.launch { _origin.value = loadOrigin() }
    }

    /** debug mock 覆盖地址；未启用 mock 时返回 null，由 [fetchLatest] 按平台来源取官方地址。 */
    private fun mockRootOverride(): String? {
        if (!BuildConfig.DEBUG || !mockUpdateEnabled) return null
        mockApiRoot?.takeIf { it.isNotBlank() }?.let { return it }
        return BuildConfig.MOCK_UPDATE_API_ROOT.takeIf { it.isNotBlank() }
    }

    /**
     * 按平台来源拉取最新 release；稳定/ai 通道由本地版本号决定（同现有逻辑）。
     * mock 覆盖（debug）优先于一切。
     */
    private fun fetchLatest(origin: UpdateOrigin): GitHubRelease? {
        val root = mockRootOverride() ?: UpdateDownloader.rootFor(origin)
        // cacheDir 启用 ETag 条件请求：304 不计入 GitHub 匿名限额（60 次/小时/IP）
        return if (VersionCompat.channelOf(BuildConfig.VERSION_NAME) == UpdateChannel.AI) {
            UpdateDownloader.fetchLatestAiRelease(root, context.cacheDir)
        } else {
            UpdateDownloader.fetchStableRelease(root, context.cacheDir)
        }
    }

    fun check(source: UpdateSource, mockRoot: String? = null, enableMock: Boolean = false) {
        if (BuildConfig.DEBUG && enableMock) {
            mockUpdateEnabled = true
            if (!mockRoot.isNullOrBlank()) mockApiRoot = mockRoot
        }
        if (!checking.compareAndSet(false, true)) return
        scope.launch {
            val origin = loadOrigin()
            _origin.value = origin
            try {
                if (source == UpdateSource.STARTUP &&
                    System.currentTimeMillis() - lastCheckTime() < CHECK_INTERVAL_MS
                ) {
                    return@launch
                }
                _state.value = UpdateUiState.Checking(source)
                var release = fetchLatest(origin)
                if (release == null && source == UpdateSource.STARTUP) {
                    // 主渠道失败时静默回退备用渠道：任一端发了新版本都能检测到。
                    // 手动检查不回退——失败要可见（错误信息带渠道名），便于排查。
                    release = fetchLatest(UpdateDownloader.otherOrigin(origin))
                }
                if (release == null) {
                    // 网络瞬断/接口失败：不写 lastCheckTime，避免 6h 节流把下次启动检查也静默屏蔽
                    _state.value = when (source) {
                        // 启动检查失败静默，不打扰
                        UpdateSource.STARTUP -> UpdateUiState.Idle
                        // 手动检查不回退备用渠道，失败信息带渠道名，便于用户自查/反馈
                        UpdateSource.MANUAL -> UpdateUiState.Failed(
                            context.getString(
                                R.string.text_update_check_failed_with_origin,
                                originLabel(origin)
                            ),
                            source
                        )
                    }
                    return@launch
                }
                saveLastCheckTime()
                when (VersionCompat.compare(BuildConfig.VERSION_NAME, release.tagName)) {
                    CompareResult.Newer -> {
                        val force = isForceUpdate(release)
                        if (source == UpdateSource.STARTUP && !force && isIgnored(release.tagName)) {
                            // 用户已对本版本点过"本次忽略"：启动不再提示
                            _state.value = UpdateUiState.Idle
                        } else {
                            _state.value = UpdateUiState.Available(release, force, source)
                        }
                    }
                    CompareResult.Equal, CompareResult.Older ->
                        _state.value = UpdateUiState.UpToDate(source)
                    CompareResult.Incompatible -> {
                        Log.w(TAG, "incompatible version tag: ${release.tagName}")
                        _state.value = when (source) {
                            UpdateSource.STARTUP -> UpdateUiState.Idle
                            UpdateSource.MANUAL -> UpdateUiState.Failed("版本信息无法解析", source)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "check failed: $e")
                _state.value = when (source) {
                    UpdateSource.STARTUP -> UpdateUiState.Idle
                    UpdateSource.MANUAL -> UpdateUiState.Failed(e.message ?: context.getString(R.string.text_update_check_failed), source)
                }
            } finally {
                checking.set(false)
            }
        }
    }

    /**
     * "本次忽略"：只跳过当前这个版本（存 release tag），About 页手动检查不受限。
     * 强更版本不可忽略。
     */
    fun ignore(tagName: String, force: Boolean) {
        if (force) return
        scope.launch {
            context.dataStore.edit { it[KEY_IGNORED_TAG] = tagName }
        }
        if (_state.value is UpdateUiState.Available) {
            _state.value = UpdateUiState.Idle
        }
    }

    /**
     * 下载当前可用版本的 APK 到 cacheDir/update/，完成后校验 SHA256。
     * 下载阶段沿用 [UpdateUiState.Available.source]，保证启动弹窗与 About 卡片按来源分轨。
     */
    fun download() {
        val (release, source) = when (val current = _state.value) {
            is UpdateUiState.Available -> current.release to current.source
            is UpdateUiState.DownloadFailed -> current.release to current.source
            else -> return
        }
        val url = release.apkUrl ?: run {
            _state.value = UpdateUiState.DownloadFailed(release, context.getString(R.string.text_update_no_apk), source)
            return
        }
        if (!downloading.compareAndSet(false, true)) return
        scope.launch {
            try {
                val dir = File(context.cacheDir, UPDATE_DIR)
                dir.mkdirs()
                val target = File(dir, release.apkName ?: "update-${release.tagName}.apk")
                _state.value = UpdateUiState.Downloading(release, 0L, 0L, source)
                val result = UpdateDownloader.downloadApk(release, target) { downloaded, total ->
                    _state.update { previous ->
                        if (previous is UpdateUiState.Downloading && previous.release.tagName == release.tagName) {
                            UpdateUiState.Downloading(release, downloaded, total, source)
                        } else {
                            previous
                        }
                    }
                }
                _state.value = when (result) {
                    is DownloadResult.Success ->
                        UpdateUiState.ReadyToInstall(release, result.file, verified = !release.apkDigest.isNullOrEmpty(), source = source)
                    is DownloadResult.Error ->
                        UpdateUiState.DownloadFailed(release, result.message, source)
                }
            } catch (e: Exception) {
                Log.w(TAG, "download failed: $e")
                _state.value = UpdateUiState.DownloadFailed(release, e.message ?: "下载失败", source)
            } finally {
                downloading.set(false)
            }
        }
    }

    /**
     * 拉起系统安装。未授予"安装未知应用"权限时跳转系统设置页并记录错误，授权后重试即可。
     */
    fun install() {
        val ready = _state.value as? UpdateUiState.ReadyToInstall ?: return
        scope.launch {
            val ok = UpdateInstaller.installApk(context, ready.file)
            if (!ok) {
                _state.value = ready.copy(
                    installError = context.getString(R.string.text_update_install_error)
                )
            }
        }
    }

    private fun isForceUpdate(release: GitHubRelease): Boolean {
        val minVersion = release.minVersion ?: return false
        return BuildConfig.VERSION_CODE < minVersion
    }

    private suspend fun isIgnored(tagName: String): Boolean {
        return runCatching {
            context.dataStore.data.first()[KEY_IGNORED_TAG] == tagName
        }.getOrDefault(false)
    }

    /**
     * 切换更新检查平台来源（关于页设置，持久化；下次检查即生效）。
     */
    fun setOrigin(origin: UpdateOrigin) {
        scope.launch {
            context.dataStore.edit { it[KEY_ORIGIN] = origin.name }
            _origin.value = origin
        }
    }

    /** 读取持久化的平台来源；未设置或解析失败回落 [DEFAULT_ORIGIN]。 */
    private suspend fun loadOrigin(): UpdateOrigin {
        return runCatching {
            context.dataStore.data.first()[KEY_ORIGIN]
                ?.let { runCatching { UpdateOrigin.valueOf(it) }.getOrNull() }
        }.getOrNull() ?: DEFAULT_ORIGIN
    }

    /** 平台来源展示名（专有名词，各语言一致）。 */
    private fun originLabel(origin: UpdateOrigin): String = when (origin) {
        UpdateOrigin.CODEBERG -> "Codeberg"
        UpdateOrigin.GITHUB -> "GitHub"
    }

    private suspend fun lastCheckTime(): Long {
        return runCatching {
            context.dataStore.data.first()[KEY_LAST_CHECK] ?: 0L
        }.getOrDefault(0L)
    }

    private suspend fun saveLastCheckTime() {
        runCatching {
            context.dataStore.edit { it[KEY_LAST_CHECK] = System.currentTimeMillis() }
        }
    }

    companion object {
        private const val TAG = "UpdateManager"

        /** 更新 APK 下载目录（cache 子目录，对应 FileProvider 的 update_apk paths）。 */
        const val UPDATE_DIR = "update"

        /** 启动检查节流间隔：6 小时。 */
        const val CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000

        private val KEY_LAST_CHECK = longPreferencesKey("update_last_check_time")
        private val KEY_IGNORED_TAG = stringPreferencesKey("update_ignored_tag")

        /** 更新检查平台来源（[UpdateOrigin.name]，默认 Codeberg）。 */
        private val KEY_ORIGIN = stringPreferencesKey("update_origin")
    }
}
