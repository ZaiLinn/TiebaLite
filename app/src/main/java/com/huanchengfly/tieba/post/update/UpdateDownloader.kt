package com.huanchengfly.tieba.post.update

import android.os.Build
import android.util.Log
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.utils.AppLog
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.DigestOutputStream
import java.security.MessageDigest

/**
 * 更新检查平台来源（DataStore 持久化）。
 * 只检查 GitHub（ZaiLinn/TiebaLite）；上游的 Codeberg 渠道已移除，避免检测到上游版本。
 */
enum class UpdateOrigin {
    GITHUB
}

/** 默认平台来源：GitHub（当前主构建线）。 */
val DEFAULT_ORIGIN = UpdateOrigin.GITHUB

/**
 * 更新元数据拉取 + APK 下载。只用 HttpURLConnection（无 token，匿名访问 Releases API），
 * debug 构建允许通过系统属性覆盖 API 根地址用于本地 mock。
 */
object UpdateDownloader {

    private const val TAG = "UpdateDownloader"

    private const val GITHUB_API_ROOT = "https://api.github.com/repos/ZaiLinn/TiebaLite"

    /** 网页地址：Releases API 匿名限额按 IP 计（60 次/小时），VPN 等共享出口常被用完，此时改走网页 */
    private const val GITHUB_WEB_ROOT = "https://github.com/ZaiLinn/TiebaLite"

    /** 平台来源对应的 Releases API 根地址。 */
    fun rootFor(origin: UpdateOrigin): String = when (origin) {
        UpdateOrigin.GITHUB -> GITHUB_API_ROOT
    }

    /** debug 构建可覆盖的 API 根地址（本地 mock 用），release 恒为官方地址。 */
    val apiRoot: String
        get() = if (BuildConfig.DEBUG) {
            System.getProperty("tieba.update.apiRoot") ?: rootFor(DEFAULT_ORIGIN)
        } else {
            rootFor(DEFAULT_ORIGIN)
        }

    /**
     * stable 通道端点：recent releases 中筛"非预发布且非 ai tag"取版本最大者。
     * 不用 /releases/latest：Codeberg (Forgejo) 的 latest 语义排除 prerelease，
     * 平台只有预发布时该端点 404（v4.0.0-ai.51 发布时实测）；列表筛选两端行为一致。
     * App 侧仍二次剔除 ai tag（GitHub 的 ai release prerelease=false，只按
     * isPrerelease 筛会漏，该护栏是 stable/ai 通道分离的最后防线）。
     *
     * [cacheDir] 非空时启用 ETag 条件请求：GitHub 匿名 API 限额 60 次/小时/IP，
     * 命中 304 的请求不计入限额（GitHub 官方行为），可显著降低用户撞 403 的概率；
     * 403/网络失败返回 null，由调用方处理。
     */
    fun fetchStableRelease(root: String = apiRoot, cacheDir: File? = null): GitHubRelease? {
        val json = httpGetConditional("${root.trimEnd('/')}/releases?per_page=10", cacheDir)
        if (json == null) {
            // 只对官方地址兜底（debug mock 源不走网页）
            return if (root.trimEnd('/') == GITHUB_API_ROOT) fetchLatestStableFromWeb() else null
        }
        return pickLatestStable(GitHubRelease.fromJsonArray(json))
    }

    /**
     * 不经 API 获取最新正式版：releases/latest 会 302 到最新正式版（不含预发布）的 tag 页，
     * expanded_assets/<tag> 页面里有 APK 下载链接。网页拿不到更新说明和 SHA-256，
     * 所以没有强更判断，下载后也不做完整性校验（安装页会提示未校验）。
     */
    private fun fetchLatestStableFromWeb(): GitHubRelease? {
        return try {
            val conn = openConnection("$GITHUB_WEB_ROOT/releases/latest") ?: return null
            conn.instanceFollowRedirects = false
            val code = conn.responseCode
            val tag = parseTagFromReleaseLocation(conn.getHeaderField("Location").orEmpty())
            conn.disconnect()
            if (tag == null || tag.contains("-ai.")) {
                AppLog.d(TAG, "web fallback: no stable tag (HTTP $code)")
                return null
            }
            val html = httpGet("$GITHUB_WEB_ROOT/releases/expanded_assets/$tag") ?: return null
            val href = findReleaseApkHref(html, tag)
            AppLog.d(TAG, "web fallback: tag=$tag apk=$href")
            GitHubRelease(
                tagName = tag,
                name = tag,
                htmlUrl = "$GITHUB_WEB_ROOT/releases/tag/$tag",
                body = "",
                isPrerelease = false,
                apkUrl = href?.let { "https://github.com$it" },
                apkName = href?.substringAfterLast('/'),
                apkDigest = null
            )
        } catch (e: Exception) {
            AppLog.d(TAG, "web fallback failed: $e")
            null
        }
    }

