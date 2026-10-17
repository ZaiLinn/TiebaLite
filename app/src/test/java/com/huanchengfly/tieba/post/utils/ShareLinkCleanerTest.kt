package com.huanchengfly.tieba.post.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareLinkCleanerTest {

    @Test
    fun `removes tracking params and keeps others in order`() {
        assertEquals(
            "https://tieba.baidu.com/p/123?pn=2&see_lz=1",
            ShareLinkCleaner.clean(
                "https://tieba.baidu.com/p/123?sfc=copy&pn=2&client_type=2&see_lz=1&st=1700000000&unique=abc"
            )
        )
    }

    @Test
    fun `drops question mark when every param is tracking`() {
        assertEquals(
            "https://tieba.baidu.com/p/123",
            ShareLinkCleaner.clean("https://tieba.baidu.com/p/123?client_version=12.0&is_video=false")
        )
    }

    @Test
    fun `keeps fragment`() {
        assertEquals(
            "https://tieba.baidu.com/p/123?pn=2#post_1",
            ShareLinkCleaner.clean("https://tieba.baidu.com/p/123?sfc=qq&pn=2#post_1")
        )
    }

    @Test
    fun `param names match case insensitively but not by prefix`() {
        assertEquals(
            "https://tieba.baidu.com/p/1?stamp=1",
            ShareLinkCleaner.clean("https://tieba.baidu.com/p/1?SFC=x&stamp=1")
        )
    }

    @Test
    fun `other baidu subdomains are cleaned`() {
        assertEquals(
            "https://tiebac.baidu.com/p/1",
            ShareLinkCleaner.clean("https://tiebac.baidu.com/p/1?st=1")
        )
    }

    @Test
    fun `non baidu links are untouched`() {
        val url = "https://example.com/a?st=1&sfc=2"
        assertEquals(url, ShareLinkCleaner.clean(url))
        val lookalike = "https://baidu.com.evil.example/p?st=1"
        assertEquals(lookalike, ShareLinkCleaner.clean(lookalike))
    }

    @Test
    fun `links without query or scheme are untouched`() {
        assertEquals("https://tieba.baidu.com/p/1", ShareLinkCleaner.clean("https://tieba.baidu.com/p/1"))
        assertEquals("not a url?st=1", ShareLinkCleaner.clean("not a url?st=1"))
    }
}
