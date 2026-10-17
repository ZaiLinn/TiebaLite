package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ProtoFailureResponseInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object AddThreadRepository {
    /** 新帖子的 id 与服务器提示 */
    data class Result(val threadId: Long, val message: String)

    /**
     * 发新帖。需要验证码时抛出 [NeedVcodeException]，完成验证后带上 [vcode]、
     * [vcodeMd5]、[vcodeType] 重新调用（与回帖 [AddPostRepository] 相同）。
     */
    fun addThread(
        title: String,
        content: String,
        forumId: Long,
        forumName: String,
        tbs: String? = null,
        vcode: String? = null,
        vcodeMd5: String? = null,
        vcodeType: String? = null,
    ): Flow<Result> =
        TiebaApi.getInstance()
            .addThreadFlow(
                title = title,
                content = content,
                forumId = forumId.toString(),
                forumName = forumName,
                tbs = tbs,
                vcode = vcode,
                vcodeMd5 = vcodeMd5,
                vcodeType = vcodeType,
            )
            .map {
                val errorCode = it.error?.error_code ?: 0
                if (errorCode in ProtoFailureResponseInterceptor.VCODE_ERROR_CODES) {
                    val message = it.error?.error_msg.orEmpty().ifEmpty { "需要验证码" }
                    val challenge = VcodeChallenge.from(it.data_?.info)
                        ?: throw TiebaApiException(CommonResponse(errorCode, message))
                    throw NeedVcodeException(challenge, message)
                }
                val threadId = it.data_?.tid?.toLongOrNull()
                    ?: throw TiebaApiException(CommonResponse(-1, it.error?.error_msg.orEmpty().ifEmpty { "发帖失败" }))
                Result(threadId, it.data_.msg)
            }
}
