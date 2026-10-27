package com.huanchengfly.tieba.post.api.interfaces.impls

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.ClientVersion
import com.huanchengfly.tieba.post.api.buildAppPosInfo
import com.huanchengfly.tieba.post.api.buildCommonRequest
import com.huanchengfly.tieba.post.api.buildProtobufRequestBody
import com.huanchengfly.tieba.post.api.getScreenHeight
import com.huanchengfly.tieba.post.api.getScreenWidth
import com.huanchengfly.tieba.post.api.interfaces.IThreadApi
import com.huanchengfly.tieba.post.api.models.AgreeBean
import com.huanchengfly.tieba.post.api.models.CheckReportBean
import com.huanchengfly.tieba.post.api.models.CollectDataBean
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.NewCollectDataBean
import com.huanchengfly.tieba.post.api.models.PicPageBean
import com.huanchengfly.tieba.post.api.models.SubFloorListBean
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorRequest
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorRequestData
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorResponse
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageRequest
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageRequestData
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.models.protos.threadList.AdParam
import com.huanchengfly.tieba.post.api.retrofit.ApiResult
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import retrofit2.Call

object ThreadApiImpl : IThreadApi {
    override fun opAgree(
        threadId: String,
        postId: String,
        opType: Int
    ): Call<AgreeBean> =
        RetrofitTiebaApi.MINI_TIEBA_API.agree(postId, threadId, op_type = opType)

    override fun disagree(
        threadId: String,
        postId: String,
        opType: Int
    ): Call<AgreeBean> = RetrofitTiebaApi.MINI_TIEBA_API.disagree(postId, threadId, op_type = opType)

    override fun opAgreeFlow(
        threadId: String,
        postId: String,
        opType: Int,
        objType: Int,
        agreeType: Int,
    ): Flow<AgreeBean> =
        RetrofitTiebaApi.MINI_TIEBA_API.opAgreeFlow(
            threadId,
            postId,
            opType = opType,
            objType = objType,
            agreeType = agreeType
        )

    override fun disagreeFlow(
        threadId: String,
        postId: String,
        opType: Int
    ): Flow<AgreeBean> = RetrofitTiebaApi.MINI_TIEBA_API.disagreeFlow(postId, threadId, op_type = opType)

    override fun floor(
        threadId: String, page: Int, postId: String?, subPostId: String?
    ): Call<SubFloorListBean> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.floor(threadId, page, postId, subPostId)

    override fun picPage(
        forumId: String,
        forumName: String,
        threadId: String,
        seeLz: Boolean,
        picId: String,
        picIndex: String,
        objType: String,
        prev: Boolean
    ): Call<PicPageBean> = RetrofitTiebaApi.MINI_TIEBA_API.picPage(
        forumId,
        forumName,
        threadId,
        picId,
        picIndex,
        objType,
        prev = if (prev) 10 else 0,
        next = if (prev) 0 else 10,
        not_see_lz = if (seeLz) 0 else 1
    )

    override fun picPageFlow(
        forumId: String,
        forumName: String,
        threadId: String,
        seeLz: Boolean,
        picId: String,
        picIndex: String,
        objType: String,
        prev: Boolean
    ): Flow<PicPageBean> = RetrofitTiebaApi.MINI_TIEBA_API.picPageFlow(
        forumId,
        forumName,
        threadId,
        picId,
        picIndex,
        objType,
        prev = if (prev) 10 else 0,
        next = if (prev) 0 else 10,
        not_see_lz = if (seeLz) 0 else 1
    )

    override fun delThread(
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String
    ): Call<CommonResponse> =
        RetrofitTiebaApi.MINI_TIEBA_API.delThread(forumId, forumName, threadId, tbs)

    override fun delThreadFlow(
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String?,
        delMyThread: Boolean,
        isHide: Boolean,
    ): Flow<CommonResponse> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API
            .delThreadFlow(
                forumId,
                forumName,
                threadId,
                tbs,
                deleteMyThread = if (delMyThread) 1 else 0,
                isFrsMask = if (isHide) 1 else 0
            )

