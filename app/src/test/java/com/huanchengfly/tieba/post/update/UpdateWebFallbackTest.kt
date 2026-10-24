package com.huanchengfly.tieba.post.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateWebFallbackTest {
    @Test
    fun parsesTagFromLatestRedirect() {
        assertEquals(
            "v4.0.65",
            parseTagFromReleaseLocation("https://github.com/ZaiLinn/TiebaLite/releases/tag/v4.0.65")
        )
        assertNull(parseTagFromReleaseLocation("https://github.com/ZaiLinn/TiebaLite/releases"))
        assertNull(parseTagFromReleaseLocation(""))
    }

    @Test
    fun findsApkHrefMatchingVersion() {
        val html = """
            <a href="/ZaiLinn/TiebaLite/archive/refs/tags/v4.0.65.zip">Source code</a>
            <a href="/ZaiLinn/TiebaLite/releases/download/v4.0.65/debug-4.0.64.apk">old</a>
            <a href="/ZaiLinn/TiebaLite/releases/download/v4.0.65/release-4.0.65+3583ecd.400065.apk">apk</a>
        """.trimIndent()
        assertEquals(
            "/ZaiLinn/TiebaLite/releases/download/v4.0.65/release-4.0.65+3583ecd.400065.apk",
            findReleaseApkHref(html, "v4.0.65")
        )
        assertNull(findReleaseApkHref(html, "v4.0.66"))
    }
}
