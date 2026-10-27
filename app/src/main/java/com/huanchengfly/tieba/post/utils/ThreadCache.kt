package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.util.Log
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import java.io.File

/**
 * 浏览过的帖子本地缓存。
 *
 * 帖子每一页加载成功后把响应原样（protobuf）存一份，下次加载同一页时网络不可用就读这份。
 * 每个帖子一个目录、每页一个文件，文件名由请求参数决定；只缓存文字和楼层结构，图片走图片缓存。
 * 超过 [MAX_THREADS] 个帖子时，删掉最久没看的帖子。
 */
object ThreadCache {
    private const val TAG = "ThreadCache"
    private const val DIR_NAME = "thread_cache"
    private const val MAX_THREADS = 300

    /** 两次清理之间至少写入这么多次，避免每页都扫描目录 */
    private const val PRUNE_INTERVAL = 20

    private var writesSinceLastPrune = PRUNE_INTERVAL

    private fun rootDir(context: Context = App.INSTANCE): File = File(context.filesDir, DIR_NAME)

    val enabled: Boolean
        get() = App.INSTANCE.appPreferences.cacheViewedThreads

    private fun fileOf(threadId: Long, key: String): File =
        File(File(rootDir(), threadId.toString()), "$key.pb")

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
    }

    /** 读缓存；返回响应和保存时间，没有或损坏时返回 null */
    fun load(threadId: Long, key: String): Pair<PbPageResponse, Long>? {
        if (!enabled) return null
        val file = fileOf(threadId, key)
        if (!file.isFile) return null
        return runCatching {
            PbPageResponse.ADAPTER.decode(file.readBytes()) to file.lastModified()
        }.onFailure {
            Log.w(TAG, "load $threadId/$key failed", it)
            file.delete()
        }.getOrNull()
    }

    private fun prune() {
        val threads = rootDir().listFiles()?.filter { it.isDirectory } ?: return
        if (threads.size <= MAX_THREADS) return
        threads.sortedByDescending { it.lastModified() }
            .drop(MAX_THREADS)
            .forEach { it.deleteRecursively() }
    }

    fun size(context: Context): Long =
        rootDir(context).walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun clear(context: Context) {
        rootDir(context).deleteRecursively()
    }
}
