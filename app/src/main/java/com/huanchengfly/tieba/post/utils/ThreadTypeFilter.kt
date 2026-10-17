package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo

/**
 * 推荐流 / 吧页面帖子列表的类型过滤。
 *
 * 判定规则参考 ForbidAd4TieBa（GPL-3.0）的 CustomPostFilterMatcher，
 * 字段编号来自 n0099/tbclient.protobuf。
 */
object ThreadTypeFilter {
    /** 打分帖 */
    private const val THREAD_TYPE_SCORE = 75

    /** 抽奖帖 */
    private const val THREAD_TYPE_LOTTERY = 76

    private val LOTTERY_PRIMARY_MARKERS = listOf("抽奖", "大奖", "中奖")
    private val LOTTERY_CONTEXT_MARKERS = listOf("活动时间", "开奖时间", "活动奖品", "盖楼")

    data class Options(
        val blockVideo: Boolean = false,
        val blockVote: Boolean = false,
        val blockGoods: Boolean = false,
        val blockAppPromotion: Boolean = false,
        val blockLottery: Boolean = false,
        val blockScore: Boolean = false,
    )

    /** 直播和贴吧推广广告始终隐藏，其余类型按 [options] 决定。 */
    fun shouldHide(thread: ThreadInfo, options: Options): Boolean = when {
        thread.ala_info != null -> true
        thread.is_tiebaplus_ad == "1" -> true
        options.blockVideo && thread.videoInfo != null -> true
        options.blockVote && !thread.poll_info?.options.isNullOrEmpty() -> true
        options.blockGoods && thread.pb_goods_info.isNotEmpty() -> true
        options.blockAppPromotion && (thread.item?.item_id ?: 0L) != 0L -> true
        options.blockLottery && isLottery(thread) -> true
        options.blockScore && thread.thread_type == THREAD_TYPE_SCORE -> true
        else -> false
    }

    /**
     * 抽奖帖：thread_type 为 76，且标题或摘要里同时出现"抽奖"类和"活动时间"类词，
     * 只靠 thread_type 或只靠关键词都容易误伤普通帖子。
     */
    private fun isLottery(thread: ThreadInfo): Boolean {
        if (thread.thread_type != THREAD_TYPE_LOTTERY) return false
        val text = thread.title + " " + thread._abstract.joinToString(" ") { it.text }
        return LOTTERY_PRIMARY_MARKERS.any { it in text } && LOTTERY_CONTEXT_MARKERS.any { it in text }
    }
}
