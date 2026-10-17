package com.huanchengfly.tieba.post.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

/**
 * 更新弹窗 / 关于页内联卡片共享的更新说明渲染（版本名 + changelog 正文）。
 */
@Composable
fun UpdateReleaseContent(release: GitHubRelease) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = release.name.ifEmpty { release.tagName },
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            color = ExtendedTheme.colors.text
        )
        val changelog = release.changelog()
        if (changelog.isNotEmpty() && changelog != release.name && changelog != release.tagName) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = changelog,
                style = MaterialTheme.typography.body2,
                color = ExtendedTheme.colors.textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 下载进度条（Downloading 状态共用）。
 */
@Composable
fun UpdateDownloadProgress(downloaded: Long, total: Long) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (total > 0) {
            val progress = (downloaded.toFloat() / total).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = ExtendedTheme.colors.accent,
                backgroundColor = ExtendedTheme.colors.text.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatBytes(downloaded)} / ${formatBytes(total)}",
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = ExtendedTheme.colors.accent,
                backgroundColor = ExtendedTheme.colors.text.copy(alpha = 0.1f)
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
