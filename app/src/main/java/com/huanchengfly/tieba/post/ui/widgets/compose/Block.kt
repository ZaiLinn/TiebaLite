package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.BlockManager
import com.huanchengfly.tieba.post.utils.appPreferences

@Composable
fun BlockTip(
    text: @Composable () -> Unit = { Text(text = stringResource(id = R.string.tip_blocked_content)) },
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ExtendedTheme.colors.textSecondary.copy(alpha = 0.1f))
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.caption) {
            ProvideContentColor(color = ExtendedTheme.colors.textSecondary) {
                text()
            }
        }
    }
}

@Composable
fun BlockableContent(
    blocked: Boolean,
    modifier: Modifier = Modifier,
    blockedTip: @Composable () -> Unit = { BlockTip() },
    hideBlockedContent: Boolean = LocalContext.current.appPreferences.hideBlockedContent,
    content: @Composable () -> Unit,
) {
    if (!blocked) {
        content()
    } else if (!hideBlockedContent) {
        Column(modifier = modifier) {
            blockedTip()
        }
    }
}

/** 富文本里图片、视频、语音的文字占位符（见 PbContentRender.toString） */
private val MEDIA_PLACEHOLDERS = listOf("[图片]", "[视频]", "[语音]")

/**
 * 页面打开后屏蔽规则变化的次数。条目是否被屏蔽在加载数据时已经算好，
 * 只有在当前页面新增了屏蔽规则（次数不为 0）时才需要在界面上重新判断。
 */
@Composable
fun rememberBlockRulesChangeCount(): Int {
    val initialVersion = remember { BlockManager.version.value }
    val version by BlockManager.version.collectAsState()
    return version - initialVersion
}

/** 规则没有变化时直接用加载时算好的 [precomputed]，否则按当前规则重新判断 */
@Composable
fun rememberBlocked(
    key: Any,
    precomputed: Boolean,
    blockRulesChangeCount: Int,
    check: () -> Boolean,
): Boolean =
    if (blockRulesChangeCount == 0) precomputed else remember(key, blockRulesChangeCount) { check() }

/**
 * 长按菜单里的「屏蔽该楼内容」「屏蔽该用户」。
 * 纯图片/视频内容的文本只有占位符，按它完全匹配会把同类内容都屏蔽掉，不提供屏蔽内容；
 * 自己发的内容不提供屏蔽用户。菜单只在弹出时组合，这里的计算不影响列表滑动。
 */
@Composable
fun BlockMenuItems(
    contentText: String,
    authorId: Long,
    authorName: String?,
    currentUserId: Long,
    isSubPost: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val hasText = remember(contentText) {
        MEDIA_PLACEHOLDERS.fold(contentText) { text, placeholder -> text.replace(placeholder, "") }
            .isNotBlank()
    }
    if (hasText) {
        DropdownMenuItem(
            onClick = {
                BlockManager.blockExactContent(contentText) {
                    if (it) context.toastShort(R.string.toast_blocked)
                }
                onDismiss()
            }
        ) {
            Text(
                text = stringResource(
                    id = if (isSubPost) R.string.menu_block_sub_post_content else R.string.menu_block_post_content
                )
            )
        }
    }
    if (authorId != 0L && authorId != currentUserId) {
        DropdownMenuItem(
            onClick = {
                BlockManager.blockUser(authorId, authorName) {
                    if (it) context.toastShort(R.string.toast_blocked)
                }
                onDismiss()
            }
        ) {
            Text(text = stringResource(id = R.string.menu_block_user))
        }
    }
}
