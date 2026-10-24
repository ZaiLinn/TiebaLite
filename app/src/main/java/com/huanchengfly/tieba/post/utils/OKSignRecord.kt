package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import java.io.File

/**
 * 最近一次一键签到的结果。
 *
 * 签到跑在 :oksign 进程里，DataStore 不支持多进程读写，所以单独存成 filesDir 下的 JSON 文件，
 * 由签到进程写、设置页读。
 */
@Keep
data class OKSignRecord(
    val time: Long,
    val userName: String,
    val total: Int,
    val successCount: Int,
    val failures: List<Failure>,
) {
    @Keep
    data class Failure(
        val forumId: String,
        val forumName: String,
        val errorCode: Int,
        val errorMsg: String,
    )

    companion object {
        private const val TAG = "OKSignRecord"
        private const val FILE_NAME = "oksign_last_result.json"

        private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

        fun load(context: Context): OKSignRecord? = runCatching {
            val file = file(context)
            if (!file.exists()) return null
            GsonUtil.getGson().fromJson(file.readText(), OKSignRecord::class.java)
        }.onFailure {
            Log.w(TAG, "load failed", it)
        }.getOrNull()

        fun save(context: Context, record: OKSignRecord) {
            runCatching {
                val file = file(context)
                // 先写临时文件再改名，避免设置页读到写了一半的内容
                val tmp = File(file.parentFile, "$FILE_NAME.tmp")
                tmp.writeText(GsonUtil.getGson().toJson(record))
                if (!tmp.renameTo(file)) {
                    file.writeText(tmp.readText())
                    tmp.delete()
                }
            }.onFailure {
                Log.w(TAG, "save failed", it)
            }
        }
    }
}
