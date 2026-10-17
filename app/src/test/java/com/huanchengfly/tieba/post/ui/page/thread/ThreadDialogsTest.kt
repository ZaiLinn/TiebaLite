package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreadDialogsTest {

    // ---- parseJumpPage ----

    @Test
    fun `empty or blank input yields null instead of crashing`() {
        assertNull(parseJumpPage("", totalPage = 10))
        assertNull(parseJumpPage("   ", totalPage = 10))
    }

    @Test
    fun `non numeric and overflowing input yields null`() {
        assertNull(parseJumpPage("abc", totalPage = 10))
        assertNull(parseJumpPage("99999999999", totalPage = 10))
    }

    @Test
    fun `page outside 1 until totalPage yields null`() {
        assertNull(parseJumpPage("0", totalPage = 10))
        assertNull(parseJumpPage("11", totalPage = 10))
    }

    @Test
    fun `valid page is returned`() {
        assertEquals(1, parseJumpPage("1", totalPage = 10))
        assertEquals(10, parseJumpPage("10", totalPage = 10))
        assertEquals(3, parseJumpPage("003", totalPage = 10))
    }

    @Test
    fun `unknown total page only requires a positive page`() {
        assertEquals(42, parseJumpPage("42", totalPage = 0))
        assertNull(parseJumpPage("0", totalPage = 0))
    }

    // ---- buildDeleteIntent ----

    private fun deleteIntent(post: Post?, threadAuthorId: Long?, userId: Long) = buildDeleteIntent(
        deletePost = post,
        forumId = 1L,
        forumName = "forum",
        threadId = 2L,
        threadAuthorId = threadAuthorId,
        userId = userId,
        tbs = "tbs"
    )

    @Test
    fun `null post deletes the thread and detects own thread`() {
        assertEquals(
            ThreadUiIntent.DeleteThread(1L, "forum", 2L, deleteMyThread = true, tbs = "tbs"),
            deleteIntent(post = null, threadAuthorId = 100L, userId = 100L)
        )
        assertEquals(
            ThreadUiIntent.DeleteThread(1L, "forum", 2L, deleteMyThread = false, tbs = "tbs"),
            deleteIntent(post = null, threadAuthorId = 100L, userId = 200L)
        )
    }

    @Test
    fun `unknown thread author is not treated as own thread`() {
        assertEquals(
            ThreadUiIntent.DeleteThread(1L, "forum", 2L, deleteMyThread = false, tbs = "tbs"),
            deleteIntent(post = null, threadAuthorId = null, userId = 100L)
        )
    }

    @Test
    fun `post deletes that floor and detects own post`() {
        val post = Post(id = 30L, author_id = 100L)
        assertEquals(
            ThreadUiIntent.DeletePost(1L, "forum", 2L, postId = 30L, deleteMyPost = true, tbs = "tbs"),
            deleteIntent(post = post, threadAuthorId = 999L, userId = 100L)
        )
        assertEquals(
            ThreadUiIntent.DeletePost(1L, "forum", 2L, postId = 30L, deleteMyPost = false, tbs = "tbs"),
            deleteIntent(post = post, threadAuthorId = 100L, userId = 200L)
        )
    }
}
