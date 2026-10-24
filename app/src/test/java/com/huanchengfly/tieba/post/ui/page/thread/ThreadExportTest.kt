package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.PbContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadExportTest {
    @Test
    fun convertsContentTypes() {
        val markdown = listOf(
            PbContent(type = 0, text = "你好"),
            PbContent(type = 2, text = "image_emoticon", c = "滑稽"),
            PbContent(type = 1, text = "官网", link = "https://example.com"),
            PbContent(type = 3, originSrc = "https://img/a.jpg", bsize = "10,10"),
            PbContent(type = 10),
        ).toMarkdown()
        assertEquals("你好[滑稽][官网](https://example.com)\n![](https://img/a.jpg)\n[语音]", markdown)
    }

    @Test
    fun buildsDocumentWithFloorsAndSubPosts() {
        val markdown = ExportThread(
            threadId = 123,
            title = "标题",
            forumName = "测试",
            seeLz = true,
            posts = listOf(
                ExportPost(1, 1, 0, "楼主", "一楼正文", emptyList()),
                ExportPost(2, 2, 0, "路人", "二楼", listOf("甲" to "回复1", "乙" to "回复\n2")),
            ),
        ).toMarkdown(exportTime = 0)
        assertTrue(markdown.startsWith("# 标题\n"))
        assertTrue(markdown.contains("- 链接：https://tieba.baidu.com/p/123"))
        assertTrue(markdown.contains("- 只看楼主"))
        assertTrue(markdown.contains("### 1 楼 · 楼主\n\n一楼正文"))
        assertTrue(markdown.contains("> **甲**：回复1\n>\n> **乙**：回复 2\n"))
        assertFalse(markdown.endsWith("\n\n"))
    }
}
