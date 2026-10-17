package com.huanchengfly.tieba.post.api.interfaces

import com.huanchengfly.tieba.post.api.models.*
import com.huanchengfly.tieba.post.api.models.AtUserListBean
import com.huanchengfly.tieba.post.api.models.protos.getUserInfo.GetUserInfoResponse
import com.huanchengfly.tieba.post.api.models.protos.profile.ProfileResponse
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeResponse
import com.huanchengfly.tieba.post.api.models.protos.userPost.UserPostResponse
import com.huanchengfly.tieba.post.api.retrofit.ApiResult
import com.huanchengfly.tieba.post.models.MyInfoBean
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import retrofit2.Call
import java.io.File

interface IUserApi {
    /**
     * 查看用户关注的吧列表
     *
     * @param uid 用户 ID
     * @param page 分页页码（从 1 开始）
     */
    fun userLikeForum(
        uid: String,
        page: Int = 1
    ): Call<UserLikeForumBean>

    /**
     * 查看用户的所有主题贴/回复
     *
     * @param uid 用户 ID
     * @param page 分页页码（从 1 开始）
     * @param isThread 是否查看主题贴
     */
    fun userPost(
        uid: String,
        page: Int = 1,
        isThread: Boolean = true
    ): Call<UserPostBean>

    /**
     * 用户信息
     *
     * @param uid 用户 ID
     */
    fun profile(
        uid: String
    ): Call<ProfileBean>

    /**
     * 用户信息（Flow）
     *
     * @param uid 用户 ID
     */
    fun profileFlow(
        uid: String
    ): Flow<Profile>

    /**
     * 用户信息（异步）
     */
    fun myProfileAsync(): Deferred<ApiResult<com.huanchengfly.tieba.post.api.models.web.Profile>>

    /**
     * 关注用户（web 接口）
     *
     * **需登录**
     *
     * @param portrait 头像
     * @param tbs tbs
     */
    fun follow(
        portrait: String,
        tbs: String
    ): Call<CommonResponse>

    /**
     * 取关用户（web 接口）
     *
     * **需登录**
     *
     * @param portrait 头像
     * @param tbs tbs
     */
    fun unfollow(
        portrait: String,
        tbs: String
    ): Call<CommonResponse>

    /**
     * 关注用户（客户端接口）
     *
     * **需登录**
     *
     * @param portrait 头像
     * @param tbs tbs
     */
    fun followFlow(
        portrait: String,
        tbs: String
    ): Flow<FollowBean>

    /**
     * 取关用户（客户端接口）
     *
     * **需登录**
     *
     * @param portrait 头像
     * @param tbs tbs
     */
    fun unfollowFlow(
        portrait: String,
        tbs: String
    ): Flow<CommonResponse>

    /**
     * 登录用户信息
     *
     * @param cookie 登录 Cookie 信息
     */
    fun myInfo(
        cookie: String
    ): Call<MyInfoBean>

    /**
     * 登录用户信息
     *
     * @param cookie 登录 Cookie 信息
     */
    fun myInfoAsync(
        cookie: String
    ): Deferred<ApiResult<MyInfoBean>>

    /**
     * 登录用户信息
     *
     * @param cookie 登录 Cookie 信息
     */
    fun myInfoFlow(
        cookie: String
    ): Flow<MyInfoBean>

    /**
     * 获得当前用户昵称（需登录）
     */
    fun initNickNameFlow(): Flow<InitNickNameBean>

    /**
     * 获得用户昵称
     */
    fun initNickNameFlow(
        bduss: String,
        sToken: String,
    ): Flow<InitNickNameBean>

    /**
     * 更新登录信息（需登录）
     */
    fun loginFlow(): Flow<LoginBean>

    /**
     * 登录
     */
    fun loginFlow(
        bduss: String,
        sToken: String
    ): Flow<LoginBean>

    /**
     * 修改个人资料
     *
     * @param birthdayShowStatus 是否仅显示星座
     * @param birthdayTime 生日时间戳 / 1000
     * @param intro 个人简介（最多 500 字）
     * @param sex 性别（1 = 男，2 = 女）
     * @param nickName 昵称
     */
    fun profileModifyFlow(
        birthdayShowStatus: Boolean,
        birthdayTime: String,
        intro: String,
        sex: String,
        nickName: String,
    ): Flow<CommonResponse>

    /**
     * 上传头像
     *
     * @param file 图片 File 对象
     *
     */
    fun imgPortrait(
        file: File
    ): Flow<CommonResponse>

    /**
     * 关注动态
     *
     * @param pageTag 页码
     * @param lastRequestUnix 上次请求的时间戳（10位）
     * @param loadType 加载类型（1 = 下拉刷新，2 = 加载更多）
     */
    fun userLikeFlow(
        pageTag: String,
        lastRequestUnix: Long,
        loadType: Int
    ): Flow<UserLikeResponse>

    fun syncFlow(clientId: String? = null): Flow<Sync>

    /**
     * 用户信息（Flow）
     *
     * @param uid 用户 ID
     */
    fun userProfileFlow(
        uid: Long
    ): Flow<ProfileResponse>

    /**
     * 查看用户的所有主题贴/回复
     *
     * @param uid 用户 ID
     * @param page 分页页码（从 1 开始）
     * @param isThread 是否查看主题贴
     */
    fun userPostFlow(
        uid: Long,
        page: Int = 1,
        isThread: Boolean = true,
    ): Flow<UserPostResponse>

    fun userLikeForumFlow(
        uid: String,
        page: Int = 1,
    ): Flow<UserLikeForumBean>

    /**
     * 获得当前用户信息（需登录）
     */
    fun getUserInfoFlow(): Flow<GetUserInfoResponse>

    /**
     * 获得用户信息
     */
    fun getUserInfoFlow(
        uid: Long,
        bduss: String?,
        sToken: String?,
    ): Flow<GetUserInfoResponse>

    /** @ 好友候选；[word] 为空时返回关注的人 */
    fun atUserListFlow(word: String? = null): Flow<AtUserListBean>
}
