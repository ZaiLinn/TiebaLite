package com.huanchengfly.tieba.post.ui.page.reply

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.AtUserListBean.AtUser
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull

private const val SEARCH_DEBOUNCE_MS = 300L

/**
 * 选择要 @ 的用户：默认列出关注的人，输入关键词后搜索。
 * 接口为 c/u/follow/atSearchUserList（官方客户端 AtListActivity）。
 */
@Composable
fun AtUserPickerDialog(
    onPick: (AtUser) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<AtUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.isNotEmpty()) delay(SEARCH_DEBOUNCE_MS)
        loading = true
        error = null
        var failure: Throwable? = null
        val result = TiebaApi.getInstance()
            .atUserListFlow(query.trim())
            .catch { failure = it }
            .firstOrNull()
        users = result?.userList.orEmpty().filter { it.displayName.isNotEmpty() }
        error = failure?.getErrorMessage()
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = ExtendedTheme.colors.windowBackground) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.title_at_user),
                    style = MaterialTheme.typography.h6,
                    color = ExtendedTheme.colors.text,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(text = stringResource(id = R.string.hint_at_user_search)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    when {
                        loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        error != null -> Text(
                            text = error.orEmpty(),
                            color = ExtendedTheme.colors.textSecondary,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        users.isEmpty() -> Text(
                            text = stringResource(id = R.string.tip_at_user_empty),
                            color = ExtendedTheme.colors.textSecondary,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        else -> MyLazyColumn {
                            items(users, key = { it.id ?: it.displayName }) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(user) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Avatar(
                                        data = StringUtil.getAvatarUrl(user.portrait),
                                        size = 36.dp,
                                        contentDescription = null,
                                    )
                                    Column {
                                        Text(text = user.displayName, color = ExtendedTheme.colors.text)
                                        if (!user.name.isNullOrBlank() && user.name != user.displayName) {
                                            Text(
                                                text = user.name,
                                                style = MaterialTheme.typography.caption,
                                                color = ExtendedTheme.colors.textSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
