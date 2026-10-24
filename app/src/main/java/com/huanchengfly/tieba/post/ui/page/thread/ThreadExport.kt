package com.huanchengfly.tieba.post.ui.page.thread

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.repository.EmptyDataException
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 导出用到的一层楼，和 protobuf 解耦，方便测试 */
internal data class ExportPost(
    val id: Long,
    val floor: Int,
    val time: Long,
    val author: String,
    val content: String,
    /** 楼中楼：(作者, 内容) */
    val subPosts: List<Pair<String, String>>,
)

internal data class ExportThread(
    val threadId: Long,
    val title: String,
    val forumName: String,
    val seeLz: Boolean,
    val posts: List<ExportPost>,
)

/** 把一段帖子内容转成 Markdown：图片和链接保留地址，表情写成 [名字] */
internal fun List<PbContent>.toMarkdown(): String = buildString {
    this@toMarkdown.forEach {
        when (it.type) {
            0, 9, 27 -> append(it.text)
            1 -> append("[${it.text.ifBlank { it.link }}](${it.link})")
            2 -> append("[${it.c}]")
            3 -> {
                val url = it.originSrc.ifBlank { it.src }.ifBlank { it.cdnSrc }
                if (url.isNotBlank()) append("\n![](${url})\n")
            }

            4 -> append(it.text)
            5 -> {
                val url = it.link.ifBlank { it.text }
                append("[视频](${url})")
            }

            10 -> append("[语音]")
            20 -> if (it.src.isNotBlank()) append("\n![](${it.src})\n")
        }
    }
}.trim()

private fun Post.authorName(): String =
    author?.let { it.nameShow.ifBlank { it.name } }.orEmpty().ifBlank { author_id.toString() }

internal fun Post.toExportPost(): ExportPost = ExportPost(
    id = id,
    floor = floor,
    time = time.toLong(),
    author = authorName(),
    content = content.toMarkdown(),
    subPosts = sub_post_list?.sub_post_list.orEmpty().map { sub ->
        val name = sub.author?.let { it.nameShow.ifBlank { it.name } }.orEmpty()
            .ifBlank { sub.author_id.toString() }
        name to sub.content.toMarkdown()
    },
)

internal fun ExportThread.toMarkdown(exportTime: Long = System.currentTimeMillis()): String {
    val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return buildString {
        appendLine("# ${title.ifBlank { "帖子 $threadId" }}")
        appendLine()
        appendLine("- 吧：${forumName}吧")
        appendLine("- 链接：https://tieba.baidu.com/p/$threadId")
        if (seeLz) appendLine("- 只看楼主")
        appendLine("- 导出时间：${timeFormat.format(Date(exportTime))}，共 ${posts.size} 层")
        posts.forEach { post ->
            appendLine()
            appendLine("---")
            appendLine()
            val time = if (post.time > 0) " · ${timeFormat.format(Date(post.time * 1000))}" else ""
            appendLine("### ${post.floor} 楼 · ${post.author}$time")
            appendLine()
            if (post.content.isNotEmpty()) appendLine(post.content)
            if (post.subPosts.isNotEmpty()) {
                appendLine()
                appendLine(
                    post.subPosts.joinToString("\n>\n") { (author, content) ->
                        "> **$author**：${content.replace("\n", " ")}"
                    }
                )
            }
        }
    }.trimEnd() + "\n"
}

/** 一次最多导出这么多页，避免超大帖子一直请求下去 */
private const val MAX_EXPORT_PAGES = 300

/**
 * 从第 1 页起逐页拉取整个帖子（或只看楼主），按楼层去重排序。
 * 每页之间稍等一下，别给服务器太大压力。
 */
internal suspend fun fetchThreadForExport(
    threadId: Long,
    forumId: Long?,
    seeLz: Boolean,
    onProgress: (page: Int, totalPage: Int) -> Unit,
): ExportThread = withContext(Dispatchers.IO) {
    val posts = linkedMapOf<Long, ExportPost>()
    var title = ""
    var forumName = ""
    var page = 1
    var totalPage = 1
    while (page <= totalPage && page <= MAX_EXPORT_PAGES) {
        withContext(Dispatchers.Main) { onProgress(page, totalPage) }
        val response = try {
            PbPageRepository.pbPage(threadId, page = page, forumId = forumId, seeLz = seeLz).first()
        } catch (e: EmptyDataException) {
            break
        }
        val data = response.data_ ?: break
        title = data.thread?.title.orEmpty().ifBlank { title }
        forumName = data.forum?.name.orEmpty().ifBlank { forumName }
        totalPage = data.page?.new_total_page?.takeIf { it > 0 } ?: totalPage
        data.first_floor_post?.let { posts.putIfAbsent(it.id, it.toExportPost()) }
        data.post_list.forEach { posts.putIfAbsent(it.id, it.toExportPost()) }
        if (data.page?.has_more == 0) break
        page++
        delay(300)
    }
    ExportThread(threadId, title, forumName, seeLz, posts.values.sortedBy { it.floor })
}

internal suspend fun writeText(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("无法写入所选文件")
    output.bufferedWriter().use { it.write(text) }
}

@Composable
internal fun ExportThreadDialog(
    dialogState: DialogState,
    onExport: (seeLz: Boolean) -> Unit,
) {
    Dialog(
        dialogState = dialogState,
        title = { Text(text = stringResource(id = R.string.title_export_thread)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_export_all),
                onClick = { onExport(false) }
            )
            DialogPositiveButton(
                text = stringResource(id = R.string.button_export_see_lz),
                onClick = { onExport(true) }
            )
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(text = stringResource(id = R.string.tip_export_thread))
        }
    }
}

@Composable
internal fun ExportProgressDialog(
    dialogState: DialogState,
    page: Int,
    totalPage: Int,
    onCancel: () -> Unit,
) {
    Dialog(
        dialogState = dialogState,
        cancelable = false,
        cancelableOnTouchOutside = false,
        title = { Text(text = stringResource(id = R.string.title_export_thread)) },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel), onClick = onCancel)
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(text = stringResource(id = R.string.tip_export_progress, page, totalPage))
        }
    }
}
