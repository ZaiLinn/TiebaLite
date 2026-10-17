package com.huanchengfly.tieba.post.api.interfaces

import com.huanchengfly.tieba.post.api.models.*
import com.huanchengfly.tieba.post.api.retrofit.ApiResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import retrofit2.Call

interface IMessageApi {
    /**
     * 消息提醒数
     *
     * **需登录**
     */
    fun msg(): Call<MsgBean>

    /**
     * 消息提醒数
     *
     * **需登录**
     */
    fun msgFlow(): Flow<MsgBean>

    /**
     * 回复我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun replyMe(
        page: Int = 1
    ): Call<MessageListBean>

    /**
     * 回复我的消息列表（异步）
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun replyMeAsync(
        page: Int = 1
    ): Deferred<ApiResult<MessageListBean>>

    /**
     * 回复我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     * @return Flow
     */
    fun replyMeFlow(
        page: Int = 1
    ): Flow<MessageListBean>

    /**
     * 提到我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun atMe(
        page: Int = 1
    ): Call<MessageListBean>

    /**
     * 提到我的消息列表（异步）
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun atMeAsync(
        page: Int = 1
    ): Deferred<ApiResult<MessageListBean>>

    /**
     * 提到我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     * @return Flow
     */
    fun atMeFlow(
        page: Int = 1
    ): Flow<MessageListBean>

    /**
     * 赞我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     */
    fun agreeMe(
        page: Int = 1
    ): Call<MessageListBean>

    /**
     * 赞我的消息列表
     *
     * **需登录**
     *
     * @param page 分页页码（从 1 开始）
     * @return Flow
     */
    fun agreeMeFlow(
        page: Int = 1
    ): Flow<MessageListBean>
}
