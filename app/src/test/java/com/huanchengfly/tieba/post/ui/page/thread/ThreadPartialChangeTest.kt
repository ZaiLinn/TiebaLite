package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.arch.wrapImmutable
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ThreadPartialChangeTest {

    // 显式传入 blocked/contentRenders/subPosts，避免默认值在 JVM 单测里走 Android 依赖
    private fun item(id: Long) = PostItemData(
        post = wrapImmutable(Post(id = id)),
        blocked = false,
        contentRenders = persistentListOf(),
        subPosts = persistentListOf(),
    )

    private val state = ThreadUiState(data = persistentListOf(item(1L), item(2L), item(3L)))

    @Test
    fun `delete post success removes that post`() {
        val newState = ThreadPartialChange.DeletePost.Success(postId = 2L).reduce(state)
        assertEquals(listOf(1L, 3L), newState.data.map { it.post.get { id } })
    }

    @Test
    fun `delete post success for a post not in the list keeps state`() {
        val newState = ThreadPartialChange.DeletePost.Success(postId = 99L).reduce(state)
        assertSame(state, newState)
    }

    @Test
    fun `delete post failure keeps state`() {
        val newState = ThreadPartialChange.DeletePost.Failure(-1, "error").reduce(state)
        assertSame(state, newState)
    }
}
