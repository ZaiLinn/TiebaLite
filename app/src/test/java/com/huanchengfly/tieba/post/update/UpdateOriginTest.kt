package com.huanchengfly.tieba.post.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 更新渠道（平台来源）与 stable 通道选版：
 * - rootFor/otherOrigin：GitHub 为默认主渠道，Codeberg 为镜像/回退渠道
 * - pickLatestStable：列表筛选替代 /releases/latest（Codeberg 的 latest 排除 prerelease，
 *   只有预发布时 404），判据剔除 prerelease 与 ai tag，base 优先取最大
 */
class UpdateOriginTest {

    private fun rel(tag: String, prerelease: Boolean = false) = GitHubRelease(
        tagName = tag, name = tag, htmlUrl = "", body = "",
        isPrerelease = prerelease, apkUrl = null, apkName = null, apkDigest = null
    )

    // ---- 平台来源映射 ----

    @Test
    fun `rootFor maps codeberg and github to their api roots`() {
        assertEquals(
            "https://codeberg.org/api/v1/repos/min09577/TiebaLite",
            UpdateDownloader.rootFor(UpdateOrigin.CODEBERG)
        )
        assertEquals(
            "https://api.github.com/repos/qaqmin/TiebaLite",
            UpdateDownloader.rootFor(UpdateOrigin.GITHUB)
        )
    }

    @Test
    fun `default origin is github`() {
        assertEquals(UpdateOrigin.GITHUB, DEFAULT_ORIGIN)
    }

    @Test
    fun `otherOrigin flips both ways`() {
        assertEquals(UpdateOrigin.GITHUB, UpdateDownloader.otherOrigin(UpdateOrigin.CODEBERG))
        assertEquals(UpdateOrigin.CODEBERG, UpdateDownloader.otherOrigin(UpdateOrigin.GITHUB))
    }

    // ---- stable 通道候选集选版 ----

    @Test
    fun `stable picks max base among non-prerelease non-ai`() {
        val selected = pickLatestStable(
            listOf(rel("v4.0.0"), rel("v4.0.1"), rel("v3.9.0"))
        )
        assertEquals("v4.0.1", selected?.tagName)
    }

    @Test
    fun `stable excludes prerelease and ai tags`() {
        // Codeberg 的真实 ai release 是 prerelease=true；GitHub 侧 ai release
        // prerelease=false——两个判据都必须有，否则会选到预发布
        val selected = pickLatestStable(
            listOf(
                rel("v4.0.0-ai.51", prerelease = true),
                rel("v4.0.0-ai.50", prerelease = false),
                rel("v4.0.0", prerelease = false)
            )
        )
        assertEquals("v4.0.0", selected?.tagName)
    }

    @Test
    fun `stable base wins over higher prerelease ver of older base`() {
        val selected = pickLatestStable(
            listOf(rel("v4.0.0-ai.99", prerelease = true), rel("v4.0.1"))
        )
        assertEquals("v4.0.1", selected?.tagName)
    }

    @Test
    fun `stable unparseable tags and empty list yield null`() {
        assertNull(pickLatestStable(emptyList()))
        assertNull(pickLatestStable(listOf(rel("garbage-tag"))))
        assertNull(pickLatestStable(listOf(rel("v4.0.0-ai.1", prerelease = true))))
    }

    // ---- ETag 条件请求决策（resolveListingBody）----

    @Test
    fun `conditional 304 with cache reuses cached body`() {
        val cached = CachedListing("\"etag-v1\"", "[{\"tag_name\":\"v4.0.54\"}]")
        assertEquals(cached.body, resolveListingBody(304, cached, null))
    }

    @Test
    fun `conditional 200 uses fresh body`() {
        val fresh = "[{\"tag_name\":\"v4.0.55\"}]"
        assertEquals(fresh, resolveListingBody(200, null, fresh))
    }

    @Test
    fun `conditional 200 prefers fresh body over stale cache`() {
        val cached = CachedListing("\"etag-v1\"", "old-body")
        assertEquals("new-body", resolveListingBody(200, cached, "new-body"))
    }

    @Test
    fun `conditional 304 without cache yields null`() {
        assertNull(resolveListingBody(304, null, null))
    }

    @Test
    fun `conditional 403 rate limit yields null for fallback`() {
        // GitHub 匿名限额 60 次/小时/IP：403 时必须返回 null 让调用方回退备用渠道
        assertNull(resolveListingBody(403, null, null))
        assertNull(resolveListingBody(403, CachedListing("\"e\"", "body"), null))
    }

    @Test
    fun `conditional 429 and 5xx yield null`() {
        assertNull(resolveListingBody(429, null, null))
        assertNull(resolveListingBody(500, null, null))
        assertNull(resolveListingBody(502, null, null))
    }
}