    override fun delPost(
        forumId: String,
        forumName: String,
        threadId: String,
        postId: String,
        tbs: String,
        isFloor: Boolean,
        delMyPost: Boolean
    ): Call<CommonResponse> =
        RetrofitTiebaApi.MINI_TIEBA_API.delPost(
            forumId,
            forumName,
            threadId,
            postId,
            tbs,
            is_floor = if (isFloor) 1 else 0,
            src = if (isFloor) 3 else 1,
            is_vip_del = if (delMyPost) 0 else 1,
            delete_my_post = if (delMyPost) 1 else 0
        )

    override fun delPostFlow(
        forumId: Long,
        forumName: String,
        threadId: Long,
        postId: Long,
        tbs: String?,
        isFloor: Boolean,
        delMyPost: Boolean
    ): Flow<CommonResponse> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API
            .delPostFlow(
                forumId,
                forumName,
                threadId,
                postId,
                isFloor = if (isFloor) 1 else 0,
                src = if (isFloor) 3 else 1,
                isVipDel = if (delMyPost) 0 else 1,
                deleteMyPost = if (delMyPost) 1 else 0,
                tbs = tbs
            )

    override fun threadStore(page: Int, pageSize: Int): Call<ThreadStoreBean> =
        RetrofitTiebaApi.NEW_TIEBA_API.threadStore(
            pageSize,
            pageSize * page,
            AccountUtil.getUid()
        )

    override fun threadStoreFlow(page: Int, pageSize: Int): Flow<ThreadStoreBean> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.threadStoreFlow(
            pageSize,
            pageSize * page
        )

    override fun removeStore(threadId: String, tbs: String): Call<CommonResponse> =
        RetrofitTiebaApi.NEW_TIEBA_API.removeStore(threadId, tbs)

    override fun removeStoreFlow(
        threadId: Long,
        forumId: Long,
        tbs: String?
    ): Flow<CommonResponse> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.removeStoreFlow(
            threadId.toString(),
            forumId.toString(),
            tbs ?: AccountUtil.getLoginInfo()!!.tbs
        )

    override fun removeStoreFlow(threadId: String): Flow<CommonResponse> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.removeStoreFlow(threadId)

    override fun addStore(threadId: String, postId: String, tbs: String): Call<CommonResponse> =
        RetrofitTiebaApi.NEW_TIEBA_API.addStore(
            listOf(
                CollectDataBean(
                    threadId,
                    postId,
                    "0",
                    "0"
                )
            ).toJson(),
            tbs
        )

    override fun addStoreAsync(threadId: Long, postId: Long): Deferred<ApiResult<CommonResponse>> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.addStoreAsync(
            listOf(
                NewCollectDataBean(
                    threadId.toString(),
                    postId.toString(),
                    status = 1
                )
            ).toJson()
        )

    override fun addStoreFlow(threadId: Long, postId: Long): Flow<CommonResponse> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.addStoreFlow(
            listOf(
                NewCollectDataBean(
                    threadId.toString(),
                    postId.toString(),
                    status = 1
                )
            ).toJson()
        )

    override fun threadContent(
        threadId: String, page: Int, seeLz: Boolean, reverse: Boolean
    ): Call<ThreadContentBean> = RetrofitTiebaApi.OFFICIAL_TIEBA_API.threadContent(
        threadId,
        page,
        last = if (reverse) "1" else null,
        r = if (reverse) "1" else null,
        lz = if (seeLz) 1 else 0
    )

    override fun threadContent(
        threadId: String, postId: String?, seeLz: Boolean, reverse: Boolean
    ): Call<ThreadContentBean> = RetrofitTiebaApi.OFFICIAL_TIEBA_API.threadContent(
        threadId,
        postId,
        last = if (reverse) "1" else null,
        r = if (reverse) "1" else null,
        lz = if (seeLz) 1 else 0
    )

