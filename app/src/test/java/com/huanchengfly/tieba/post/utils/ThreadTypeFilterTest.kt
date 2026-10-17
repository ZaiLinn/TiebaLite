package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.protos.Abstract
import com.huanchengfly.tieba.post.api.models.protos.AlaLiveInfo
import com.huanchengfly.tieba.post.api.models.protos.Item
import com.huanchengfly.tieba.post.api.models.protos.PbGoodsInfo
import com.huanchengfly.tieba.post.api.models.protos.PollInfo
import com.huanchengfly.tieba.post.api.models.protos.PollOption
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadTypeFilterTest {
    private val none = ThreadTypeFilter.Options()
    private val all = ThreadTypeFilter.Options(
        blockVideo = true,
        blockVote = true,
        blockGoods = true,
        blockAppPromotion = true,
        blockLottery = true,
        blockScore = true,
    )

    private fun hidden(thread: ThreadInfo, options: ThreadTypeFilter.Options) =
        ThreadTypeFilter.shouldHide(thread, options)

    @Test
    fun `normal thread is kept even with every option on`() {
        assertFalse(hidden(ThreadInfo(title = "普通帖子"), all))
    }

    @Test
    fun `live and tiebaplus ads are always hidden`() {
        assertTrue(hidden(ThreadInfo(ala_info = AlaLiveInfo()), none))
        assertTrue(hidden(ThreadInfo(is_tiebaplus_ad = "1"), none))
        assertFalse(hidden(ThreadInfo(is_tiebaplus_ad = "0"), all))
    }

    @Test
    fun `video follows its option`() {
        val thread = ThreadInfo(videoInfo = VideoInfo())
        assertFalse(hidden(thread, none))
        assertTrue(hidden(thread, ThreadTypeFilter.Options(blockVideo = true)))
    }

    @Test
    fun `vote needs poll options`() {
        val vote = ThreadInfo(poll_info = PollInfo(options = listOf(PollOption())))
        assertFalse(hidden(vote, none))
        assertTrue(hidden(vote, ThreadTypeFilter.Options(blockVote = true)))
        // 空的 poll_info 不算投票帖，避免服务器下发空对象时误伤
        assertFalse(hidden(ThreadInfo(poll_info = PollInfo()), all))
    }

    @Test
    fun `goods follows its option`() {
        val goods = ThreadInfo(pb_goods_info = listOf(PbGoodsInfo(goods_id = 1)))
        assertFalse(hidden(goods, none))
        assertTrue(hidden(goods, ThreadTypeFilter.Options(blockGoods = true)))
    }

    @Test
    fun `app promotion needs a real item id`() {
        val promoted = ThreadInfo(item = Item(item_id = 42))
        assertFalse(hidden(promoted, none))
        assertTrue(hidden(promoted, ThreadTypeFilter.Options(blockAppPromotion = true)))
        assertFalse(hidden(ThreadInfo(item = Item()), all))
    }

    @Test
    fun `lottery needs thread type 76 and both kinds of text markers`() {
        val options = ThreadTypeFilter.Options(blockLottery = true)
        val lottery = ThreadInfo(
            thread_type = 76,
            title = "盖楼抽奖送周边",
            _abstract = listOf(Abstract(text = "开奖时间：周五晚八点")),
        )
        assertTrue(hidden(lottery, options))
        assertFalse(hidden(lottery, none))
        // 只有类型、没有关键词
        assertFalse(hidden(ThreadInfo(thread_type = 76, title = "普通标题"), options))
        // 只有一类关键词
        assertFalse(hidden(ThreadInfo(thread_type = 76, title = "我中奖了"), options))
        // 有关键词但类型不对
        assertFalse(hidden(ThreadInfo(thread_type = 0, title = "抽奖 开奖时间"), options))
    }

    @Test
    fun `score follows thread type 75`() {
        val score = ThreadInfo(thread_type = 75)
        assertFalse(hidden(score, none))
        assertTrue(hidden(score, ThreadTypeFilter.Options(blockScore = true)))
    }
}
