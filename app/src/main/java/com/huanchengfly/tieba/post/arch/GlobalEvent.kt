package com.huanchengfly.tieba.post.arch

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

sealed interface GlobalEvent : UiEvent {
    data object AccountSwitched : GlobalEvent

    data object ScrollToTop : GlobalEvent

    data class Refresh(val key: String) : GlobalEvent

    data class StartSelectImages(
        val id: String,
        val maxCount: Int,
        val mediaType: PickMediasRequest.MediaType
    ) : GlobalEvent

    data class SelectedImages(
        val id: String,
        val images: List<Uri>,
    ) : GlobalEvent

    data class ReplySuccess(
        val threadId: Long,
        val newPostId: Long,
        val postId: Long? = null,
        val subPostId: Long? = null,
        val newSubPostId: Long? = null,
    ) : GlobalEvent

    data class StartActivityForResult(
        val requesterId: String,
        val intent: Intent,
    ) : GlobalEvent

    data class ActivityResult(
        val requesterId: String,
        val resultCode: Int,
        val intent: Intent?,
    ) : GlobalEvent
}

private val globalEventSharedFlow: MutableSharedFlow<UiEvent> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    MutableSharedFlow(0, 2, BufferOverflow.DROP_OLDEST)
}

val GlobalEventFlow = globalEventSharedFlow.asSharedFlow()

fun CoroutineScope.emitGlobalEvent(event: UiEvent) {
    launch {
        globalEventSharedFlow.emit(event)
    }
}

suspend fun emitGlobalEventSuspend(event: UiEvent) {
    globalEventSharedFlow.emit(event)
}

inline fun <reified Event : UiEvent> CoroutineScope.onGlobalEvent(
    noinline filter: (Event) -> Boolean = { true },
    noinline listener: suspend (Event) -> Unit,
): Job {
    return launch {
        GlobalEventFlow
            .filterIsInstance<Event>()
            .filter {
                filter(it)
            }
            .cancellable()
            .collect {
                Log.i("GlobalEvent", "onGlobalEvent: $it")
                listener(it)
            }
    }
}

@Composable
inline fun <reified Event : UiEvent> onGlobalEvent(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    noinline filter: (Event) -> Boolean = { true },
    noinline listener: suspend (Event) -> Unit,
) {
    // 这里原来是 DisposableEffect(filter, listener)——filter/listener 每次重组都是新
    // 实例，导致每次重组都取消重订阅。与 replay=0 的全局总线之间因此有两种窗口：
    // 1) 无收集者窗口：事件被直接丢弃（v4.0.57 ReplyPage「选完图面板无反应」）；
    // 2) 新旧收集器重叠窗口：旧收集器的取消尚未生效、新收集器已订阅，同一事件被
    //    收到两次。2026-10-16 模拟器实测：MainActivityV2 的选图 launcher 就是这样
    //    被调用了两次，启动出两个相册 Activity——用户在顶层实例选图确认后，底下那个
    //    空实例露出，症状即「添加图片后相册页面不退出」（issue #1 同根因）。
    // 改为 LaunchedEffect(Unit) 常驻收集：组合存活期间只有一个收集器，事件不丢不重；
    // filter/listener 用 rememberUpdatedState 保持最新。
    val currentFilter by rememberUpdatedState(filter)
    val currentListener by rememberUpdatedState(listener)
    LaunchedEffect(Unit) {
        GlobalEventFlow
            .filterIsInstance<Event>()
            .filter { currentFilter(it) }
            .cancellable()
            .collect {
                Log.i("GlobalEvent", "onGlobalEvent: $it")
                currentListener(it)
            }
    }
}