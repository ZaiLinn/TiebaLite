package com.huanchengfly.tieba.post.api.interfaces.impls

import com.huanchengfly.tieba.post.api.interfaces.IMessageApi
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.MsgBean
import com.huanchengfly.tieba.post.api.retrofit.ApiResult
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import retrofit2.Call

object MessageApiImpl : IMessageApi {
    override fun msg(): Call<MsgBean> = RetrofitTiebaApi.NEW_TIEBA_API.msg()

    override fun msgFlow(): Flow<MsgBean> = RetrofitTiebaApi.NEW_TIEBA_API.msgFlow()

    override fun replyMe(page: Int): Call<MessageListBean> =
        RetrofitTiebaApi.NEW_TIEBA_API.replyMe(page)

    override fun replyMeAsync(page: Int): Deferred<ApiResult<MessageListBean>> =
        RetrofitTiebaApi.NEW_TIEBA_API.replyMeAsync(page)

    override fun replyMeFlow(page: Int): Flow<MessageListBean> =
        RetrofitTiebaApi.NEW_TIEBA_API.replyMeFlow(page)

    override fun atMe(page: Int): Call<MessageListBean> = RetrofitTiebaApi.NEW_TIEBA_API.atMe(page)

    override fun atMeAsync(page: Int): Deferred<ApiResult<MessageListBean>> =
        RetrofitTiebaApi.NEW_TIEBA_API.atMeAsync(page)

    override fun atMeFlow(page: Int): Flow<MessageListBean> = RetrofitTiebaApi.NEW_TIEBA_API.atMeFlow(page)

    override fun agreeMe(page: Int): Call<MessageListBean> =
        RetrofitTiebaApi.NEW_TIEBA_API.agreeMe(page)

    override fun agreeMeFlow(page: Int): Flow<MessageListBean> =
        RetrofitTiebaApi.NEW_TIEBA_API.agreeMeFlow(page)
}
