package com.huanchengfly.tieba.post.im

import com.huanchengfly.tieba.post.api.models.protos.im.ImMsgInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ImPeer(
    val uid: Long,
    val name: String = "",
    val portrait: String = "",
)

@Serializable
data class ImMessage(
    val msgId: Long,
    val peer: ImPeer,
    val fromSelf: Boolean,
    val text: String,
    val supported: Boolean,
    /** 秒级时间戳 */
    val createTime: Long,
)

data class ImConversation(
    val peer: ImPeer,
    val lastMessage: ImMessage,
)

/** 服务器消息 → 本地消息，以及会话分组。纯逻辑，便于单元测试。 */
object PrivateMessages {
    /** 已读回执、系统杂项等不展示的消息类型 */
    private val HIDDEN_MSG_TYPES = setOf(10, 22)

    private val json = Json { ignoreUnknownKeys = true }

    fun fromProto(msg: ImMsgInfo, selfUid: Long): ImMessage? {
        if (msg.msgType in HIDDEN_MSG_TYPES) return null
        val fromSelf = msg.userId == selfUid
        val peerUid = if (fromSelf) msg.toUid else msg.userId
        if (peerUid == 0L) return null
        val peerInfo = if (fromSelf) msg.toUserInfo else msg.userInfo
        val peer = ImPeer(
            uid = peerUid,
            name = peerInfo?.userNameShow?.ifEmpty { null } ?: peerInfo?.nickName?.ifEmpty { null }
                ?: peerInfo?.userName.orEmpty(),
            portrait = peerInfo?.portrait?.substringBefore('?').orEmpty(),
        )
        val supported = msg.msgType == ImProtocol.MSG_TYPE_TEXT
        return ImMessage(
            msgId = msg.msgId,
            peer = peer,
            fromSelf = fromSelf,
            text = if (supported) extractText(msg.content) else "",
            supported = supported,
            createTime = msg.createTime.toLong(),
        )
    }

    /**
     * 文本私信的 content 可能是纯文本，也可能是 `[{"type":0,"text":"..."}]` 形式的 JSON，
     * 后者取出所有 text 拼接；解析失败按纯文本处理。
     */
    fun extractText(content: String): String {
        val trimmed = content.trim()
        if (!trimmed.startsWith("[")) return content
        return try {
            json.parseToJsonElement(trimmed).let { it as? JsonArray }
                ?.mapNotNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull }
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString("")
                ?: content
        } catch (e: Exception) {
            content
        }
    }

    /** 合并新旧消息：按 msgId 去重、按时间排序，每个会话最多保留 [limitPerPeer] 条 */
    fun merge(existing: List<ImMessage>, incoming: List<ImMessage>, limitPerPeer: Int): List<ImMessage> =
        (existing + incoming)
            .associateBy { it.msgId }
            .values
            .groupBy { it.peer.uid }
            .values
            .flatMap { messages -> messages.sortedWith(ORDER).takeLast(limitPerPeer) }
            .sortedWith(ORDER)

    /** 每个对方一条会话，取最新一条消息；对方昵称头像以最新一条非空的为准 */
    fun conversations(messages: List<ImMessage>): List<ImConversation> =
        messages.groupBy { it.peer.uid }
            .map { (_, list) ->
                val last = list.maxWith(ORDER)
                val peer = list.sortedWith(ORDER).lastOrNull { it.peer.name.isNotEmpty() }?.peer ?: last.peer
                ImConversation(peer = peer, lastMessage = last)
            }
            .sortedByDescending { it.lastMessage.createTime }

    private val ORDER = compareBy<ImMessage>({ it.createTime }, { it.msgId })
}
