package com.huanchengfly.tieba.post.utils

/**
 * 去掉贴吧分享链接里的跟踪参数。
 *
 * 参数清单参考 ForbidAd4TieBa（GPL-3.0）的 ShareTrackingParamCleanerHook。
 * 只处理 baidu.com 及其子域名的链接，其他站点的参数可能有业务含义，原样返回。
 * 纯字符串处理、不依赖 android.net.Uri，便于单元测试。
 */
object ShareLinkCleaner {
    private val TRACKING_PARAM_KEYS = setOf(
        "sfc",
        "client_type",
        "client_version",
        "st",
        "is_video",
        "unique",
    )

    fun clean(url: String): String {
        if (!isBaiduUrl(url)) return url
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url

        val fragmentStart = url.indexOf('#', queryStart + 1)
        val queryEnd = if (fragmentStart >= 0) fragmentStart else url.length
        val segments = url.substring(queryStart + 1, queryEnd).split('&').filter { it.isNotEmpty() }
        val kept = segments.filterNot { isTrackingSegment(it) }
        if (kept.size == segments.size) return url

        val base = url.substring(0, queryStart)
        val fragment = if (fragmentStart >= 0) url.substring(fragmentStart) else ""
        return if (kept.isEmpty()) base + fragment else "$base?${kept.joinToString("&")}$fragment"
    }

    private fun isTrackingSegment(segment: String): Boolean =
        segment.substringBefore('=').lowercase() in TRACKING_PARAM_KEYS

    private fun isBaiduUrl(url: String): Boolean {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return false
        val host = afterScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')
            .substringBefore(':')
            .lowercase()
        return host == "baidu.com" || host.endsWith(".baidu.com")
    }
}
