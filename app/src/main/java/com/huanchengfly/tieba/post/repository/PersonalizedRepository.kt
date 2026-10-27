package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponse
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

object PersonalizedRepository {
    /** 「关注」标签一页过滤后少于这么多条就继续往后拉 */
    private const val MIN_FOLLOWED_THREADS = 5

    /** 「关注」标签一次刷新/加载最多额外再拉的页数 */
    private const val MAX_EXTRA_PAGES = 4

    private data class FollowedForums(
        val uid: String?,
        val ids: Set<Long>,
        val names: Set<String>,
    )

    @Volatile
    private var followedForumsCache: FollowedForums? = null

    /**
     * 个性推荐
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    private fun rawPersonalizedFlow(loadType: Int, page: Int) =
        TiebaApi.getInstance()
            .personalizedProtoFlow(loadType, page)
            .map { response ->
                val liveThreadIds =
                    response.data_?.thread_list?.filter { it.ala_info != null }?.map { it.id }
                        ?: emptyList()
                response.copy(
                    data_ = response.data_?.copy(
                        thread_list = response.data_.thread_list.filter { !liveThreadIds.contains(it.id) },
                        thread_personalized = response.data_.thread_personalized.filter {
                            !liveThreadIds.contains(
                                it.tid
                            )
                        }
                    )
                )
            }

    /**
     * 个性推荐
     *
     * [onlyFollowedForums] 为 true 时（动态页「关注」标签）只保留已关注吧的帖子；
     * 过滤后太少会继续拉后面几页。返回结果和实际拉到的最后一页页码。
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    fun personalizedFlow(
        loadType: Int,
        page: Int = 1,
        onlyFollowedForums: Boolean = false,
    ): Flow<Pair<PersonalizedResponse, Int>> =
        flow {
            val first = rawPersonalizedFlow(loadType, page).first()
            if (!onlyFollowedForums) {
                emit(first to page)
                return@flow
            }
            val followed = getFollowedForums(forceRefresh = loadType == 1)
            if (followed.ids.isEmpty()) {
                emit(first.filterFollowed(followed) to page)
                return@flow
            }
            var merged = first.filterFollowed(followed)
            var lastPage = page
            var extraPages = 0
            while (merged.threadCount() < MIN_FOLLOWED_THREADS && extraPages < MAX_EXTRA_PAGES) {
                val next = rawPersonalizedFlow(2, lastPage + 1).first()
                lastPage += 1
                extraPages += 1
                if (next.data_?.thread_list.isNullOrEmpty()) break
                merged = merged.mergeWith(next.filterFollowed(followed))
            }
            emit(merged to lastPage)
        }

    /** 关注的吧列表，下拉刷新时重新拉取；拉取失败时退回缓存，没有缓存就抛出 */
    private suspend fun getFollowedForums(forceRefresh: Boolean): FollowedForums {
        val uid = AccountUtil.getUid()
        val cached = followedForumsCache?.takeIf { it.uid == uid }
        if (cached != null && !forceRefresh) return cached
        val fetched = runCatching {
            val likeForums = TiebaApi.getInstance().forumRecommendNewFlow().first()
                .data_?.like_forum ?: emptyList()
            FollowedForums(
                uid = uid,
                ids = likeForums.map { it.forum_id }.filter { it != 0L }.toSet(),
                names = likeForums.map { it.forum_name }.filter { it.isNotEmpty() }.toSet(),
            )
        }
        fetched.getOrNull()?.let { followedForumsCache = it }
        return fetched.getOrNull() ?: cached ?: throw fetched.exceptionOrNull()!!
    }

    private fun ThreadInfo.isFrom(followed: FollowedForums): Boolean {
        val forumId = forumId.takeIf { it != 0L } ?: forumInfo?.id ?: 0L
        if (forumId != 0L) return forumId in followed.ids
        val name = forumName.ifEmpty { forumInfo?.name.orEmpty() }
        return name in followed.names
    }

    private fun PersonalizedResponse.filterFollowed(followed: FollowedForums): PersonalizedResponse {
        val data = data_ ?: return this
        val threads = data.thread_list.filter { it.isFrom(followed) }
        val ids = threads.map { it.id }.toSet()
        return copy(
            data_ = data.copy(
                thread_list = threads,
                thread_personalized = data.thread_personalized.filter { it.tid in ids },
            )
        )
    }

    private fun PersonalizedResponse.mergeWith(other: PersonalizedResponse): PersonalizedResponse {
        val data = data_ ?: return other
        val otherData = other.data_ ?: return this
        return copy(
            data_ = data.copy(
                thread_list = data.thread_list + otherData.thread_list,
                thread_personalized = data.thread_personalized + otherData.thread_personalized,
            )
        )
    }

    private fun PersonalizedResponse.threadCount(): Int = data_?.thread_list?.size ?: 0
}
