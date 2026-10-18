package com.huanchengfly.tieba.post.ui.page.thread

import android.util.LruCache
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo

/**
 * 从列表点进帖子时暂存列表里已有的帖子信息，帖子页打开时先用它显示标题、楼主和 1 楼，
 * 不必等接口返回。只在内存里转交（帖子信息太大，不适合放进路由参数），按帖子 id 保留最近几条。
 */
object ThreadPreviewCache {
    private val cache = LruCache<Long, ThreadInfo>(8)

    fun put(threadId: Long, threadInfo: ThreadInfo) {
        cache.put(threadId, threadInfo)
    }

    fun get(threadId: Long): ThreadInfo? = cache.get(threadId)
}

/** 带上列表里已有的帖子信息打开帖子页 */
fun NavController.navigateToThread(threadId: Long, threadInfo: ThreadInfo) {
    ThreadPreviewCache.put(threadId, threadInfo)
    navigate("thread/$threadId")
}
