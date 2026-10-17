package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName

/** c/u/follow/atSearchUserList 的响应：可 @ 的用户（不带关键词时为关注的人） */
data class AtUserListBean(
    @SerializedName("error_code")
    val errorCode: String? = null,
    @SerializedName("error_msg")
    val errorMsg: String? = null,
    @SerializedName("user_list")
    val userList: List<AtUser>? = null,
) {
    data class AtUser(
        @SerializedName("id")
        val id: String? = null,
        @SerializedName("name")
        val name: String? = null,
        @SerializedName("name_show")
        val nameShow: String? = null,
        @SerializedName("portrait")
        val portrait: String? = null,
    ) {
        /** 插入正文的名字：官方编辑器的 @ 片段用的是昵称（AtSelectData.mNameShow） */
        val displayName: String get() = nameShow?.ifBlank { null } ?: name.orEmpty()
    }
}
