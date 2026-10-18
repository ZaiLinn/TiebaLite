package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponse
import com.huanchengfly.tieba.post.api.models.protos.threadList.ThreadListResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.utils.ThreadTypeFilter
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

object FrsPageRepository {
    // 最近一次成功的响应及其请求参数，两者只在成功时一起更新：
    // 参数相同才复用或在失败时兜底，避免请求失败时显示别的吧的内容
    var lastHash: String = ""
    var lastResponse: FrsPageResponse? = null

    fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int? = null,
        forceNew: Boolean = false,
    ): Flow<FrsPageResponse> {
        val hash = "${forumName}_${page}_${loadType}_${sortType}_${goodClassifyId}"
        if (!forceNew && lastResponse != null && lastHash == hash) {
            return flowOf(lastResponse!!)
        }
        return TiebaApi.getInstance().frsPage(forumName, page, loadType, sortType, goodClassifyId)
            .map { response ->
                if (response.data_ == null) throw TiebaUnknownException
                val userList = response.data_.user_list
                val filterOptions = App.INSTANCE.appPreferences.threadTypeFilterOptions
                val threadList = response.data_.thread_list
                    .map { threadInfo ->
                        threadInfo.copy(author = userList.find { it.id == threadInfo.authorId })
                    }
                    .filterNot { ThreadTypeFilter.shouldHide(it, filterOptions) }
                response.copy(data_ = response.data_.copy(thread_list = threadList))
            }
            .onEach { response ->
                // 原来这里还会把整份响应 toString() 写进 AppCache：字符串超过 100KB 就被丢弃，
                // 也从来没有地方读取，却挡在内容显示之前，已删除
                lastHash = hash
                lastResponse = response
            }
            .catch {
                val cached = lastResponse
                if (cached != null && lastHash == hash) emit(cached) else throw it
            }
    }

    fun threadList(
        forumId: Long,
        forumName: String,
        page: Int,
        sortType: Int,
        threadIds: String = "",
    ): Flow<ThreadListResponse> =
        TiebaApi.getInstance()
            .threadList(forumId, forumName, page, sortType, threadIds)
            .map { response ->
                if (response.data_ == null) throw TiebaUnknownException
                val userList = response.data_.user_list
                val filterOptions = App.INSTANCE.appPreferences.threadTypeFilterOptions
                val threadList = response.data_.thread_list
                    .map { threadInfo ->
                        threadInfo.copy(author = userList.find { it.id == threadInfo.authorId })
                    }
                    .filterNot { ThreadTypeFilter.shouldHide(it, filterOptions) }
                response.copy(data_ = response.data_.copy(thread_list = threadList))
            }
}