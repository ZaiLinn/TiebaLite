package com.huanchengfly.tieba.post.ui.widgets.compose

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Animatable as AnimatableDrawable
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.compose.rememberAsyncImageState
import com.github.panpf.sketch.request.Depth
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.pauseLoadWhenScrolling
import com.github.panpf.sketch.stateimage.ThumbnailMemoryCacheStateImage
import com.github.panpf.sketch.transform.MaskTransformation
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity.Companion.EXTRA_PHOTO_VIEW_DATA
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.NetworkUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.flow.combine

fun shouldLoadImage(context: Context, skipNetworkCheck: Boolean): Boolean {
    val imageLoadSettings =
        context.appPreferences.imageLoadType?.toIntOrNull() ?: ImageUtil.SETTINGS_SMART_ORIGIN
    return skipNetworkCheck
            || imageLoadSettings == ImageUtil.SETTINGS_SMART_ORIGIN
            || imageLoadSettings == ImageUtil.SETTINGS_ALL_ORIGIN
            || (imageLoadSettings == ImageUtil.SETTINGS_SMART_LOAD && NetworkUtil.isWifiConnected(
        context
    ))
}

@Composable
private fun PreviewImage(
    imageUri: String,
    show: Boolean,
    layoutSizeProvider: () -> IntSize,
    layoutOffsetProvider: () -> Offset,
    imageAspectRatioProvider: () -> Float,
    originImageUri: String? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var showFullScreenLayout by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            showFullScreenLayout = true
            showPreview = true
        } else {
            showPreview = false
        }
    }

    val windowWidthSizeClass = LocalWindowSizeClass.current.widthSizeClass
    val widthFraction = remember(windowWidthSizeClass) {
        when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact -> 0.8f
            WindowWidthSizeClass.Medium -> 0.6f
            else -> 0.4f
        }
    }
    val imageAspectRatio = imageAspectRatioProvider()
    val previewImageWidthPx = remember(widthFraction) {
        App.ScreenInfo.EXACT_SCREEN_WIDTH * widthFraction
    }
    val previewImageHeightPx = remember(imageAspectRatio, previewImageWidthPx) {
        previewImageWidthPx * imageAspectRatio
    }
    val previewImageWidthDp = remember(previewImageWidthPx, density) {
        with(density) { previewImageWidthPx.toDp() }
    }
    val previewImageHeightDp = remember(previewImageHeightPx, density) {
        with(density) { previewImageHeightPx.toDp() }
    }

    val screenWidth = App.ScreenInfo.EXACT_SCREEN_WIDTH
    val screenCenterX = screenWidth / 2
    val screenHeight = App.ScreenInfo.EXACT_SCREEN_HEIGHT
    val screenCenterY = screenHeight / 2
    val statusBarHeight = WindowInsets.statusBars.getTop(density)

    if (showFullScreenLayout) {
        val animProgress = remember { Animatable(0f) }

        LaunchedEffect(showPreview) {
            animProgress.animateTo(
                if (showPreview) 1f else 0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                )
            )
            if (!showPreview) {
                showFullScreenLayout = false
            }
        }

        FullScreen {
            val (layoutWidthPx, layoutHeightPx) = layoutSizeProvider()
            val layoutWidthDp = remember(layoutWidthPx, density) {
                with(density) { layoutWidthPx.toDp() }
            }
            val layoutHeightDp = remember(layoutHeightPx, density) {
                with(density) { layoutHeightPx.toDp() }
            }

            val request = remember(imageUri) {
                DisplayRequest(context, imageUri)
            }

            val originRequest = remember(imageUri, originImageUri) {
                DisplayRequest(context, originImageUri ?: imageUri) {
                    placeholder(ThumbnailMemoryCacheStateImage(imageUri))
                    crossfade(fadeStart = false)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(
                            lerp(layoutWidthDp, previewImageWidthDp, animProgress.value)
                        )
                        .height(
                            lerp(layoutHeightDp, previewImageHeightDp, animProgress.value)
                        )
                        .absoluteOffset {
                            val layoutOffset = layoutOffsetProvider()
                            val currentLayoutWidthPx = lerp(
                                layoutWidthPx.toFloat(),
                                previewImageWidthPx,
                                animProgress.value
                            )
                            val currentLayoutHeightPx = lerp(
                                layoutHeightPx.toFloat(),
                                previewImageHeightPx,
                                animProgress.value
                            )
                            IntOffset(
                                lerp(
                                    layoutOffset.x - (screenCenterX - currentLayoutWidthPx / 2),
                                    0f,
                                    animProgress.value
                                ).toInt(),
                                lerp(
                                    layoutOffset.y - (screenCenterY - currentLayoutHeightPx / 2 + statusBarHeight / 2),
                                    0f,
                                    animProgress.value
                                ).toInt()
                            )
                        }
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    AsyncImage(
                        request = request,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (originImageUri != null && animProgress.value >= 1f) {
                        AsyncImage(
                            request = originRequest,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkImage(
    imageUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    photoViewData: PhotoViewData? = null,
    contentScale: ContentScale = ContentScale.Fit,
    skipNetworkCheck: Boolean = false,
    enablePreview: Boolean = false,
) {
    val context = LocalContext.current

    var shouldLoad by remember { mutableStateOf(shouldLoadImage(context, skipNetworkCheck)) }
    val enableClick = remember(photoViewData, shouldLoad) { photoViewData != null || !shouldLoad }

    val colorMask =
        if (ExtendedTheme.colors.isNightMode && context.appPreferences.imageDarkenWhenNightMode) {
            MaskTransformation(Color.parseColor("#99000000"))
        } else null

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }

    var layoutOffset by remember { mutableStateOf(Offset.Zero) }

    val request = remember(imageUri, shouldLoad, colorMask) {
        DisplayRequest(context, imageUri) {
            placeholder(ImageUtil.getPlaceHolder(context, 0))
            crossfade()
            pauseLoadWhenScrolling()
            if (!shouldLoad) {
                depth(Depth.LOCAL)
            }
            if (colorMask != null) {
                transformations(colorMask)
            }
        }
    }

    val state = rememberAsyncImageState()
    val imageAspectRatio by remember {
        derivedStateOf {
            with(state.result) {
                if (this is DisplayResult.Success) imageInfo.height.toFloat() / imageInfo.width
                else 0f
            }
        }
    }

    // 惯性滑动时暂停动图，停下后继续；只恢复由这里暂停的，不打扰动图自己的播放状态
    LaunchedEffect(state) {
        var pausedByFling: AnimatableDrawable? = null
        snapshotFlow { (state.result as? DisplayResult.Success)?.drawable as? AnimatableDrawable }
            .combine(ListFling.flinging) { drawable, flinging -> drawable to flinging }
            .collect { (drawable, flinging) ->
                if (flinging && drawable != null && drawable.isRunning) {
                    drawable.stop()
                    pausedByFling = drawable
                } else if (!flinging) {
                    pausedByFling?.start()
                    pausedByFling = null
                }
            }
    }

    var isLongPressing by remember { mutableStateOf(false) }

    if (enablePreview) {
        PreviewImage(
            imageUri = imageUri,
            show = isLongPressing,
            layoutSizeProvider = { layoutSize },
            layoutOffsetProvider = { layoutOffset },
            imageAspectRatioProvider = { imageAspectRatio },
            originImageUri = photoViewData?.data?.originUrl
        )
    }

    val onTapImage = {
        if (!shouldLoad) {
            shouldLoad = true
        } else if (photoViewData != null) {
            context.goToActivity<PhotoViewActivity> {
                putExtra(EXTRA_PHOTO_VIEW_DATA, photoViewData)
            }
        }
    }
    Box(
        modifier = Modifier
            .pointerInput(enableClick, enablePreview) {
                // 长按预览时才需要 onLongPress/onPress；列表里绝大多数图只需点击
                if (enablePreview) {
                    detectTapGestures(
                        onLongPress = { isLongPressing = true },
                        onPress = {
                            tryAwaitRelease()
                            isLongPressing = false
                        },
                        onTap = { if (enableClick) onTapImage() },
                    )
                } else if (enableClick) {
                    detectTapGestures(onTap = { onTapImage() })
                }
            }
            .then(modifier)
    ) {
        AsyncImage(
            request = request,
            // onGloballyPositioned 每帧上报位置，列表滚动时会让每张图每帧重组；
            // 位置和尺寸只有长按预览用到，非预览图不挂这些回调
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enablePreview) {
                        Modifier
                            .onSizeChanged { layoutSize = it }
                            .onGloballyPositioned { layoutOffset = it.positionInWindow() }
                    } else {
                        Modifier
                    }
                ),
            contentDescription = contentDescription,
            contentScale = contentScale,
            state = state,
        )
    }
}

@Composable
fun NetworkImage(
    imageUriProvider: () -> String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    photoViewDataProvider: (() -> PhotoViewData)? = null,
    contentScale: ContentScale = ContentScale.Fit,
    skipNetworkCheck: Boolean = false,
    enablePreview: Boolean = false,
) {
    val imageUri by rememberUpdatedState(newValue = imageUriProvider())
    val photoViewData by rememberUpdatedState(newValue = photoViewDataProvider?.invoke())

    NetworkImage(
        imageUri = imageUri,
        contentDescription = contentDescription,
        modifier = modifier,
        photoViewData = photoViewData,
        contentScale = contentScale,
        skipNetworkCheck = skipNetworkCheck,
        enablePreview = enablePreview
    )
}