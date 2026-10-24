package com.huanchengfly.tieba.post.ui.page.settings.oksign

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.depend
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TimePickerPerf
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.OKSignRecord
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.powerManager
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.utils.requestPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 切换自动签到或修改签到时间后立即重新安排闹钟，并告诉用户结果。
 * 之前只写入了设置，要等下次冷启动才生效，点了没有任何反馈。
 */
private fun applyAutoSign(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    enabled: Boolean,
    time: String? = null,
) {
    val nextTime = runCatching {
        if (time != null) TiebaUtil.initAutoSign(context, enabled, time)
        else TiebaUtil.initAutoSign(context, enabled)
    }.onFailure {
        Log.e("OKSignSettingsPage", "initAutoSign failed", it)
    }.getOrNull()
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.requestPermission {
            permissions = listOf(PermissionUtils.POST_NOTIFICATIONS)
            description = context.getString(R.string.desc_permission_post_notifications)
        }
    }
    coroutineScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        if (!enabled) {
            snackbarHostState.showSnackbar(context.getString(R.string.toast_auto_sign_off))
            return@launch
        }
        if (nextTime == null) {
            snackbarHostState.showSnackbar(context.getString(R.string.error_unknown))
            return@launch
        }
        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextTime))
        val nextText = context.getString(
            if (DateUtils.isToday(nextTime)) R.string.text_today_time else R.string.text_tomorrow_time,
            timeText
        )
        val message = context.getString(R.string.toast_auto_sign_on, nextText)
        if (TiebaUtil.canScheduleExactAlarms(context)) {
            snackbarHostState.showSnackbar(message)
        } else {
            val result = snackbarHostState.showSnackbar(
                message = "$message\n${context.getString(R.string.toast_auto_sign_inexact)}",
                actionLabel = context.getString(R.string.button_go_to_grant),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }
        }
    }
}

private fun formatRecordTime(time: Long): String {
    val pattern = if (DateUtils.isToday(time)) "HH:mm" else "MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(time))
}

@Composable
private fun OKSignFailedDialog(
    dialogState: DialogState,
    record: OKSignRecord,
    onRetry: () -> Unit,
) {
    Dialog(
        dialogState = dialogState,
        title = { Text(text = stringResource(id = R.string.title_oksign_failed_forums)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_oksign_retry_failed, record.failures.size),
                onClick = onRetry,
            )
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(record.failures) {
                Text(
                    text = stringResource(id = R.string.text_oksign_failed_forum, it.forumName, it.errorMsg),
                    color = ExtendedTheme.colors.text,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun OKSignSettingsPage(
    navigator: NavHostController,
) {
    val coroutineScope = rememberCoroutineScope()
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_oksign),
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.popBackStack() })
                }
            )
        },
    ) { paddingValues ->
        val context = LocalContext.current
        val dataStore = context.dataStore
        val snackbarHostState = LocalSnackbarHostState.current
        var lastRecord by remember { mutableStateOf(OKSignRecord.load(context)) }
        // 签到在 :oksign 进程里跑，回到本页时重新读一次结果
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) lastRecord = OKSignRecord.load(context)
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        val failedDialogState = rememberDialogState()
        lastRecord?.let { record ->
            OKSignFailedDialog(
                dialogState = failedDialogState,
                record = record,
                onRetry = {
                    runCatching { TiebaUtil.startSign(context, retryFailed = true) }
                        .onSuccess {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.toast_oksign_retry_started))
                            }
                        }
                        .onFailure {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.toast_oksign_failed, it.message.orEmpty())
                                )
                            }
                        }
                },
            )
        }
        PrefsScreen(
            dataStore = dataStore,
            dividerThickness = 0.dp,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            prefsItem {
                val record = lastRecord
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.History,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    title = stringResource(id = R.string.title_oksign_last_record),
                    summary = when {
                        record == null -> stringResource(id = R.string.summary_oksign_no_record)
                        record.failures.isEmpty() -> stringResource(
                            id = R.string.summary_oksign_last_record,
                            formatRecordTime(record.time),
                            record.userName,
                            record.successCount,
                            record.total,
                        )

                        else -> stringResource(
                            id = R.string.summary_oksign_last_record_failed,
                            formatRecordTime(record.time),
                            record.userName,
                            record.successCount,
                            record.total,
                            record.failures.size,
                        )
                    },
                    darkenOnDisable = false,
                    enabled = record != null && record.failures.isNotEmpty(),
                    onClick = { failedDialogState.show() },
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.BrowseGallery,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    key = "oksign_slow_mode",
                    title = stringResource(id = R.string.title_oksign_slow_mode),
                    defaultChecked = true,
                    summaryOn = stringResource(id = R.string.summary_oksign_slow_mode_on),
                    summaryOff = stringResource(id = R.string.summary_oksign_slow_mode),
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.Speed,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    key = "oksign_use_official_oksign",
                    title = stringResource(id = R.string.title_oksign_use_official_oksign),
                    defaultChecked = true,
                    summary = stringResource(id = R.string.summary_oksign_use_official_oksign),
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.OfflinePin,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    key = "auto_sign",
                    title = stringResource(id = R.string.title_auto_sign),
                    defaultChecked = false,
                    summaryOn = stringResource(id = R.string.summary_auto_sign_on),
                    summaryOff = stringResource(id = R.string.summary_auto_sign),
                    onCheckedChange = {
                        applyAutoSign(context, coroutineScope, snackbarHostState, it)
                    },
                )
            }
            prefsItem {
                TimePickerPerf(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.WatchLater,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    key = "auto_sign_time",
                    title = stringResource(id = R.string.title_auto_sign_time),
                    defaultValue = "09:00",
                    summary = { stringResource(id = R.string.summary_auto_sign_time, it) },
                    dialogTitle = stringResource(id = R.string.title_auto_sign_time),
                    enabled = depend(key = "auto_sign"),
                    onValueSaved = {
                        applyAutoSign(context, coroutineScope, snackbarHostState, true, it)
                    }
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.BatteryAlert,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    title = stringResource(id = R.string.title_ignore_battery_optimization),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !context.isIgnoringBatteryOptimizations(),
                    summary =
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) stringResource(id = R.string.summary_battery_optimization_old_android_version)
                    else if (context.isIgnoringBatteryOptimizations()) stringResource(id = R.string.summary_battery_optimization_ignored)
                    else stringResource(id = R.string.summary_ignore_battery_optimization),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!context.powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                context.requestIgnoreBatteryOptimizations()
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.toast_ignore_battery_optimization_already))
                                }
                            }
                        }
                    }
                )
            }

            prefsItem {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(stringResource(id = R.string.tip_start))
                        }
                        append(stringResource(id = R.string.tip_auto_sign))
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color = ExtendedTheme.colors.chip)
                        .padding(12.dp),
                    color = ExtendedTheme.colors.onChip,
                    fontSize = 12.sp
                )
            }
        }
    }
}