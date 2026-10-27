package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.panpf.sketch.sketch
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 浏览过的帖子本地缓存。
 *
 * 帖子每一页加载成功后把响应原样（protobuf）存一份，下次加载同一页时网络不可用就读这份。
 * 每个帖子一个目录、每页一个文件，文件名由请求参数决定。
 *
 * 图片存在帖子目录的 img/ 下：页面存下后等一会儿（让界面先把图加载出来），
 * 先从图片下载缓存里复制，没有的再下载。离线读缓存时，把图片地址换成本地文件。
 *
 * 帖子数或占用空间超过设置的上限时，删掉最久没看的帖子。
 */
object ThreadCache {
    private const val TAG = "ThreadCache"
    private const val DIR_NAME = "thread_cache"
    private const val IMAGE_DIR_NAME = "img"

    /** 存下页面后等这么久再存图片，界面上的图大多已经在图片缓存里了 */
    private const val IMAGE_DELAY_MS = 15_000L

    /** 两次清理之间至少写入这么多次，避免每页都扫描目录 */
    private const val PRUNE_INTERVAL = 20

    private var writesSinceLastPrune = PRUNE_INTERVAL

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val imageMutex = Mutex()
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun rootDir(context: Context = App.INSTANCE): File = File(context.filesDir, DIR_NAME)

    val enabled: Boolean
        get() = App.INSTANCE.appPreferences.cacheViewedThreads

    private fun threadDir(threadId: Long): File = File(rootDir(), threadId.toString())

    private fun fileOf(threadId: Long, key: String): File = File(threadDir(threadId), "$key.pb")

    private fun imageFileOf(threadId: Long, url: String): File =
        File(File(threadDir(threadId), IMAGE_DIR_NAME), sha1(url))

    fun save(threadId: Long, key: String, response: PbPageResponse) {
        if (!enabled) return
        runCatching {
            val file = fileOf(threadId, key)
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeBytes(response.encode())
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
            file.parentFile?.setLastModified(System.currentTimeMillis())
            synchronized(this) {
                writesSinceLastPrune += 1
                if (writesSinceLastPrune >= PRUNE_INTERVAL) {
                    writesSinceLastPrune = 0
                    prune()
                }
            }
        }.onFailure { Log.w(TAG, "save $threadId/$key failed", it) }
        scheduleImages(threadId, response)
    }

    /** 读缓存；返回响应和保存时间，没有或损坏时返回 null */
    fun load(threadId: Long, key: String): Pair<PbPageResponse, Long>? {
        if (!enabled) return null
        val file = fileOf(threadId, key)
        if (!file.isFile) return null
        return runCatching {
            val response = PbPageResponse.ADAPTER.decode(file.readBytes())
            response.withLocalImages(threadId) to file.lastModified()
        }.onFailure {
            Log.w(TAG, "load $threadId/$key failed", it)
            file.delete()
        }.getOrNull()
    }

    private fun scheduleImages(threadId: Long, response: PbPageResponse) {
        val urls = response.allPosts()
            .flatMap { it.allContents() }
            .mapNotNull { it.imageUrlToSave() }
            .distinct()
        if (urls.isEmpty()) return
        scope.launch {
            delay(IMAGE_DELAY_MS)
            imageMutex.withLock { saveImages(threadId, urls) }
        }
    }

