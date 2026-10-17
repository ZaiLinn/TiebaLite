package com.huanchengfly.tieba.post.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 版本比对规则矩阵（设计文档 version-and-update-conventions.md）：
 * - ai 段整数比对（ai.9 → ai.10 升级、ai.10 → ai.9 不提示）
 * - base 段语义化比对
 * - 跨通道：ai 本地遇同 base stable 提示可升；stable 本地不推 ai 预发布
 * - 相等判等；+sha 构建元数据丢弃；v 前缀容忍
 */
class VersionCompatTest {

    private fun cmp(local: String, remote: String): CompareResult =
        VersionCompat.compare(local, remote)

    // ---- ai 段整数比对（SemVer 字典序会算反的核心用例）----

    @Test
    fun `ai9 to ai10 is newer`() {
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.9", "4.0.0-ai.10"))
    }

    @Test
    fun `ai10 to ai9 is NOT newer`() {
        assertEquals(CompareResult.Older, cmp("4.0.0-ai.10", "4.0.0-ai.9"))
    }

    @Test
    fun `ai50 to ai51 is newer`() {
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.50", "4.0.0-ai.51"))
    }

    @Test
    fun `ai100 to ai99 is older`() {
        assertEquals(CompareResult.Older, cmp("4.0.0-ai.100", "4.0.0-ai.99"))
    }

    @Test
    fun `ai equal versions are equal`() {
        assertEquals(CompareResult.Equal, cmp("4.0.0-ai.50", "4.0.0-ai.50"))
    }

    // ---- base 段语义化比对 ----

    @Test
    fun `base patch bump is newer regardless of ai`() {
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.50", "4.0.1-ai.1"))
        assertEquals(CompareResult.Newer, cmp("4.0.0", "4.0.1"))
        assertEquals(CompareResult.Older, cmp("4.1.0-ai.1", "4.0.9-ai.99"))
    }

    @Test
    fun `base major and minor bumps`() {
        assertEquals(CompareResult.Newer, cmp("4.0.0", "5.0.0"))
        assertEquals(CompareResult.Newer, cmp("4.0.0", "4.1.0"))
        assertEquals(CompareResult.Older, cmp("4.2.0", "4.1.9"))
    }

    // ---- 跨通道 ----

    @Test
    fun `ai local with same-base stable remote prompts upgrade back to stable track`() {
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.50", "4.0.0"))
    }

    @Test
    fun `ai local with newer-base stable remote prompts`() {
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.50", "4.0.1"))
    }

    @Test
    fun `stable local does NOT get ai prerelease at same base`() {
        assertEquals(CompareResult.Older, cmp("4.0.0", "4.0.0-ai.51"))
    }

    @Test
    fun `stable local does not get ai prerelease even with higher preVer`() {
        assertEquals(CompareResult.Older, cmp("4.0.0", "4.0.0-ai.999"))
    }

    // ---- 相等判等 ----

    @Test
    fun `equal stable versions are equal`() {
        assertEquals(CompareResult.Equal, cmp("4.0.0", "4.0.0"))
    }

    // ---- +sha 构建元数据容忍（比对时丢弃）----

    @Test
    fun `sha suffix is ignored in comparison`() {
        assertEquals(CompareResult.Equal, cmp("4.0.0-ai.50+8196b4a", "4.0.0-ai.50"))
        assertEquals(CompareResult.Equal, cmp("4.0.0-ai.50", "4.0.0-ai.50+deadbeef"))
        assertEquals(CompareResult.Equal, cmp("4.0.0+1234567", "4.0.0"))
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.49+aaaaaaa", "4.0.0-ai.50+bbbbbbb"))
    }

    // ---- v 前缀容忍 ----

    @Test
    fun `v prefix is tolerated`() {
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.50", "v4.0.0-ai.51"))
        assertEquals(CompareResult.Equal, cmp("v4.0.0-ai.50", "4.0.0-ai.50"))
        assertEquals(CompareResult.Newer, cmp("v4.0.0-ai.49", "V4.0.0-ai.50"))
    }

    @Test
    fun `v prefix with sha suffix combined`() {
        assertEquals(CompareResult.Equal, cmp("v4.0.0-ai.50+8196b4a", "4.0.0-ai.50"))
    }

    // ---- 无法解析 → Incompatible ----

    @Test
    fun `garbage tags are incompatible`() {
        assertEquals(CompareResult.Incompatible, cmp("4.0.0-ai.50", ""))
        assertEquals(CompareResult.Incompatible, cmp("4.0.0-ai.50", "not-a-version"))
        assertEquals(CompareResult.Incompatible, cmp("", "4.0.0-ai.51"))
    }

    @Test
    fun `empty sha-only string is incompatible`() {
        assertEquals(CompareResult.Incompatible, cmp("+abcdef", "4.0.0"))
    }

    // ---- 解析器细节 ----

    @Test
    fun `parse tolerates whitespace`() {
        assertEquals(CompareResult.Equal, cmp("  4.0.0-ai.50  ", "4.0.0-ai.50"))
    }

    @Test
    fun `channel detection`() {
        assertEquals(UpdateChannel.AI, VersionCompat.channelOf("4.0.0-ai.50"))
        assertEquals(UpdateChannel.AI, VersionCompat.channelOf("v4.0.0-ai.7+deadbee"))
        assertEquals(UpdateChannel.STABLE, VersionCompat.channelOf("4.0.0"))
        assertEquals(UpdateChannel.STABLE, VersionCompat.channelOf("v4.0.0+8196b4a"))
    }

