package com.huanchengfly.tieba.post.api.interfaces

import com.huanchengfly.tieba.post.api.models.*
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.ForumRecommendResponse
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponse
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponse
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListResponse
import com.huanchengfly.tieba.post.api.models.web.HotMessageListBean
import com.huanchengfly.tieba.post.api.retrofit.ApiResult
import com.huanchengfly.tieba.post.models.DislikeBean
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import retrofit2.Call

interface IFeedApi {
    /**
     * 个性推荐（每页 15 贴）
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    fun personalized(
        loadType: Int,
        page: Int = 1
    ): Call<PersonalizedBean>

    /**
     * 个性推荐（每页 15 贴）
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    fun personalizedAsync(
        loadType: Int,
        page: Int = 1
    ): Deferred<ApiResult<PersonalizedBean>>

    /**
     * 个性推荐（每页 15 贴）
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    fun personalizedFlow(
        loadType: Int,
        page: Int = 1
    ): Flow<PersonalizedBean>

    /**
     * 个性推荐（每页 15 贴）
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    fun personalizedProtoFlow(
        loadType: Int,
        page: Int = 1
    ): Flow<PersonalizedResponse>

    /**
     * 关注吧列表
     *
     * **需登录**
     */
    fun forumRecommend(): Call<ForumRecommend>

    /**
     * 关注吧列表
     *
     * **需登录**
     */
    fun forumRecommendAsync(): Deferred<ApiResult<ForumRecommend>>

    /**
     * 关注吧列表
     *
     * **需登录**
     */
    fun forumRecommendFlow(): Flow<ForumRecommend>

    /**
     * 推荐“不感兴趣”
     *
     * **需登录**
     *
     * @param dislikeBean “不感兴趣”信息 [com.huanchengfly.tieba.post.models.DislikeBean]
     * @param stoken stoken
     */
    fun submitDislike(
        dislikeBean: DislikeBean,
        stoken: String
    ): Call<CommonResponse>

    /**
     * 推荐“不感兴趣”
     *
     * **需登录**
     *
     * @param dislikeBean “不感兴趣”信息 [com.huanchengfly.tieba.post.models.DislikeBean]
     * @param stoken stoken
     */
    fun submitDislikeFlow(
        dislikeBean: DislikeBean
    ): Flow<CommonResponse>

    fun hotMessageList(): Call<HotMessageListBean>

    /**
     * 首页热榜
     *
     * @param tabCode Tab
     */
    fun hotThreadListFlow(
        tabCode: String
    ): Flow<HotThreadListResponse>

    /**
     * 话题榜
     */
    fun topicListFlow(): Flow<TopicListResponse>

    /**
     * 关注吧列表
     *
     * **需登录**
     *
     * @param sortType 排序（0=更新排序 1=等级排序）
     */
    fun forumRecommendNewFlow(
        sortType: Int = 1
    ): Flow<ForumRecommendResponse>
}