    override fun threadContentAsync(
        threadId: String,
        page: Int,
        seeLz: Boolean,
        reverse: Boolean
    ): Deferred<ApiResult<ThreadContentBean>> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.threadContentAsync(
            threadId,
            page,
            last = if (reverse) "1" else null,
            r = if (reverse) "1" else null,
            lz = if (seeLz) 1 else 0
        )

    override fun threadContentAsync(
        threadId: String,
        postId: String?,
        seeLz: Boolean,
        reverse: Boolean
    ): Deferred<ApiResult<ThreadContentBean>> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.threadContentAsync(
            threadId,
            postId,
            last = if (reverse) "1" else null,
            r = if (reverse) "1" else null,
            lz = if (seeLz) 1 else 0
        )

    override fun checkReportPost(postId: String): Call<CheckReportBean> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.checkReport(
            category = "1",
            reportParam = mapOf(
                "pid" to postId
            )
        )

    override fun checkReportPostAsync(postId: String): Deferred<ApiResult<CheckReportBean>> =
        RetrofitTiebaApi.OFFICIAL_TIEBA_API.checkReportAsync(
            category = "1",
            reportParam = mapOf(
                "pid" to postId
            )
        )

    override fun pbPageFlow(
        threadId: Long,
        page: Int,
        postId: Long,
        seeLz: Boolean,
        back: Boolean,
        sortType: Int,
        forumId: Long?,
        stType: String,
        mark: Int,
        lastPostId: Long?,
        jumpFloor: Int,
    ): Flow<PbPageResponse> {
        return RetrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.pbPageFlow(
            buildProtobufRequestBody(
                PbPageRequest(
                    PbPageRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        kz = threadId,
                        pid = postId,
                        pn = page,
                        r = sortType,
                        lz = if (seeLz) 1 else 0,
                        forum_id = forumId ?: 0,
                        ad_param = com.huanchengfly.tieba.post.api.models.protos.pbPage.AdParam(
                            load_count = 0,
                            refresh_count = 1,
                            is_req_ad = 1
                        ),
                        mark = mark,
                        last_pid = lastPostId ?: 0,
                        app_pos = buildAppPosInfo(),
                        back = if (back) 1 else 0,
                        banner = 0,
                        broadcast_id = 0,
                        floor_rn = 4,
                        // 1 = 按热度，0 = 按时间
                        floor_sort_type = if (App.INSTANCE.appPreferences.subPostSortByTime) 0 else 1,
                        from_push = 0,
                        from_smart_frs = 0,
                        immersion_video_comment_source = 0,
                        is_comm_reverse = 0,
                        is_fold_comment_req = 0,
                        is_jumpfloor = if (jumpFloor > 0) 1 else 0,
                        jumpfloor_num = jumpFloor,
                        need_repost_recommend_forum = 0,
                        obj_locate = "",
                        obj_param1 = "10",
                        obj_source = "",
                        ori_ugc_type = 0,
                        pb_rn = 0,
                        q_type = 2,
                        request_times = 0,
                        rn = 15,
                        s_model = 0,
                        scr_dip = App.ScreenInfo.DENSITY.toDouble(),
                        scr_h = getScreenHeight(),
                        scr_w = getScreenWidth(),
                        similar_from = 0,
                        source_type = 2,
                        st_type = stType,
                        thread_type = 0,
                        weipost = 0,
                        with_floor = 1
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12
            )
        )
    }

    override fun pbFloorFlow(
        threadId: Long,
        postId: Long,
        forumId: Long,
        page: Int,
        subPostId: Long
    ): Flow<PbFloorResponse> {
        return RetrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.pbFloorFlow(
            buildProtobufRequestBody(
                PbFloorRequest(
                    PbFloorRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V22),
                        forum_id = forumId,
                        kz = threadId,
                        pid = postId,
                        pn = page,
                        spid = subPostId,
                        scr_dip = App.ScreenInfo.DENSITY.toDouble(),
                        scr_h = getScreenHeight(),
                        scr_w = getScreenWidth(),
                        is_comm_reverse = 0,
                        ori_ugc_type = 0
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V22,
                needSToken = false
            )
        )
    }
}
