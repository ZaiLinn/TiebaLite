package com.huanchengfly.tieba.post.im

import android.util.Log
import com.huanchengfly.tieba.post.api.ClientVersion
import com.huanchengfly.tieba.post.api.models.protos.im.UpdateClientInfoReqIdl
import com.huanchengfly.tieba.post.api.models.protos.im.UpdateClientInfoResIdl
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.UIDUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 一条私信长连接：ws://im.tieba.baidu.com:8000。
 *
 * 服务器只支持明文 ws，cmd 1001 握手帧里的 BDUSS / STOKEN 不加密，
 * 所以私信是默认关闭的实验功能，只在私信页面打开期间保持连接。
 */
class ImConnection private constructor(
    private val webSocket: WebSocket,
    private val listener: Listener,
) {
    class GroupInfo(val groupId: Long, val groupType: Int, val lastMsgId: Long)

    class ImException(message: String, val code: Int = 0) : IOException(message)

    /** 服务器主动推送、没有对应请求的帧（例如新消息通知） */
    val pushes: SharedFlow<Int> get() = listener.pushes

    val isOpen: Boolean get() = listener.isOpen

    lateinit var groups: List<GroupInfo>
        private set

    suspend fun request(cmd: Int, payload: ByteArray): ByteArray {
        val key = checkNotNull(listener.aesKey) { "key exchange not finished" }
        return send(cmd, payload, key)
    }

    fun close() {
        webSocket.close(NORMAL_CLOSURE, null)
        listener.failAll(ImException("connection closed"))
    }

    private suspend fun send(cmd: Int, payload: ByteArray, aesKey: ByteArray?): ByteArray {
        if (!listener.isOpen) throw ImException("connection closed")
        val requestId = listener.requestIds.incrementAndGet()
        val response = CompletableDeferred<ByteArray>()
        listener.pending[requestId] = response
        try {
            val frame = ImProtocol.pack(payload, cmd, requestId, aesKey)
            if (!webSocket.send(frame.toByteString())) throw ImException("send failed")
            return withTimeout(REQUEST_TIMEOUT_MS) { response.await() }
        } finally {
            listener.pending.remove(requestId)
        }
    }

    private suspend fun handshake(account: Account, secret: ByteArray) {
        val request = UpdateClientInfoReqIdl(
            cuid = "${UIDUtil.newCUID}|com.baidu.tieba${CLIENT_VERSION}",
            data_ = UpdateClientInfoReqIdl.DataReq(
                bduss = account.bduss,
                device = ImProtocol.deviceJson(
                    cuid = UIDUtil.newCUID,
                    cuidGalaxy2 = CuidUtils.getNewCuid(),
                    clientVersion = CLIENT_VERSION,
                    timestampMillis = System.currentTimeMillis(),
                ),
                secretKey = ImProtocol.encryptSecret(secret).toByteString(),
                stoken = account.sToken,
            ),
        )
        // 握手帧本身不加密：服务器要先拿到 secretKey 才能解密后续帧
        val body = send(ImProtocol.CMD_UPDATE_CLIENT_INFO, request.encode(), aesKey = null)
        val response = UpdateClientInfoResIdl.ADAPTER.decode(body)
        response.error?.takeIf { it.error_code != 0 }?.let {
            throw ImException(it.error_msg.ifEmpty { "UpdateClientInfo failed" }, it.error_code)
        }
        groups = response.data_?.groupInfo.orEmpty().map { GroupInfo(it.groupId, it.groupType, it.lastMsgId) }
        listener.aesKey = ImProtocol.deriveAesKey(secret)
    }

    private class Listener : WebSocketListener() {
        val opened = CompletableDeferred<Unit>()
        val pending = ConcurrentHashMap<Int, CompletableDeferred<ByteArray>>()
        val requestIds = AtomicInteger((System.currentTimeMillis() / 1000).toInt())
        val pushes = MutableSharedFlow<Int>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        @Volatile
        var aesKey: ByteArray? = null

        @Volatile
        var isOpen = false

        override fun onOpen(webSocket: WebSocket, response: Response) {
            isOpen = true
            opened.complete(Unit)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val frame = try {
                ImProtocol.parse(bytes.toByteArray(), aesKey)
            } catch (e: Exception) {
                Log.w(TAG, "drop unreadable frame", e)
                return
            }
            val waiter = pending[frame.requestId]
            if (waiter != null) waiter.complete(frame.data) else pushes.tryEmit(frame.cmd)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE, null)
            failAll(ImException("closed by server: $code $reason"))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "connection failed, http=${response?.code}", t)
            failAll(ImException(t.message ?: "connection failed"))
        }

        fun failAll(error: Throwable) {
            isOpen = false
            opened.completeExceptionally(error)
            pending.values.forEach { it.completeExceptionally(error) }
            pending.clear()
        }
    }

    companion object {
        private const val TAG = "ImConnection"
        private const val URL = "ws://im.tieba.baidu.com:8000"
        private const val NORMAL_CLOSURE = 1000
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val REQUEST_TIMEOUT_MS = 10_000L
        private val CLIENT_VERSION = ClientVersion.TIEBA_V22.version

        /**
         * OkHttp 建 WebSocket 时会把 Sec-WebSocket-Extensions 强制设成 permessage-deflate，
         * 带这个值时服务器不返回 101、握手失败。这里改回官方客户端使用的 im_version=2.3。
         */
        private val extensionHeaderInterceptor = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Sec-WebSocket-Extensions", "im_version=2.3")
                    .build()
            )
        }

        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .addInterceptor(extensionHeaderInterceptor)
                .build()
        }

        /** 建立连接并完成密钥交换 */
        suspend fun open(account: Account): ImConnection {
            val listener = Listener()
            val request = Request.Builder()
                .url(URL)
                .header("User-Agent", "bdtb for Android $CLIENT_VERSION")
                .build()
            val webSocket = client.newWebSocket(request, listener)
            val connection = ImConnection(webSocket, listener)
            try {
                withTimeout(CONNECT_TIMEOUT_MS) { listener.opened.await() }
                connection.handshake(account, ImProtocol.newSecret())
            } catch (e: Throwable) {
                connection.close()
                throw e
            }
            return connection
        }
    }
}
