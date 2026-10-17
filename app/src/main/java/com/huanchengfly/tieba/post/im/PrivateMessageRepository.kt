package com.huanchengfly.tieba.post.im

import android.util.Log
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.im.CommitPersonalMsgReqIdl
import com.huanchengfly.tieba.post.api.models.protos.im.CommitPersonalMsgResIdl
import com.huanchengfly.tieba.post.api.models.protos.im.GetGroupMsgReqIdl
import com.huanchengfly.tieba.post.api.models.protos.im.GetGroupMsgResIdl
import com.huanchengfly.tieba.post.arch.AppScope
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.UIDUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 私信数据源：管理长连接、增量拉取、发送和本地缓存。
 *
 * 页面通过 [acquire] / [release] 声明使用；最后一个页面离开 [IDLE_CLOSE_MS] 后断开连接，
 * 不在后台常驻。不发送已读回执。
 */
object PrivateMessageRepository {
    private const val TAG = "PrivateMessage"
    private const val IDLE_CLOSE_MS = 30_000L
    private const val MESSAGES_PER_PEER = 300
    private const val MAX_FETCH_PAGES = 20

    data class State(
        val loading: Boolean = false,
        val error: String? = null,
        val messages: List<ImMessage> = emptyList(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val mutex = Mutex()
    private var connection: ImConnection? = null
    private var connectionUid: Long = 0L
    private var privateGroupId: Long = 0L
    /** 增量拉取的游标：已收到的最大 msgId */
    private var fetchCursor: Long = 0L

    /** 私信组里已知的最新 msgId（含握手时服务器返回的），用于计算发送时的 recordId */
    private var latestMsgId: Long = 0L
    private var users = 0
    private var idleCloseJob: Job? = null
    private var loadedUid: Long? = null

    private val json = Json { ignoreUnknownKeys = true }

    fun acquire() {
        idleCloseJob?.cancel()
        users++
    }

    fun release() {
        users = (users - 1).coerceAtLeast(0)
        if (users == 0) {
            idleCloseJob = AppScope.launch {
                delay(IDLE_CLOSE_MS)
                mutex.withLock { closeLocked() }
            }
        }
    }

    /** 拉取新消息；首次调用时先读本地缓存 */
    suspend fun refresh() {
        val account = AccountUtil.getLoginInfo() ?: run {
            _state.update { it.copy(error = "请先登录") }
            return
        }
        mutex.withLock {
            loadCacheLocked(account)
            _state.update { it.copy(loading = true, error = null) }
            try {
                val conn = connectLocked(account)
                fetchLocked(conn, account)
                _state.update { it.copy(loading = false) }
            } catch (e: Exception) {
                Log.w(TAG, "refresh failed", e)
                closeLocked()
                _state.update { it.copy(loading = false, error = e.message ?: "连接失败") }
            }
        }
    }

    /** 发送文本私信，成功后拉一次新消息以拿到服务器上的完整记录 */
    suspend fun send(peer: ImPeer, text: String): Result<Unit> {
        val account = AccountUtil.getLoginInfo() ?: return Result.failure(IllegalStateException("请先登录"))
        val result = mutex.withLock {
            runCatching {
                val conn = connectLocked(account)
                val request = CommitPersonalMsgReqIdl(
                    data_ = CommitPersonalMsgReqIdl.DataReq(
                        toUid = peer.uid,
                        msgType = ImProtocol.MSG_TYPE_TEXT,
                        content = text,
                        recordId = latestMsgId * 100 + 1,
                    )
                )
                val response = CommitPersonalMsgResIdl.ADAPTER.decode(
                    conn.request(ImProtocol.CMD_COMMIT_PERSONAL_MSG, request.encode())
                )
                response.error?.takeIf { it.error_code != 0 }?.let {
                    throw ImConnection.ImException(it.error_msg.ifEmpty { "发送失败" }, it.error_code)
                }
                response.data_?.blockInfo?.takeIf { it.blockErrno != 0 }?.let {
                    throw ImConnection.ImException(it.blockErrmsg.ifEmpty { "对方不接收私信" }, it.blockErrno)
                }
                Unit
            }.onFailure {
                Log.w(TAG, "send failed", it)
                if (it !is ImConnection.ImException || it.code == 0) closeLocked()
            }
        }
        if (result.isSuccess) refresh()
        return result
    }

    private suspend fun connectLocked(account: Account): ImConnection {
        val uid = account.uid.toLongOrNull() ?: throw IllegalStateException("账号信息不完整")
        connection?.takeIf { it.isOpen && connectionUid == uid }?.let { return it }
        closeLocked()
        val conn = ImConnection.open(account)
        val group = conn.groups.firstOrNull { it.groupType == ImProtocol.GROUP_TYPE_PRIVATE_MSG }
            ?: run {
                conn.close()
                throw ImConnection.ImException("没有找到私信消息组")
            }
        connection = conn
        connectionUid = uid
        privateGroupId = group.groupId
        latestMsgId = maxOf(latestMsgId, group.lastMsgId)
        return conn
    }

    private suspend fun fetchLocked(conn: ImConnection, account: Account) {
        val selfUid = account.uid.toLong()
        val incoming = mutableListOf<ImMessage>()
        for (page in 0 until MAX_FETCH_PAGES) {
            val request = GetGroupMsgReqIdl(
                cuid = "${UIDUtil.newCUID}|com.baidu.tieba_mini12.35.1.0",
                data_ = GetGroupMsgReqIdl.DataReq(
                    groupMids = listOf(GetGroupMsgReqIdl.GroupLastId(privateGroupId, fetchCursor)),
                    gettype = "1",
                ),
            )
            val response = GetGroupMsgResIdl.ADAPTER.decode(
                conn.request(ImProtocol.CMD_GET_GROUP_MSG, request.encode())
            )
            response.error?.takeIf { it.error_code != 0 }?.let {
                throw ImConnection.ImException(it.error_msg.ifEmpty { "拉取私信失败" }, it.error_code)
            }
            val group = response.data_?.groupInfo.orEmpty()
                .firstOrNull { it.groupInfo?.groupId == privateGroupId }
                ?: break
            incoming += group.msgList.mapNotNull { PrivateMessages.fromProto(it, selfUid) }
            val maxId = group.msgList.maxOfOrNull { it.msgId } ?: 0L
            Log.i(TAG, "fetch page $page: ${group.msgList.size} messages, cursor $fetchCursor -> $maxId, hasMore=${group.hasMore}")
            // 游标不再前进时停止，避免服务器一直返回同一批消息
            if (maxId <= fetchCursor) break
            fetchCursor = maxId
            latestMsgId = maxOf(latestMsgId, maxId)
            if (group.hasMore == 0) break
        }
        if (incoming.isNotEmpty()) {
            val merged = PrivateMessages.merge(_state.value.messages, incoming, MESSAGES_PER_PEER)
            _state.update { it.copy(messages = merged) }
            saveCache(selfUid, merged)
        }
    }

    private fun closeLocked() {
        connection?.close()
        connection = null
    }

    private suspend fun loadCacheLocked(account: Account) {
        val uid = account.uid.toLongOrNull() ?: return
        if (loadedUid == uid) return
        val cached = withContext(Dispatchers.IO) {
            runCatching {
                cacheFile(uid).takeIf { it.exists() }
                    ?.readText()
                    ?.let { json.decodeFromString(ListSerializer(ImMessage.serializer()), it) }
            }.getOrNull().orEmpty()
        }
        loadedUid = uid
        fetchCursor = cached.maxOfOrNull { it.msgId } ?: 0L
        latestMsgId = fetchCursor
        _state.value = State(messages = cached)
    }

    private fun saveCache(uid: Long, messages: List<ImMessage>) {
        AppScope.launch(Dispatchers.IO) {
            runCatching {
                cacheFile(uid).apply { parentFile?.mkdirs() }
                    .writeText(json.encodeToString(ListSerializer(ImMessage.serializer()), messages))
            }.onFailure { Log.w(TAG, "save cache failed", it) }
        }
    }

    private fun cacheFile(uid: Long) = File(App.INSTANCE.filesDir, "private_messages/$uid.json")
}
