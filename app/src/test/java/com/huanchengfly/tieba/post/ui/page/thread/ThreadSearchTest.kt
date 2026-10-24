package com.huanchengfly.tieba.post.ui.page.thread

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadSearchTest {
    private fun post(id: Long, floor: Int, text: String) = SearchablePost(id, floor, text)

    @Test
    fun findsKeywordIgnoringCaseSortedByFloor() {
        val hits = searchPosts(
            listOf(
                post(3, 5, "后面也提到了 Kotlin"),
                post(1, 2, "kotlin 协程"),
                post(2, 3, "无关内容"),
            ),
            " KOTLIN "
        )
        assertEquals(listOf(2, 5), hits.map { it.floor })
        val first = hits.first()
        assertEquals("kotlin", first.snippet.substring(first.matchStart, first.matchStart + first.matchLength))
    }

    @Test
    fun snippetIsTrimmedAroundMatch() {
        val text = "a".repeat(100) + "关键词" + "b".repeat(100)
        val hit = searchPosts(listOf(post(1, 2, text)), "关键词").single()
        assertTrue(hit.snippet.startsWith("…"))
        assertTrue(hit.snippet.endsWith("…"))
        assertEquals("关键词", hit.snippet.substring(hit.matchStart, hit.matchStart + hit.matchLength))
    }

    @Test
    fun blankKeywordFindsNothing() {
        assertTrue(searchPosts(listOf(post(1, 2, "内容")), "  ").isEmpty())
    }

    @Test
    fun parsesFloorInput() {
        assertEquals(12, parseJumpFloor(" 12 "))
        assertNull(parseJumpFloor("0"))
        assertNull(parseJumpFloor(""))
        assertNull(parseJumpFloor("99999999999"))
    }
}
