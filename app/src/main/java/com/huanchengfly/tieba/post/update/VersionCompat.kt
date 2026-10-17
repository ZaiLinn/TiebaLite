package com.huanchengfly.tieba.post.update

import net.swiftzer.semver.SemVer

/**
 * 版本通道。
 */
enum class UpdateChannel {
    /** 正式版：versionName 不含 -ai.N 预发布段。 */
    STABLE,

    /** 体验版：versionName 含 -ai.N 预发布段，预发布序号按整数比对。 */
    AI
}

/**
 * 解析后的版本号。
 *
 * [base] 为语义化版本主段；[preReleaseName]/[preReleaseVer] 为 -ai.N 预发布段（可空）。
 */
data class ParsedVersion(
    val base: SemVer,
    val preReleaseName: String?,
    val preReleaseVer: Int?
) {
    val channel: UpdateChannel
        get() = if (preReleaseName == "ai" && preReleaseVer != null) UpdateChannel.AI else UpdateChannel.STABLE
}

/**
 * 版本比对结果。
 */
sealed class CompareResult {
    /** 远端有更新，需要提示。 */
    data object Newer : CompareResult()

    /** 版本相同。 */
    data object Equal : CompareResult()

    /** 远端更旧，不提示。 */
    data object Older : CompareResult()

    /** 无法解析，不提示。 */
    data object Incompatible : CompareResult()
}

/**
 * 版本串解析与比对工具。
 *
 * 容忍格式：v/V 前缀、-ai.N 预发布段（按整数比对，不走 SemVer 的字典序）、+sha 构建元数据（比对时丢弃）。
 * 纯 Kotlin 实现，无 Android 依赖，可单元测试。
 */
object VersionCompat {

    /**
     * 解析版本串，失败返回 null。
     */
    fun parse(raw: String): ParsedVersion? {
        var s = raw.trim()
        if (s.isEmpty()) return null
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1)
        // 丢弃 +sha 构建元数据
        s = s.substringBefore('+')
        if (s.isEmpty()) return null

        val basePart: String
        var preName: String? = null
        var preVer: Int? = null
        val dash = s.indexOf('-')
        if (dash >= 0) {
            basePart = s.substring(0, dash)
            val pre = s.substring(dash + 1)
            val dot = pre.indexOf('.')
            if (dot >= 0) {
                preName = pre.substring(0, dot).ifEmpty { null }
                preVer = pre.substring(dot + 1).toIntOrNull()
            } else {
                preName = pre.ifEmpty { null }
            }
        } else {
            basePart = s
        }

        val base = try {
            SemVer.parse(basePart)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return ParsedVersion(base = base, preReleaseName = preName, preReleaseVer = preVer)
    }

    /**
     * 判定本地版本所属通道。
     */
    fun channelOf(versionName: String): UpdateChannel {
        return parse(versionName)?.channel ?: UpdateChannel.STABLE
    }

    /**
     * 比对本地与远端版本串（容忍 v 前缀 / +sha 后缀）。
     */
    fun compare(local: String, remote: String): CompareResult {
        val localParsed = parse(local) ?: return CompareResult.Incompatible
        val remoteParsed = parse(remote) ?: return CompareResult.Incompatible
        return compare(localParsed, remoteParsed)
    }

    /**
     * 比对已解析的本地与远端版本。
     *
     * 规则：
     * - base 段用语义化比对，base 不相等时直接决出高低；
     * - base 相等且同为 ai 通道：预发布序号按整数比对；
     * - base 相等且 ai 本地 / stable 远端：视为远端更新（引导回正式轨道）；
     * - base 相等且 stable 本地 / ai 远端：不推送预发布（SemVer 语义中预发布低于正式版）。
     */
    fun compare(local: ParsedVersion, remote: ParsedVersion): CompareResult {
        when {
            remote.base > local.base -> return CompareResult.Newer
            remote.base < local.base -> return CompareResult.Older
        }

        val localChannel = local.channel
        val remoteChannel = remote.channel
        if (localChannel == remoteChannel) {
            if (localChannel == UpdateChannel.STABLE) return CompareResult.Equal
            val localPre = local.preReleaseVer ?: 0
            val remotePre = remote.preReleaseVer ?: 0
            return when {
                remotePre > localPre -> CompareResult.Newer
                remotePre < localPre -> CompareResult.Older
                else -> CompareResult.Equal
            }
        }

        return if (localChannel == UpdateChannel.AI && remoteChannel == UpdateChannel.STABLE) {
            // ai 本地遇到同 base 的正式版：提示可升，引导回正式轨道
            CompareResult.Newer
        } else {
            // stable 本地不推 ai 预发布
            CompareResult.Older
        }
    }
}