    /**
     * ai 通道端点：recent releases 中筛 ai tag（tag 含 -ai.）取最新者。
     * 判据只认 isAiTag 不认 isPrerelease：真实 CI 未加 --prerelease，
     * 真实 ai release 的 prerelease=false，按 isPrerelease 筛会把它滤空、
     * 两通道同时空转（P0-2 实测）。版本解析失败与 stable tag 由 [pickLatestAi] 统一剔除。
     */
    fun fetchLatestAiRelease(root: String = apiRoot, cacheDir: File? = null): GitHubRelease? {
        // trimEnd('/')：mockRoot/自定义根尾带斜杠时防 "$root/..." 产生 // 双斜杠 404
        val json = httpGetConditional("${root.trimEnd('/')}/releases?per_page=10", cacheDir) ?: return null
        return pickLatestAi(GitHubRelease.fromJsonArray(json))
    }

    /**
     * 下载 APK 到目标文件（调用方保证父目录已存在）。
     * 流式下载同时计算 SHA-256，完成后与 release 资产 digest（若提供）比对，
     * 不匹配则删除半成品并报错。
     */
    fun downloadApk(
        release: GitHubRelease,
        target: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): DownloadResult {
        val url = release.apkUrl ?: return DownloadResult.Error("没有可用的安装包")
        try {
            val conn = openConnection(url) ?: return DownloadResult.Error("无法连接服务器")
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) {
                return DownloadResult.Error("下载失败（HTTP $code）")
            }
            // contentLengthLong 需要 API 24，minSdk 是 21
            val total = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) conn.contentLengthLong
            else conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            DigestOutputStream(FileOutputStream(target), digest).use { out ->
                conn.inputStream.use { input ->
                    val buf = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                    while (true) {
                        val read = input.read(buf)
                        if (read < 0) break
                        out.write(buf, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            val expected = release.apkDigest?.removePrefix("sha256:")?.lowercase()
            if (!expected.isNullOrEmpty() && actual != expected) {
                target.delete()
                return DownloadResult.Error("安装包校验失败")
            }
            return DownloadResult.Success(target, actual)
        } catch (e: IOException) {
            target.delete()
            return DownloadResult.Error(e.message ?: "下载失败")
        } catch (e: Exception) {
            Log.w(TAG, "downloadApk failed", e)
            target.delete()
            return DownloadResult.Error("下载失败")
        } finally {
            runCatching { }
        }
    }

    private fun openConnection(url: String): HttpURLConnection? {
        return try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "TiebaLite-Update")
            }
        } catch (e: Exception) {
            Log.w(TAG, "openConnection failed: $e")
            null
        }
    }

    private fun httpGet(url: String): String? {
        return try {
            val conn = openConnection(url) ?: return null
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "HTTP $code for $url")
                return null
            }
            conn.inputStream.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            Log.w(TAG, "httpGet failed: $e")
            null
        }
    }

    /**
     * 带 ETag 条件请求的 GET：[cacheDir] 为空时退化为普通 GET。
     * 缓存按 URL 哈希落盘（etag + body 成对），304 时直接复用缓存 body——
     * GitHub 明确 304 不计入匿名限额，Forgejo 同样支持 ETag。
     */
    private fun httpGetConditional(url: String, cacheDir: File?): String? {
        if (cacheDir == null) return httpGet(url)
        return try {
            val cached = readListingCache(cacheDir, url)
            val conn = openConnection(url) ?: return null
            if (cached != null) conn.setRequestProperty("If-None-Match", cached.etag)
            val code = conn.responseCode
            val freshBody =
                if (code in 200..299) conn.inputStream.use { it.bufferedReader().readText() } else null
            val resolved = resolveListingBody(code, cached, freshBody)
            if (resolved == null && code != 304) {
                // 403/429 且剩余次数为 0 即匿名限额用完
                AppLog.d(
                    TAG,
                    "HTTP $code for $url, ratelimit-remaining=${conn.getHeaderField("X-RateLimit-Remaining")}"
                )
            }
            if (code in 200..299 && freshBody != null) {
                val etag = conn.getHeaderField("ETag")
                if (!etag.isNullOrBlank()) writeListingCache(cacheDir, url, etag, freshBody)
            }
            resolved
        } catch (e: Exception) {
            AppLog.d(TAG, "httpGetConditional failed: $e")
            null
        }
    }

    private fun listingCacheFile(cacheDir: File, url: String): File {
        val digest = MessageDigest.getInstance("MD5")
            .digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(cacheDir, "update_listing_$digest.json")
    }

    private fun readListingCache(cacheDir: File, url: String): CachedListing? {
        return try {
            val f = listingCacheFile(cacheDir, url)
            if (!f.isFile) return null
            val obj = JSONObject(f.readText())
            val etag = obj.optString("etag").takeIf { it.isNotBlank() } ?: return null
            val body = obj.optString("body").takeIf { it.isNotBlank() } ?: return null
            CachedListing(etag, body)
        } catch (e: Exception) {
            null
        }
    }

    private fun writeListingCache(cacheDir: File, url: String, etag: String, body: String) {
        try {
            cacheDir.mkdirs()
            val obj = JSONObject()
            obj.put("etag", etag)
            obj.put("body", body)
            listingCacheFile(cacheDir, url).writeText(obj.toString())
        } catch (e: Exception) {
            Log.w(TAG, "writeListingCache failed: $e")
        }
    }

    private const val DEFAULT_BUFFER_SIZE = 8192
}

