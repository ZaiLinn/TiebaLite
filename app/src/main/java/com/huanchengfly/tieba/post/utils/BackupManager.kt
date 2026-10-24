package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.TopForum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.findAll

/**
 * 把本地数据导出成一个 JSON 文件，换机或重装后导入。
 *
 * 包含：设置、屏蔽规则、草稿、置顶的吧、浏览历史。
 * 不包含账号（BDUSS 等登录凭据不应该出现在用户随手保存、分享的文件里）、
 * 设备标识（client_id 等）以及只对本机有意义的值。收藏存在贴吧服务器上，登录后自然就有。
 */
object BackupManager {
    const val FORMAT = "tiebalite-backup"
    const val FORMAT_VERSION = 1

    /** 不导出、也不导入的设置项 */
    internal val EXCLUDED_PREFERENCE_KEYS = setOf(
        // 设备标识
        "client_id",
        "sample_id",
        "baidu_id",
        "active_timestamp",
        // 只对本机或本次安装有意义
        "sign_day",
        "userLikeLastRequestUnix",
        "imeHeight",
        "translucent_theme_background_path",
        "update_last_check_time",
        "update_origin",
    )

    private val gson = GsonBuilder().setPrettyPrinting().create()

    @Keep
    data class Backup(
        val format: String = FORMAT,
        val version: Int = FORMAT_VERSION,
        val appVersion: String = "",
        val createTime: Long = 0,
        val preferences: List<PreferenceItem>? = null,
        val blocks: List<BlockItem>? = null,
        val drafts: List<DraftItem>? = null,
        val topForums: List<String>? = null,
        val histories: List<HistoryItem>? = null,
    )

    @Keep
    data class PreferenceItem(
        val key: String,
        val type: String,
        val value: JsonElement,
    )

    @Keep
    data class BlockItem(
        val category: Int,
        val type: Int,
        val keywords: String?,
        val username: String?,
        val uid: String?,
    )

    @Keep
    data class DraftItem(
        val hash: String?,
        val content: String?,
        val threadId: Long,
        val forumId: Long,
        val forumName: String?,
        val postId: Long,
        val createTime: Long,
        val contentPreview: String?,
    )

    @Keep
    data class HistoryItem(
        val title: String,
        val data: String,
        val type: Int,
        val timestamp: Long,
        val count: Int,
        val extras: String?,
        val avatar: String?,
        val username: String?,
    )

    data class Summary(
        val preferences: Int,
        val blocks: Int,
        val drafts: Int,
        val topForums: Int,
        val histories: Int,
    )

    class InvalidBackupException : Exception("不是贴吧 Lite 的备份文件")

    suspend fun export(context: Context, uri: Uri): Summary = withContext(Dispatchers.IO) {
        val backup = Backup(
            appVersion = BuildConfig.VERSION_NAME,
            createTime = System.currentTimeMillis(),
            preferences = exportPreferences(context.dataStore.data.first()),
            blocks = LitePal.findAll<Block>().map {
                BlockItem(it.category, it.type, it.keywords, it.username, it.uid)
            },
            drafts = LitePal.findAll<Draft>().map {
                DraftItem(
                    it.hash, it.content, it.threadId, it.forumId, it.forumName,
                    it.postId, it.createTime, it.contentPreview
                )
            },
            topForums = LitePal.findAll<TopForum>().mapNotNull { it.forumId },
            histories = LitePal.findAll<History>().map {
                HistoryItem(
                    it.title, it.data, it.type, it.timestamp, it.count,
                    it.extras, it.avatar, it.username
                )
            },
        )
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("无法写入所选文件")
        output.bufferedWriter().use { gson.toJson(backup, it) }
        backup.summary()
    }

    /**
     * 导入是合并：设置项覆盖，屏蔽规则、置顶的吧去重后追加，草稿按 hash 覆盖，历史按 type + data 保留较新的。
     */
    suspend fun import(context: Context, uri: Uri): Summary = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("无法读取所选文件")
        val backup = input.bufferedReader().use { parse(it.readText()) }

        val preferences = backup.preferences.orEmpty()
            .filter { it.key !in EXCLUDED_PREFERENCE_KEYS }
        context.dataStore.edit { prefs ->
            preferences.forEach { applyPreference(prefs, it) }
        }

