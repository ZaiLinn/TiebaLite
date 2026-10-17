package com.huanchengfly.tieba.post.im

import com.huanchengfly.tieba.post.api.models.protos.im.ImMsgInfo
import com.huanchengfly.tieba.post.api.models.protos.im.ImUserInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateMessagesTest {
    private val self = 1L
    private val alice = ImUserInfo(userId = 2, userName = "alice", userNameShow = "Alice", portrait = "tb.1.abc?t=1700000000")

    private fun msg(id: Long, from: Long, to: Long, content: String = "hi", type: Int = 1, time: Int = id.toInt()) =
        ImMsgInfo(
            msgId = id,
            msgType = type,
            userId = from,
            toUid = to,
            content = content,
            createTime = time,
            userInfo = if (from == alice.userId) alice else ImUserInfo(userId = from),
            toUserInfo = if (to == alice.userId) alice else ImUserInfo(userId = to),
        )

    @Test
    fun `received message uses sender as peer`() {
        val m = PrivateMessages.fromProto(msg(10, from = 2, to = self), self)!!
        assertFalse(m.fromSelf)
        assertEquals(ImPeer(2, "Alice", "tb.1.abc"), m.peer)
    }

    @Test
    fun `sent message uses receiver as peer`() {
        val m = PrivateMessages.fromProto(msg(11, from = self, to = 2), self)!!
        assertTrue(m.fromSelf)
        assertEquals(2L, m.peer.uid)
        assertEquals("Alice", m.peer.name)
    }

    @Test
    fun `read receipts and misc messages are hidden`() {
        assertNull(PrivateMessages.fromProto(msg(12, 2, self, type = 22), self))
        assertNull(PrivateMessages.fromProto(msg(13, 2, self, type = 10), self))
    }

    @Test
    fun `non text messages are kept but marked unsupported`() {
        val m = PrivateMessages.fromProto(msg(14, 2, self, content = "{\"pic\":1}", type = 2), self)!!
        assertFalse(m.supported)
        assertEquals("", m.text)
    }

    @Test
    fun `text is extracted from json content`() {
        assertEquals("你好世界", PrivateMessages.extractText("[{\"type\":0,\"text\":\"你好\"},{\"type\":0,\"text\":\"世界\"}]"))
        assertEquals("plain", PrivateMessages.extractText("plain"))
        assertEquals("[not json", PrivateMessages.extractText("[not json"))
        assertEquals("[]", PrivateMessages.extractText("[]"))
    }

    @Test
    fun `merge deduplicates sorts and caps per peer`() {
        val a = (1L..5L).map { PrivateMessages.fromProto(msg(it, 2, self), self)!! }
        val b = (14L..17L).map { PrivateMessages.fromProto(msg(it, self, 3), self)!! }
        // a 的最后两条重复出现，按 msgId 去重
        val merged = PrivateMessages.merge(a, a.takeLast(2) + b, limitPerPeer = 3)
        assertEquals(merged.sortedBy { it.createTime }, merged)
        assertEquals(listOf(3L, 4L, 5L), merged.filter { it.peer.uid == 2L }.map { it.msgId })
        assertEquals(listOf(15L, 16L, 17L), merged.filter { it.peer.uid == 3L }.map { it.msgId })
    }

    @Test
    fun `conversations take latest message per peer`() {
        val messages = listOf(
            PrivateMessages.fromProto(msg(1, 2, self, content = "old", time = 100), self)!!,
            PrivateMessages.fromProto(msg(2, self, 2, content = "new", time = 200), self)!!,
            PrivateMessages.fromProto(msg(3, 3, self, content = "other", time = 150), self)!!,
        )
        val conversations = PrivateMessages.conversations(messages)
        assertEquals(listOf(2L, 3L), conversations.map { it.peer.uid })
        assertEquals("new", conversations.first().lastMessage.text)
        assertEquals("Alice", conversations.first().peer.name)
    }
}
