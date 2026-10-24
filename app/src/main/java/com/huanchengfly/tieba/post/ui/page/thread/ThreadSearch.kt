package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog

private val FloorInputRegex = "^[0-9]*$".toRegex()

/** 解析"跳到楼层"输入，楼层从 1 开始 */
internal fun parseJumpFloor(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it >= 1 }

/** 一层楼可供搜索的文字：正文和已加载的楼中楼 */
@Immutable
internal data class SearchablePost(
    val postId: Long,
    val floor: Int,
    val text: String,
)

@Immutable
internal data class ThreadSearchHit(
    val postId: Long,
    val floor: Int,
    /** 命中位置附近的一段文字 */
    val snippet: String,
    /** 命中关键词在 [snippet] 中的位置 */
    val matchStart: Int,
    val matchLength: Int,
)

private const val SNIPPET_BEFORE = 12
private const val SNIPPET_LENGTH = 60

/** 在楼层中查找关键词（忽略大小写），每层楼只取第一处命中，按楼层排序 */
internal fun searchPosts(posts: List<SearchablePost>, keyword: String): List<ThreadSearchHit> {
    val query = keyword.trim()
    if (query.isEmpty()) return emptyList()
    return posts.mapNotNull { post ->
        val text = post.text.replace('\n', ' ')
        val index = text.indexOf(query, ignoreCase = true)
        if (index < 0) return@mapNotNull null
        val start = (index - SNIPPET_BEFORE).coerceAtLeast(0)
        val end = (start + SNIPPET_LENGTH).coerceAtMost(text.length).coerceAtLeast(index + query.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        ThreadSearchHit(
            postId = post.postId,
            floor = post.floor,
            snippet = prefix + text.substring(start, end) + suffix,
            matchStart = prefix.length + index - start,
            matchLength = query.length,
        )
    }.distinctBy { it.postId }.sortedBy { it.floor }
}

internal fun PostItemData.toSearchable(): SearchablePost {
    val post = post.get()
    val text = buildString {
        append(contentRenders.joinToString("\n") { it.toString() })
        subPosts.forEach {
            append('\n')
            append(it.subPostContent.text)
        }
    }
    return SearchablePost(post.id, post.floor, text)
}

@Composable
internal fun ThreadSearchDialog(
    dialogState: DialogState,
    loadedFloorCount: Int,
    onSearch: (keyword: String) -> Unit,
) {
    PromptDialog(
        onConfirm = { onSearch(it) },
        dialogState = dialogState,
        title = { Text(text = stringResource(id = R.string.title_thread_search)) },
        content = {
            Text(text = stringResource(id = R.string.tip_thread_search, loadedFloorCount))
        }
    )
}

@Composable
internal fun ThreadSearchResultDialog(
    dialogState: DialogState,
    keyword: String,
    hits: List<ThreadSearchHit>,
    onHitClick: (ThreadSearchHit) -> Unit,
) {
    Dialog(
        dialogState = dialogState,
        title = {
            Text(text = stringResource(id = R.string.title_thread_search_result, keyword, hits.size))
        },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_close))
        },
    ) {
        if (hits.isEmpty()) {
            Text(
                text = stringResource(id = R.string.tip_thread_search_no_result),
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(hits, key = { it.postId }) { hit ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dismiss()
                                onHitClick(hit)
                            }
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.tip_post_floor, hit.floor),
                            style = MaterialTheme.typography.caption,
                            color = ExtendedTheme.colors.textSecondary,
                        )
                        Text(
                            text = buildAnnotatedString {
                                append(hit.snippet.substring(0, hit.matchStart))
                                withStyle(
                                    SpanStyle(
                                        color = ExtendedTheme.colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(hit.snippet.substring(hit.matchStart, hit.matchStart + hit.matchLength))
                                }
                                append(hit.snippet.substring(hit.matchStart + hit.matchLength))
                            },
                            style = MaterialTheme.typography.body2,
                            color = ExtendedTheme.colors.text,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun JumpToFloorDialog(
    dialogState: DialogState,
    onJump: (floor: Int) -> Unit,
) {
    PromptDialog(
        onConfirm = { input -> parseJumpFloor(input)?.let(onJump) },
        dialogState = dialogState,
        onValueChange = { newVal, _ -> FloorInputRegex.matches(newVal) && newVal.length <= 9 },
        title = { Text(text = stringResource(id = R.string.title_jump_floor)) },
    )
}