/** ETag 条件请求的缓存条目（etag 与响应体成对落盘）。 */
internal data class CachedListing(val etag: String, val body: String)

/**
 * 条件请求体决策（纯函数，可 JVM 单测）：
 * - 304 且有缓存 → 复用缓存 body（不计入平台匿名限额）；
 * - 2xx → 用新拉取的 body（ETag 由调用方落盘）；
 * - 其余（403 限额 / 429 / 5xx / 网络异常经上层转 null）→ null，调用方回退备用渠道。
 */
internal fun resolveListingBody(code: Int, cached: CachedListing?, freshBody: String?): String? =
    when {
        code == 304 && cached != null -> cached.body
        code in 200..299 -> freshBody
        else -> null
    }

/** 从 releases/latest 的跳转地址（…/releases/tag/<tag>）取出 tag，非该格式返回 null */
internal fun parseTagFromReleaseLocation(location: String): String? =
    location.substringAfter("/releases/tag/", "").substringBefore('?').takeIf { it.isNotBlank() }

/** 在 expanded_assets 页面里找发版 APK（release-*.apk，文件名含版本号）的下载路径 */
internal fun findReleaseApkHref(html: String, tagName: String): String? {
    val versionName = tagName.removePrefix("v")
    return Regex("""href="(/[^"]+/releases/download/[^"]+/(release-[^"/]+\.apk))"""")
        .findAll(html)
        .firstOrNull { it.groupValues[2].contains(versionName) }
        ?.groupValues?.get(1)
}

/**
 * ai 通道候选集选版（生产侧纯函数，可 JVM 单测）：
 * 1. 只留 tag 含 -ai. 的候选（isAiTag——不依赖 CI 是否加 --prerelease，真实 CI 未加）；
 * 2. tag 解析失败的剔除；
 * 3. base 优先排序取最大：base 高者恒胜，preVer 仅在同 base 内比较
 *    （只比 preVer 时 base 升级窗口内 4.0.0-ai.51 会压过 4.0.1-ai.1 选出旧 base，
 *    经 compare 判 Equal → 新 base 推送被静默屏蔽，每个 base 升级必触发）。
 */
fun pickLatestAi(candidates: List<GitHubRelease>): GitHubRelease? {
    return candidates.asSequence()
        .filter { it.isAiTag }
        .mapNotNull { release ->
            VersionCompat.parse(release.tagName)?.let { release to it }
        }
        .maxWithOrNull(
            compareBy({ it.second.base }, { it.second.preReleaseVer ?: 0 })
        )
        ?.first
}

/**
 * stable 通道候选集选版（生产侧纯函数，可 JVM 单测），判据与 [pickLatestAi] 镜像：
 * 1. 剔除 prerelease（两平台正式版均为非预发布；Codeberg 的 ai release 是 prerelease=true，
 *    仅靠 isPrerelease 剔除会在 GitHub 侧漏掉 prerelease=false 的 ai tag）；
 * 2. 剔除 ai tag；
 * 3. tag 解析失败的剔除；
 * 4. base 优先排序取最大：base 高者恒胜，preVer 仅在同 base 内比较。
 */
fun pickLatestStable(candidates: List<GitHubRelease>): GitHubRelease? {
    return candidates.asSequence()
        .filter { !it.isPrerelease && !it.isAiTag }
        .mapNotNull { release ->
            VersionCompat.parse(release.tagName)?.let { release to it }
        }
        .maxWithOrNull(
            compareBy({ it.second.base }, { it.second.preReleaseVer ?: 0 })
        )
        ?.first
}

sealed class DownloadResult {
    data class Success(val file: File, val sha256: String) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}
