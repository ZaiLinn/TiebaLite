package com.huanchengfly.tieba.post.ui.page.settings.more

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.dependNot
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.BackupManager
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.ThreadCache
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

private fun BackupManager.Summary.describe(context: Context): String =
    context.getString(R.string.text_backup_summary, preferences, blocks, drafts, topForums, histories)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MoreSettingsPage(
    navigator: NavHostController,
) {
    val coroutineScope = rememberCoroutineScope()
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_settings_more),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.popBackStack() })
                }
            )
        },
    ) { paddingValues ->
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current
        var cacheSize by remember { mutableStateOf("0.0B") }
        var privateMessageEnabled by remember { mutableStateOf(context.appPreferences.enablePrivateMessage) }
        val privateMessageRiskDialogState = rememberDialogState()
        ConfirmDialog(
            dialogState = privateMessageRiskDialogState,
            onConfirm = {
                context.appPreferences.enablePrivateMessage = true
                privateMessageEnabled = true
            },
            confirmText = stringResource(id = R.string.button_private_message_enable),
            title = { Text(text = stringResource(id = R.string.title_private_message_experimental)) },
        ) {
            Text(text = stringResource(id = R.string.message_private_message_risk))
        }
        val backupLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val message = runCatching { BackupManager.export(context, uri) }
                    .fold(
                        onSuccess = { context.getString(R.string.toast_backup_success, it.describe(context)) },
                        onFailure = {
                            Log.e("MoreSettingsPage", "backup failed", it)
                            context.getString(R.string.toast_backup_failed, it.message.orEmpty())
                        }
                    )
                snackbarHostState.showSnackbar(message)
            }
        }
        val restoreLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val message = runCatching { BackupManager.import(context, uri) }
                    .fold(
                        onSuccess = { context.getString(R.string.toast_restore_success, it.describe(context)) },
                        onFailure = {
                            Log.e("MoreSettingsPage", "restore failed", it)
                            context.getString(R.string.toast_restore_failed, it.message.orEmpty())
                        }
                    )
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            }
        }
        val restoreDialogState = rememberDialogState()
        ConfirmDialog(
            dialogState = restoreDialogState,
            onConfirm = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            title = { Text(text = stringResource(id = R.string.title_restore)) },
        ) {
            Text(text = stringResource(id = R.string.message_restore_confirm))
        }
        var threadCacheSize by remember { mutableStateOf("0.0B") }
        LaunchedEffect(Unit) {
            thread {
                cacheSize = ImageCacheUtil.getCacheSize(context)
                threadCacheSize = ImageCacheUtil.getFormatSize(ThreadCache.size(context).toDouble())
            }
        }
        PrefsScreen(
            dataStore = LocalContext.current.dataStore,
            dividerThickness = 0.dp,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ic_chrome),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    key = "use_webview",
                    title = stringResource(id = R.string.title_use_webview),
                    defaultChecked = true,
                    summary = {
                        if (it) stringResource(id = R.string.tip_use_webview_on)
                        else stringResource(id = R.string.tip_use_webview)
                    },
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ic_today),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = dependNot(key = "use_webview"),
                    key = "use_custom_tabs",
                    title = stringResource(id = R.string.title_use_custom_tabs),
                    defaultChecked = true,
                    summary = {
                        if (it) stringResource(id = R.string.tip_use_custom_tab_on)
                        else stringResource(id = R.string.tip_use_custom_tab)
                    },
                )
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_private_message_experimental),
                    summary = stringResource(
                        id = if (privateMessageEnabled) R.string.tip_private_message_on
                        else R.string.tip_private_message_off
                    ),
                    onClick = {
                        if (privateMessageEnabled) {
                            context.appPreferences.enablePrivateMessage = false
                            privateMessageEnabled = false
                        } else {
                            privateMessageRiskDialogState.show()
                        }
                    },
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Mail,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Backup,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = true,
                    title = stringResource(id = R.string.title_backup),
                    summary = stringResource(id = R.string.summary_backup),
                    onClick = {
                        val date = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
                        backupLauncher.launch("tiebalite-backup-$date.json")
                    },
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.SettingsBackupRestore,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = true,
                    title = stringResource(id = R.string.title_restore),
                    summary = stringResource(id = R.string.summary_restore),
                    onClick = { restoreDialogState.show() },
                )
            }
            prefsItem {
                SwitchPref(
                    key = "cacheViewedThreads",
                    title = stringResource(id = R.string.title_cache_viewed_threads),
                    summary = stringResource(id = R.string.summary_cache_viewed_threads),
                    defaultChecked = true,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.CloudDownload,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.DeleteSweep,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = true,
                    title = stringResource(id = R.string.title_clear_thread_cache),
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) { ThreadCache.clear(context) }
                            threadCacheSize = "0.0B"
                            snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_thread_cache_success))
                        }
                    },
                    summary = stringResource(id = R.string.tip_cache, threadCacheSize)
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.OfflineBolt,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = true,
                    title = stringResource(id = R.string.title_clear_picture_cache),
                    onClick = {
                        coroutineScope.launch {
                            ImageCacheUtil.clearImageAllCache(context)
                            cacheSize = "0.0B"
                            snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_picture_cache_success))
                        }
                    },
                    summary = stringResource(id = R.string.tip_cache, cacheSize)
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Info,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = true,
                    title = stringResource(id = R.string.title_about),
                    onClick = {
                        navigator.navigate("settings/about")
                    },
                    summary = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME)
                )
            }
        }
    }
}