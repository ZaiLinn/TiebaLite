package com.huanchengfly.tieba.post.arch

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalEventTest {

    @Test
    fun `emitGlobalEventNow delivers to an active collector`() = runBlocking {
        val received = async(start = CoroutineStart.UNDISPATCHED) {
            GlobalEventFlow
                .filterIsInstance<GlobalEvent.Refresh>()
                .first { it.key == "global-event-test" }
        }
        emitGlobalEventNow(GlobalEvent.Refresh("global-event-test"))
        assertEquals("global-event-test", withTimeout(1_000) { received.await() }.key)
    }

    @Test
    fun `emitGlobalEventNow without collectors does not block`() {
        // 总线 DROP_OLDEST 缓冲：没有收集者时 tryEmit 也立刻返回
        repeat(10) { emitGlobalEventNow(GlobalEvent.ScrollToTop) }
    }
}
