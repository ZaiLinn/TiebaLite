package com.huanchengfly.tieba.post.models.database

import com.huanchengfly.tieba.post.fromJson
import org.litepal.crud.LitePalSupport

data class Block @JvmOverloads constructor(
    val category: Int = 0,
    val type: Int = 0,
    val keywords: String? = null,
    val username: String? = null,
    val uid: String? = null,
) : LitePalSupport() {
    val id: Long = 0L
    companion object {
        const val CATEGORY_BLACK_LIST = 10
        const val CATEGORY_WHITE_LIST = 11

        const val TYPE_KEYWORD = 0
        const val TYPE_USER = 1

        /** 内容与关键词完全相同（去掉首尾空白后比较）才屏蔽，用于长按楼层「屏蔽该楼内容」 */
        const val TYPE_EXACT_CONTENT = 2

        fun Block.getKeywords(): List<String> {
            return keywords?.fromJson() ?: emptyList()
        }
    }
}