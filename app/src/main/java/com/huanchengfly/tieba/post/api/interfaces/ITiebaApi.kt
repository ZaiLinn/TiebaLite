package com.huanchengfly.tieba.post.api.interfaces

/**
 * 贴吧 API 总入口，按领域拆分为 [IFeedApi], [IForumApi], [IThreadApi], [IReplyApi], [IUserApi], [IMessageApi], [ISearchApi]。
 * 调用方继续通过 TiebaApi.getInstance() 拿到本接口，方法签名不变。
 */
interface ITiebaApi :
    IFeedApi,
    IForumApi,
    IThreadApi,
    IReplyApi,
    IUserApi,
    IMessageApi,
    ISearchApi
