package com.huanchengfly.tieba.post.update

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState

/**
 * 应用内更新弹窗（仅启动检查路径挂载；About 页用同一 ViewModel 的 state 渲染内联卡片）。
 * 框架 Dialogs.kt + ExtendedTheme.colors；强更时不可取消、无"忽略"。
 *
 * 只对 [UpdateSource.STARTUP] 来源的状态弹窗：About 页手动检查产生的状态由其内联卡片渲染。
 * 状态机各阶段的对话框按当前 [UpdateUiState] 分发；状态离开对应阶段后组合自然移除。
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    viewModel: UpdateViewModel
) {
    when (val current = state) {
        is UpdateUiState.Available ->
            if (current.source == UpdateSource.STARTUP) {
                UpdateAvailableDialog(available = current, viewModel = viewModel)
            }
        is UpdateUiState.Downloading ->
            if (current.source == UpdateSource.STARTUP) {
                UpdateProgressDialog(state = current)
            }
        is UpdateUiState.DownloadFailed ->
            if (current.source == UpdateSource.STARTUP) {
                UpdateDownloadFailedDialog(failed = current, viewModel = viewModel)
            }
        is UpdateUiState.ReadyToInstall ->
            if (current.source == UpdateSource.STARTUP) {
                UpdateReadyDialog(ready = current, viewModel = viewModel)
            }
        else -> Unit
    }
}

@Composable
private fun UpdateAvailableDialog(
    available: UpdateUiState.Available,
    viewModel: UpdateViewModel
) {
    val force = available.force
    val dialogState = rememberDialogState()

    // Available 状态出现 → 弹窗
    LaunchedEffect(available.release.tagName, force) {
        dialogState.show()
    }

    AlertDialog(
        dialogState = dialogState,
        cancelable = !force,
        onDismiss = if (force) null else {
            { viewModel.ignoreCurrentVersion() }
        },
        title = {
            Text(
                text = stringResource(id = R.string.title_update_available),
                color = ExtendedTheme.colors.text
            )
        },
        content = {
            UpdateReleaseContent(release = available.release)
            if (force) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.text_update_force),
                    color = ExtendedTheme.colors.accent,
                    fontSize = 13.sp
                )
            }
        },
        buttons = {
            if (!force) {
                DialogNegativeButton(
                    text = stringResource(id = R.string.button_update_ignore),
                    onClick = { viewModel.ignoreCurrentVersion() }
                )
            }
            DialogPositiveButton(
                text = stringResource(id = R.string.button_update_now),
                onClick = { viewModel.download() }
            )
        }
    )
}

@Composable
private fun UpdateProgressDialog(
    state: UpdateUiState.Downloading
) {
    val dialogState = rememberDialogState()

    LaunchedEffect(state.release.tagName) {
        dialogState.show()
    }

    AlertDialog(
        dialogState = dialogState,
        cancelable = false,
        onDismiss = null,
        title = {
            Text(
                text = stringResource(id = R.string.title_update_available),
                color = ExtendedTheme.colors.text
            )
        },
        content = {
            UpdateReleaseContent(release = state.release)
            Spacer(modifier = Modifier.height(12.dp))
            UpdateDownloadProgress(downloaded = state.downloaded, total = state.total)
        },
        buttons = {}
    )
}

@Composable
private fun UpdateDownloadFailedDialog(
    failed: UpdateUiState.DownloadFailed,
    viewModel: UpdateViewModel
) {
    val dialogState = rememberDialogState()

    LaunchedEffect(failed.release.tagName) {
        dialogState.show()
    }

    AlertDialog(
        dialogState = dialogState,
        title = {
            Text(
                text = stringResource(id = R.string.text_update_download_failed),
                color = ExtendedTheme.colors.text
            )
        },
        content = {
            Text(
                text = failed.message,
                color = ExtendedTheme.colors.textSecondary,
                fontSize = 14.sp
            )
        },
        buttons = {
            DialogNegativeButton(
                text = stringResource(id = R.string.button_cancel)
            )
            DialogPositiveButton(
                text = stringResource(id = R.string.button_update_retry),
                onClick = { viewModel.download() }
            )
        }
    )
}

@Composable
private fun UpdateReadyDialog(
    ready: UpdateUiState.ReadyToInstall,
    viewModel: UpdateViewModel
) {
    val dialogState = rememberDialogState()

    LaunchedEffect(ready.release.tagName, ready.installError) {
        dialogState.show()
    }

    AlertDialog(
        dialogState = dialogState,
        title = {
            Text(
                text = stringResource(id = R.string.title_update_available),
                color = ExtendedTheme.colors.text
            )
        },
        content = {
            UpdateReleaseContent(release = ready.release)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    id = if (ready.verified) R.string.text_update_ready
                    else R.string.text_update_ready_unverified
                ),
                color = ExtendedTheme.colors.textSecondary,
                fontSize = 13.sp
            )
            ready.installError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = ExtendedTheme.colors.accent,
                    fontSize = 13.sp
                )
            }
        },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_update_install),
                onClick = { viewModel.install() }
            )
        }
    )
}
