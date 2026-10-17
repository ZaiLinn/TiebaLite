package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPost
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CommentLevelFilterTest {
    private val threadAuthor = 1L
    private val self = 2L
    private val on = CommentLevelFilter.Settings(enabled = true, minimumLevel = 5)

    private fun post(id: Long, authorId: Long, level: Int, floor: Int = id.toInt() + 1, subPosts: List<SubPostList>? = null) =
        Post(
            id = id,
            floor = floor,
            author_id = authorId,
            author = User(id = authorId, level_id = level),
            sub_post_list = subPosts?.let { SubPost(sub_post_list = it) },
            sub_post_number = subPosts?.size ?: 0,
        )

    private fun sub(id: Long, authorId: Long, level: Int) =
        SubPostList(id = id, author_id = authorId, author = User(id = authorId, level_id = level))

    private fun ids(posts: List<Post>) = posts.map { it.id }

    private fun filter(posts: List<Post>, settings: CommentLevelFilter.Settings = on, keepPostId: Long = 0L) =
        CommentLevelFilter.filter(posts, settings, threadAuthorId = threadAuthor, selfId = self, keepPostId = keepPostId)

    @Test
    fun `disabled filter returns the same list`() {
        val posts = listOf(post(10, 100, level = 1))
        assertSame(posts, filter(posts, CommentLevelFilter.Settings(enabled = false)))
    }

    @Test
    fun `hides posts below the minimum level and keeps the rest`() {
        val posts = listOf(post(10, 100, level = 4), post(11, 101, level = 5), post(12, 102, level = 9))
        assertEquals(listOf(11L, 12L), ids(filter(posts)))
    }

    @Test
    fun `first floor, thread author, self and jump target are always kept`() {
        val posts = listOf(
            post(10, 100, level = 1, floor = 1),
            post(11, threadAuthor, level = 1),
            post(12, self, level = 1),
            post(13, 103, level = 1),
            post(14, 104, level = 1),
        )
        assertEquals(listOf(10L, 11L, 12L, 13L), ids(filter(posts, keepPostId = 13L)))
    }

    @Test
    fun `unknown level is kept`() {
        val posts = listOf(post(10, 100, level = 0), Post(id = 11, floor = 3, author_id = 101))
        assertEquals(listOf(10L, 11L), ids(filter(posts)))
    }

    @Test
    fun `keepWithReplies keeps low level posts that have sub posts`() {
        val withReplies = post(10, 100, level = 1, subPosts = listOf(sub(1, 200, level = 9)))
        val noReplies = post(11, 101, level = 1)
        val settings = on.copy(keepWithReplies = true)
        assertEquals(listOf(10L), ids(filter(listOf(withReplies, noReplies), settings)))
    }

    @Test
    fun `low level sub posts are removed from previews unless skipNested`() {
        val parent = post(
            10, 100, level = 9,
            subPosts = listOf(sub(1, 200, level = 1), sub(2, 201, level = 9), sub(3, self, level = 1)),
        )
        val filtered = filter(listOf(parent)).single()
        assertEquals(listOf(2L, 3L), filtered.sub_post_list!!.sub_post_list.map { it.id })

        val skipped = filter(listOf(parent), on.copy(skipNested = true)).single()
        assertSame(parent, skipped)
    }
}
