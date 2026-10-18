package com.huanchengfly.tieba.post.utils

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateMapOf
import com.huanchengfly.tieba.post.models.database.History
import org.litepal.LitePal
import org.litepal.extension.find

/**
 * 已读帖子（打开过的帖子）的 id，列表据此把看过的帖子变灰。
 * 启动时从浏览历史读入，打开帖子时加入，清空浏览历史时一起清空。
 * 存在 Compose 快照状态里，列表读取后会随之刷新；只在主线程修改。
 */
object ReadThreads {
    private val ids = mutableStateMapOf<Long, Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 在后台线程调用：读数据库，再回到主线程写入 */
    fun init() {
        val loaded = runCatching {
            LitePal.select("data")
                .where("type = ?", HistoryUtil.TYPE_THREAD.toString())
                .find<History>()
                .mapNotNull { it.data.toLongOrNull() }
        }.getOrDefault(emptyList())
        mainHandler.post { loaded.forEach { ids[it] = Unit } }
    }

    fun markRead(threadId: Long) {
        if (threadId != 0L) ids[threadId] = Unit
    }

    fun isRead(threadId: Long): Boolean = threadId in ids

    fun clear() = ids.clear()
}
