package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ProtoFailureResponseInterceptor
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

object AddPostRepository {
    fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String? = null,
        nameShow: String? = null,
        postId: Long? = null,
        subPostId: Long? = null,
        replyUserId: Long? = null,
        vcode: String? = null,
        vcodeMd5: String? = null,
        vcodeType: String? = null,
    ): Flow<AddPostResponse> =
        TiebaApi.getInstance()
            .addPostFlow(
                content,
                forumId.toString(),
                forumName,
                threadId.toString(),
                tbs,
                nameShow,
                postId?.toString(),
                subPostId?.toString(),
                replyUserId?.toString(),
                vcode,
                vcodeMd5,
                vcodeType,
            )
            .onEach {
                val errorCode = it.error?.error_code ?: 0
                if (errorCode in ProtoFailureResponseInterceptor.VCODE_ERROR_CODES) {
                    val message = it.error?.error_msg.orEmpty().ifEmpty { "需要验证码" }
                    val challenge = VcodeChallenge.from(it.data_?.info)
                        ?: throw TiebaApiException(CommonResponse(errorCode, message))
                    throw NeedVcodeException(challenge, message)
                }
                val newPostId = checkNotNull(it.data_?.pid?.toLongOrNull())
                if (postId != null) {
                    emitGlobalEventSuspend(
                        GlobalEvent.ReplySuccess(
                            threadId,
                            postId,
                            postId,
                            subPostId,
                            newPostId
                        )
                    )
                } else {
                    emitGlobalEventSuspend(GlobalEvent.ReplySuccess(threadId, newPostId))
                }
            }
}