    private fun saveImages(threadId: Long, urls: List<String>) {
        if (!enabled) return
        val context = App.INSTANCE
        val downloadCache = runCatching { context.sketch.downloadCache }.getOrNull()
        urls.forEach { url ->
            val target = imageFileOf(threadId, url)
            if (target.isFile) return@forEach
            // 帖子目录在等待期间被清理掉了，就不再存
            if (!threadDir(threadId).isDirectory) return
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, "${target.name}.tmp")
            val copied = runCatching {
                downloadCache?.get(url)?.newInputStream()?.use { input ->
                    tmp.outputStream().use { input.copyTo(it) }
                    true
                } ?: false
            }.getOrDefault(false)
            val saved = copied || download(url, tmp)
            if (saved) {
                if (!tmp.renameTo(target)) tmp.delete()
            } else {
                tmp.delete()
            }
        }
    }

    private fun download(url: String, target: File): Boolean = runCatching {
        httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) return@use false
            body.byteStream().use { input -> target.outputStream().use { input.copyTo(it) } }
            true
        }
    }.onFailure { Log.w(TAG, "download image failed: $url", it) }.getOrDefault(false)

    private fun PbPageResponse.allPosts(): List<Post> {
        val data = data_ ?: return emptyList()
        return listOfNotNull(data.first_floor_post) + data.post_list
    }

    private fun Post.allContents(): List<PbContent> =
        content + sub_post_list?.sub_post_list.orEmpty().flatMap { it.content }

    /** 界面上显示的那张图（与 PbContent.picUrl 选图一致），视频只存封面 */
    private fun PbContent.imageUrlToSave(): String? = when (type) {
        3 -> ImageUtil.getUrl(
            App.INSTANCE, true, originSrc, bigCdnSrc, bigSrc, dynamic_, cdnSrc, cdnSrcActive, src
        )
        20 -> src
        5 -> src.takeIf { it.isNotBlank() }
        else -> null
    }?.takeIf { it.startsWith("http") }

    private fun PbPageResponse.withLocalImages(threadId: Long): PbPageResponse {
        val data = data_ ?: return this
        return copy(
            data_ = data.copy(
                first_floor_post = data.first_floor_post?.withLocalImages(threadId),
                post_list = data.post_list.map { it.withLocalImages(threadId) },
            )
        )
    }

    private fun Post.withLocalImages(threadId: Long): Post = copy(
        content = content.map { it.withLocalImage(threadId) },
        sub_post_list = sub_post_list?.let { list ->
            list.copy(sub_post_list = list.sub_post_list.map { sub ->
                sub.copy(content = sub.content.map { it.withLocalImage(threadId) })
            })
        },
    )

    /** 存过的那张图换成本地文件；所有候选地址都换掉，离线时无论按哪个选图都能命中 */
    private fun PbContent.withLocalImage(threadId: Long): PbContent {
        if (type != 3 && type != 20 && type != 5) return this
        val candidates = listOf(originSrc, bigCdnSrc, bigSrc, dynamic_, cdnSrc, cdnSrcActive, src)
        val local = candidates.asSequence()
            .filter { it.startsWith("http") }
            .map { imageFileOf(threadId, it) }
            .firstOrNull { it.isFile }
            ?: return this
        val uri = Uri.fromFile(local).toString()
        return if (type == 5) {
            copy(src = uri)
        } else {
            copy(
                originSrc = uri,
                bigCdnSrc = uri,
                bigSrc = uri,
                dynamic_ = uri,
                cdnSrc = uri,
                cdnSrcActive = uri,
                src = uri,
            )
        }
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }

    /** 按设置的上限删掉最久没看的帖子；改了上限后也会调用 */
    fun prune() {
        val prefs = App.INSTANCE.appPreferences
        val maxThreads = prefs.threadCacheMaxThreads?.toIntOrNull()?.takeIf { it > 0 } ?: Int.MAX_VALUE
        val maxBytes = prefs.threadCacheMaxSizeMb?.toLongOrNull()?.takeIf { it > 0 }
            ?.let { it * 1024 * 1024 } ?: Long.MAX_VALUE
        val threads = rootDir().listFiles()?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() } ?: return
        var total = 0L
        threads.forEachIndexed { index, dir ->
            total += dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            if (index >= maxThreads || total > maxBytes) dir.deleteRecursively()
        }
    }

    fun size(context: Context): Long =
        rootDir(context).walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun clear(context: Context) {
        rootDir(context).deleteRecursively()
    }
}
