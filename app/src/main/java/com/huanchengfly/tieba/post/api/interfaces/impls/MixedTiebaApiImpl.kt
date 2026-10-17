package com.huanchengfly.tieba.post.api.interfaces.impls

import com.huanchengfly.tieba.post.api.interfaces.IFeedApi
import com.huanchengfly.tieba.post.api.interfaces.IForumApi
import com.huanchengfly.tieba.post.api.interfaces.IMessageApi
import com.huanchengfly.tieba.post.api.interfaces.IReplyApi
import com.huanchengfly.tieba.post.api.interfaces.ISearchApi
import com.huanchengfly.tieba.post.api.interfaces.IThreadApi
import com.huanchengfly.tieba.post.api.interfaces.ITiebaApi
import com.huanchengfly.tieba.post.api.interfaces.IUserApi

/** 各领域实现的组合，方法全部委托给对应的 *ApiImpl。 */
object MixedTiebaApiImpl : ITiebaApi,
    IFeedApi by FeedApiImpl,
    IForumApi by ForumApiImpl,
    IThreadApi by ThreadApiImpl,
    IReplyApi by ReplyApiImpl,
    IUserApi by UserApiImpl,
    IMessageApi by MessageApiImpl,
    ISearchApi by SearchApiImpl
