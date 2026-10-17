package com.huanchengfly.tieba.post.update

import org.json.JSONArray
import org.json.JSONObject

/**
 * GitHub Release 元数据（只保留更新流程需要的字段）。
 */
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val htmlUrl: String,
    val body: String,
    val isPrerelease: Boolean,
    val apkUrl: String?,
    val apkName: String?,
    /** 资产 SHA256（形如 "sha256:..."，可能为 null，此时跳过完整性校验）。 */
    val apkDigest: String?
) {

    val isAiTag: Boolean
        get() = tagName.contains("-ai.")

    /**
     * 强更最低 versionCode（release body 约定行，如 "min_version: 400005"），无则 null。
     */
    val minVersion: Int?
        get() = MIN_VERSION_REGEX.find(body)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * 展示用的更新说明（去掉约定行后的正文，截断到 [maxLen]）。
     */
    fun changelog(maxLen: Int = 600): String {
        val cleaned = body
            .lineSequence()
            .filterNot { it.trim().startsWith("min_version") }
            .joinToString("\n")
            .trim()
        if (cleaned.isEmpty()) return name.ifEmpty { tagName }
        return if (cleaned.length <= maxLen) cleaned else cleaned.take(maxLen) + "…"
    }

    companion object {
        private val MIN_VERSION_REGEX = Regex("""min_version\s*[:=]\s*(\d+)""", RegexOption.IGNORE_CASE)

        /**
         * 从 assets 中按约定挑选 release-*.apk 资产（并要求文件名含 versionName，避免误选 debug 包）。
         */
        fun pickApkAsset(tagName: String, assets: JSONArray): Triple<String, String, String?>? {
            val versionName = tagName.removePrefix("v")
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                if (assetName.startsWith("release-") && assetName.endsWith(".apk") &&
                    assetName.contains(versionName) && url.isNotEmpty()
                ) {
                    return Triple(url, assetName, asset.optString("digest", "").ifEmpty { null })
                }
            }
            return null
        }

        fun fromJson(obj: JSONObject): GitHubRelease? {
            val tagName = obj.optString("tag_name", "")
            if (tagName.isEmpty()) return null
            val assets = obj.optJSONArray("assets")
            val picked = if (assets != null) pickApkAsset(tagName, assets) else null
            return GitHubRelease(
                tagName = tagName,
                name = obj.optString("name", tagName).ifEmpty { tagName },
                htmlUrl = obj.optString("html_url", ""),
                body = obj.optString("body", ""),
                isPrerelease = obj.optBoolean("prerelease", false),
                apkUrl = picked?.first,
                apkName = picked?.second,
                apkDigest = picked?.third
            )
        }

        fun fromJsonArray(text: String): List<GitHubRelease> {
            val array = JSONArray(text)
            val result = ArrayList<GitHubRelease>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                fromJson(obj)?.let { result.add(it) }
            }
            return result
        }
    }
}