    @Test
    fun `parse failures return null`() {
        assertNull(VersionCompat.parse(""))
        assertNull(VersionCompat.parse("abc"))
        assertNull(VersionCompat.parse("4.o.o"))
        // "4.0" 是合法 SemVer（缺省段补 0），不返回 null
        assertEquals(CompareResult.Equal, VersionCompat.compare("4.0", "4.0.0"))
    }

    @Test
    fun `parse keeps preRelease fields`() {
        val parsed = VersionCompat.parse("v4.0.0-ai.50+8196b4a")!!
        assertEquals("4.0.0", parsed.base.toString())
        assertEquals("ai", parsed.preReleaseName)
        assertEquals(50, parsed.preReleaseVer)
        assertEquals(UpdateChannel.AI, parsed.channel)
    }

    @Test
    fun `non-ai prerelease channel falls back to stable`() {
        // beta/rc 等非 ai 预发布段不算 ai 通道
        val parsed = VersionCompat.parse("4.0.0-beta.1")!!
        assertEquals(UpdateChannel.STABLE, parsed.channel)
    }

    // ---- ai 段非数字容忍 ----

    @Test
    fun `ai segment with non-numeric ver is treated as stable channel`() {
        val parsed = VersionCompat.parse("4.0.0-ai.x")!!
        assertNull(parsed.preReleaseVer)
        assertEquals(UpdateChannel.STABLE, parsed.channel)
    }

    // ---- min_version 强更解析（GitHubRelease body 约定行）----

    @Test
    fun `minVersion parsed from release body`() {
        val release = GitHubRelease(
            tagName = "v4.0.0-ai.51",
            name = "test",
            htmlUrl = "",
            body = "更新内容\nmin_version: 400005\n其他",
            isPrerelease = true,
            apkUrl = null,
            apkName = null,
            apkDigest = null
        )
        assertEquals(400005, release.minVersion)
        // 约定行不出现在展示用 changelog
        assertFalse(release.changelog().contains("min_version"))
        assertTrue(release.changelog().contains("更新内容"))
    }

    @Test
    fun `minVersion absent returns null`() {
        val release = GitHubRelease(
            tagName = "v4.0.0-ai.51",
            name = "test",
            htmlUrl = "",
            body = "普通更新说明，无强更标记",
            isPrerelease = true,
            apkUrl = null,
            apkName = null,
            apkDigest = null
        )
        assertNull(release.minVersion)
    }

    // ---- release 资产挑选 ----
    // 注：pickApkAsset 依赖 org.json，android.jar stub（returnDefaultValues）下方法返回默认值，
    // 纯 JVM 单测无法覆盖真实 JSON 行为，资产挑选由本地 mock 设备端全链路验收覆盖。

    // ---- ai 通道候选集选版（直接调生产函数 pickLatestAi，非镜像副本）----
    // P1 防回归：base 优先排序（只比 preVer 时 base 升级窗口内旧 base 高序号压过新 base）；
    // P0-2 防回归：判据只认 isAiTag 不认 isPrerelease（真实 CI 未加 --prerelease，
    // 真实 ai release 的 prerelease=false，按 isPrerelease 筛会把它滤空、两通道同时空转）。

    private fun rel(tag: String, prerelease: Boolean = false) = GitHubRelease(
        tagName = tag, name = tag, htmlUrl = "", body = "",
        isPrerelease = prerelease, apkUrl = null, apkName = null, apkDigest = null
    )

    @Test
    fun `mixed base candidates - higher base wins regardless of ai ver`() {
        // base 升级窗口内的真实事故形态：4.0.0-ai.51(preVer=51) vs 4.0.1-ai.1(preVer=1)
        val selected = pickLatestAi(
            listOf(rel("4.0.0-ai.51"), rel("4.0.1-ai.1"), rel("4.0.0-ai.49"))
        )
        assertEquals("4.0.1-ai.1", selected?.tagName)
    }

    @Test
    fun `same base candidates - higher ai ver wins`() {
        val selected = pickLatestAi(
            listOf(rel("4.0.0-ai.9"), rel("4.0.0-ai.51"), rel("4.0.0-ai.10"))
        )
        assertEquals("4.0.0-ai.51", selected?.tagName)
    }

    @Test
    fun `prerelease=false does not exclude ai release - P0-2 real CI data`() {
        // 真实数据形态：/releases 返回的 ai tag prerelease=false（CI 未加 --prerelease），
        // 旧判据 isPrerelease&&isAiTag 会滤空；现判据必须选中
        val selected = pickLatestAi(
            listOf(rel("v4.0.0-ai.50", prerelease = false), rel("v4.0.0-ai.49", prerelease = false))
        )
        assertEquals("v4.0.0-ai.50", selected?.tagName)
    }

    @Test
    fun `stable and unparseable tags are excluded from ai channel`() {
        val selected = pickLatestAi(
            listOf(rel("v4.0.0"), rel("garbage-tag"), rel("4.0.0-ai.7"))
        )
        assertEquals("4.0.0-ai.7", selected?.tagName)
    }

    @Test
    fun `selected latest ai release compares Newer against any older-base local`() {
        // 选出的 release 对旧 base 本地版本必须判 Newer（不再有 Equal 静默屏蔽）
        val selected = pickLatestAi(listOf(rel("4.0.0-ai.51"), rel("4.0.1-ai.1")))
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.51", selected!!.tagName))
        assertEquals(CompareResult.Newer, cmp("4.0.0-ai.49", selected.tagName))
    }

    // ---- SemVer 库语义（4.0 缺省段补 0）----

    @Test
    fun `two segment version is valid and equals three segment`() {
        // SemVer.parse("4.0") 合法（缺省段补 0）→ 4.0 与 4.0.0 判等
        assertEquals(CompareResult.Equal, cmp("4.0", "4.0.0"))
        assertEquals(CompareResult.Newer, cmp("4.0", "4.1.0"))
    }
}