        val existingBlocks = LitePal.findAll<Block>()
            .mapTo(HashSet()) { BlockItem(it.category, it.type, it.keywords, it.username, it.uid) }
        val newBlocks = backup.blocks.orEmpty().filter { existingBlocks.add(it) }
        newBlocks.forEach {
            Block(it.category, it.type, it.keywords, it.username, it.uid).save()
        }

        val drafts = backup.drafts.orEmpty().filter { !it.hash.isNullOrEmpty() }
        drafts.forEach {
            Draft(
                it.hash, it.content, it.threadId, it.forumId, it.forumName,
                it.postId, it.createTime, it.contentPreview
            ).saveOrUpdate("hash = ?", it.hash)
        }

        val existingTopForums = LitePal.findAll<TopForum>().mapNotNullTo(HashSet()) { it.forumId }
        val newTopForums = backup.topForums.orEmpty().filter { existingTopForums.add(it) }
        newTopForums.forEach { TopForum(it).save() }

        val histories = backup.histories.orEmpty()
        histories.forEach { item ->
            val existing = LitePal.where("type = ? and data = ?", item.type.toString(), item.data)
                .findFirst(History::class.java)
            if (existing == null || existing.timestamp < item.timestamp) {
                History(
                    item.title, item.data, item.type, item.timestamp, item.count,
                    item.extras, item.avatar, item.username
                ).saveOrUpdate("type = ? and data = ?", item.type.toString(), item.data)
            }
        }

        BlockManager.reload()
        ReadThreads.init()

        Summary(
            preferences = preferences.size,
            blocks = newBlocks.size,
            drafts = drafts.size,
            topForums = newTopForums.size,
            histories = histories.size,
        )
    }

    internal fun parse(json: String): Backup {
        // Backup 的字段都有默认值，Gson 会走无参构造，缺字段时 format 也是默认值，所以先看原始 JSON
        val tree = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
        val format = tree?.get("format")?.takeIf { it.isJsonPrimitive }?.asString
        val version = tree?.get("version")?.takeIf { it.isJsonPrimitive }?.asInt
        if (tree == null || format != FORMAT || version == null || version > FORMAT_VERSION) {
            throw InvalidBackupException()
        }
        return runCatching { gson.fromJson(tree, Backup::class.java) }.getOrNull()
            ?: throw InvalidBackupException()
    }

    internal fun exportPreferences(preferences: Preferences): List<PreferenceItem> =
        preferences.asMap().mapNotNull { (key, value) ->
            if (key.name in EXCLUDED_PREFERENCE_KEYS) return@mapNotNull null
            when (value) {
                is Boolean -> PreferenceItem(key.name, "boolean", JsonPrimitive(value))
                is Int -> PreferenceItem(key.name, "int", JsonPrimitive(value))
                is Long -> PreferenceItem(key.name, "long", JsonPrimitive(value))
                is Float -> PreferenceItem(key.name, "float", JsonPrimitive(value))
                is Double -> PreferenceItem(key.name, "double", JsonPrimitive(value))
                is String -> PreferenceItem(key.name, "string", JsonPrimitive(value))
                is Set<*> -> PreferenceItem(
                    key.name,
                    "string_set",
                    JsonArray().apply { value.forEach { add(it.toString()) } }
                )

                else -> null
            }
        }.sortedBy { it.key }

    internal fun applyPreference(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        item: PreferenceItem,
    ) {
        runCatching {
            val value = item.value
            when (item.type) {
                "boolean" -> prefs[booleanPreferencesKey(item.key)] = value.asBoolean
                "int" -> prefs[intPreferencesKey(item.key)] = value.asInt
                "long" -> prefs[longPreferencesKey(item.key)] = value.asLong
                "float" -> prefs[floatPreferencesKey(item.key)] = value.asFloat
                "double" -> prefs[doublePreferencesKey(item.key)] = value.asDouble
                "string" -> prefs[stringPreferencesKey(item.key)] = value.asString
                "string_set" -> prefs[stringSetPreferencesKey(item.key)] =
                    value.asJsonArray.map { it.asString }.toSet()
            }
        }
    }

    private fun Backup.summary() = Summary(
        preferences = preferences.orEmpty().size,
        blocks = blocks.orEmpty().size,
        drafts = drafts.orEmpty().size,
        topForums = topForums.orEmpty().size,
        histories = histories.orEmpty().size,
    )
}
