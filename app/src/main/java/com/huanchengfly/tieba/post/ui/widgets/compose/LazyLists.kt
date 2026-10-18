package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDrawableDecodeInterceptor
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * 列表松手后的惯性滑动状态。惯性滑动期间暂停新图片的加载解码
 * （[PauseLoadWhenScrollingDrawableDecodeInterceptor]）和动图播放；手指拖动时照常加载。
 */
object ListFling {
    // 嵌套列表 / 多个列表可能同时惯性滑动，计数归零才算停下；performFling 都在主线程
    private var count = 0
    private val _flinging = MutableStateFlow(false)
    val flinging: StateFlow<Boolean> = _flinging.asStateFlow()

    internal fun begin() {
        if (count++ == 0) update(true)
    }

    internal fun end() {
        if (--count == 0) update(false)
    }

    private fun update(flinging: Boolean) {
        PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = flinging
        _flinging.value = flinging
    }
}

private class PauseLoadFlingBehavior(private val delegate: FlingBehavior) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // 每次松手都会调用 performFling，速度接近 0 时没有惯性，不切换状态
        if (abs(initialVelocity) < 1f) return with(delegate) { performFling(initialVelocity) }
        ListFling.begin()
        try {
            return with(delegate) { performFling(initialVelocity) }
        } finally {
            ListFling.end()
        }
    }
}

@Composable
fun rememberPauseLoadFlingBehavior(): FlingBehavior {
    val delegate = ScrollableDefaults.flingBehavior()
    return remember(delegate) { PauseLoadFlingBehavior(delegate) }
}

@Composable
fun MyLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = rememberPauseLoadFlingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    onGlobalEvent<GlobalEvent.ScrollToTop> {
        state.animateScrollToItem(0)
    }

    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

@Composable
fun MyLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = rememberPauseLoadFlingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit,
) {
    onGlobalEvent<GlobalEvent.ScrollToTop> {
        state.animateScrollToItem(0)
    }

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}