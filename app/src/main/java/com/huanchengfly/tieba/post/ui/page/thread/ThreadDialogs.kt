package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.AppScope
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private val PageInputRegex = "^[0-9]*$".toRegex()

/**
 * 解析"跳转到页"输入。空串、非数字、超出 Int 范围或不在 1..[totalPage] 内时返回 null。
 * [totalPage] 未知（<= 0）时只要求 >= 1。
 */
internal fun parseJumpPage(input: String, totalPage: Int): Int? {
    val page = input.trim().toIntOrNull() ?: return null
    if (page < 1) return null
    if (totalPage > 0 && page > totalPage) return null
    return page
}

/**
 * 删除确认后要发送的 Intent：[deletePost] 为 null 表示删除整个帖子，否则删除该楼层。
 * 作者是当前用户时走"删除自己的帖子/回复"接口。
 */
internal fun buildDeleteIntent(
    deletePost: Post?,
    forumId: Long,
    forumName: String,
    threadId: Long,
    threadAuthorId: Long?,
    userId: Long,
    tbs: String?,
): ThreadUiIntent =
    if (deletePost == null) {
        ThreadUiIntent.DeleteThread(
            forumId = forumId,
            forumName = forumName,
            threadId = threadId,
            deleteMyThread = threadAuthorId == userId,
            tbs = tbs
        )
    } else {
        ThreadUiIntent.DeletePost(
            forumId = forumId,
            forumName = forumName,
            threadId = threadId,
            postId = deletePost.id,
            deleteMyPost = deletePost.author_id == userId,
            tbs = tbs
        )
    }

/**
 * 退出已收藏的帖子时询问是否把收藏进度更新到 [readFloor] 楼。
 *
 * 请求在 [AppScope] 里发：确认后页面立刻出栈，页面自己的作用域会随之取消，
 * 在那里发请求可能还没完成就被取消。
 */
@Composable
internal fun UpdateCollectMarkDialog(
    dialogState: DialogState,
    readFloor: Int,
    threadId: Long,
    markPostId: Long,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    ConfirmDialog(
        dialogState = dialogState,
        onConfirm = {
            onDismiss()
            if (markPostId != 0L) {
                AppScope.launch(Dispatchers.Main) {
                    TiebaApi.getInstance()
                        .addStoreFlow(threadId, markPostId)
                        .catch {
                            context.toastShort(
                                R.string.message_update_collect_mark_failed,
                                it.getErrorMessage()
                            )
                        }
                        .collect {
                            context.toastShort(R.string.message_update_collect_mark_success)
                        }
                }
            }
        },
        onCancel = onDismiss
    ) {
        Text(text = stringResource(R.string.message_update_collect_mark, readFloor))
    }
}

/** 删除帖子（[deletePost] 为 null）或某一楼层的确认框。 */
@Composable
internal fun DeleteConfirmDialog(
    dialogState: DialogState,
    deletePost: ImmutableHolder<Post>?,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        dialogState = dialogState,
        onConfirm = onConfirm
    ) {
        Text(
            text = stringResource(
                id = R.string.message_confirm_delete,
                if (deletePost == null) stringResource(id = R.string.this_thread)
                else stringResource(id = R.string.tip_post_floor, deletePost.get { floor })
            )
        )
    }
}

/** 跳转到指定页。输入无效（空、越界）时不跳转。 */
@Composable
internal fun JumpToPageDialog(
    dialogState: DialogState,
    currentPageMax: Int,
    totalPage: Int,
    onJump: (page: Int) -> Unit,
) {
    PromptDialog(
        onConfirm = { input -> parseJumpPage(input, totalPage)?.let(onJump) },
        dialogState = dialogState,
        onValueChange = { newVal, _ -> PageInputRegex.matches(newVal) },
        title = { Text(text = stringResource(id = R.string.title_jump_page)) },
        content = {
            Text(
                text = stringResource(
                    id = R.string.tip_jump_page,
                    currentPageMax,
                    totalPage
                )
            )
        }
    )
}
