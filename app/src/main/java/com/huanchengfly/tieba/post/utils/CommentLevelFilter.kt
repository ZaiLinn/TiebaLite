package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.User

/**
 * 帖子页按用户等级过滤楼层与楼中楼预览。
 *
 * 规则参考 ForbidAd4TieBa（GPL-3.0）的 CommentLevelFilter，并额外保证：
 * 一楼、楼主、自己、跳转目标楼层永远保留；拿不到等级（level_id <= 0）的也保留，
 * 避免用户信息缺失时整页楼层被误隐藏。
 */
object CommentLevelFilter {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 18
    const val DEFAULT_MINIMUM_LEVEL = 5

    data class Settings(
        val enabled: Boolean = false,
        val minimumLevel: Int = DEFAULT_MINIMUM_LEVEL,
        /** 有楼中楼回复的楼层不隐藏 */
        val keepWithReplies: Boolean = false,
        /** 不过滤楼中楼预览 */
        val skipNested: Boolean = false,
    )

    fun filter(
        posts: List<Post>,
        settings: Settings,
        threadAuthorId: Long,
        selfId: Long?,
        keepPostId: Long = 0L,
    ): List<Post> {
        if (!settings.enabled) return posts
        fun protected(authorId: Long) = authorId == threadAuthorId || (selfId != null && authorId == selfId)

        return posts.mapNotNull { post ->
            val keepFloor = post.floor == 1 ||
                (keepPostId != 0L && post.id == keepPostId) ||
                protected(post.author_id) ||
                !isBelow(post.author, settings) ||
                (settings.keepWithReplies && hasReplies(post))
            if (!keepFloor) return@mapNotNull null
            if (settings.skipNested) return@mapNotNull post

            val subPosts = post.sub_post_list ?: return@mapNotNull post
            val keptSubPosts = subPosts.sub_post_list.filter {
                protected(it.author_id) || !isBelow(it.author, settings)
            }
            if (keptSubPosts.size == subPosts.sub_post_list.size) post
            else post.copy(sub_post_list = subPosts.copy(sub_post_list = keptSubPosts))
        }
    }

    private fun isBelow(author: User?, settings: Settings): Boolean {
        val level = author?.level_id ?: 0
        return level > 0 && level < settings.minimumLevel
    }

    private fun hasReplies(post: Post): Boolean =
        post.sub_post_number > 0 || !post.sub_post_list?.sub_post_list.isNullOrEmpty()
}
