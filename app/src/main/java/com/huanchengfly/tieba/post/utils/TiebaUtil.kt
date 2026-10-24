package com.huanchengfly.tieba.post.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.retrofit.doIfFailure
import com.huanchengfly.tieba.post.api.retrofit.doIfSuccess
import com.huanchengfly.tieba.post.components.dialogs.LoadingDialog
import com.huanchengfly.tieba.post.pendingIntentFlagMutable
import com.huanchengfly.tieba.post.receivers.AutoSignAlarm
import com.huanchengfly.tieba.post.services.OKSignService
import com.huanchengfly.tieba.post.toastShort
import androidx.navigation.NavHostController
import java.util.Calendar

object TiebaUtil {
    private fun ClipData.setIsSensitive(isSensitive: Boolean): ClipData = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSensitive)
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun copyText(
        context: Context,
        text: String?,
        toast: String = context.getString(R.string.toast_copy_success),
        isSensitive: Boolean = false
    ) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Tieba Lite", text).setIsSensitive(isSensitive)
        cm.setPrimaryClip(clipData)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            context.toastShort(toast)
        }
    }

    /**
     * 按设置安排（或取消）每日自动签到的闹钟。
     *
     * 设置页切换开关时 DataStore 还没写完，所以允许直接传入新值。
     *
     * @return 下一次触发的时间戳；关闭自动签到时返回 null
     */
    @JvmOverloads
    fun initAutoSign(
        context: Context,
        autoSign: Boolean = context.appPreferences.autoSign,
        autoSignTime: String = context.appPreferences.autoSignTime ?: DEFAULT_AUTO_SIGN_TIME,
    ): Long? {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AutoSignAlarm::class.java),
            pendingIntentFlagMutable()
        )
        if (!autoSign) {
            alarmManager.cancel(pendingIntent)
            return null
        }
        val time = autoSignTime.split(":")
        val hour = time.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = time.getOrNull(1)?.toIntOrNull() ?: 0
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = hour
        calendar[Calendar.MINUTE] = minute
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        // 若今日目标时刻已过，则顺延至明日，确保闹钟始终有效
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 设置精确闹钟需要「闹钟和提醒」权限，没有权限时会直接抛 SecurityException，
            // 之前被 runCatching 吞掉，导致自动签到从未被安排。没有权限就退回到非精确闹钟。
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
        return calendar.timeInMillis
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private const val DEFAULT_AUTO_SIGN_TIME = "09:00"

    @JvmStatic
    fun startSign(context: Context) {
        context.appPreferences.signDay = Calendar.getInstance()[Calendar.DAY_OF_MONTH]
//        OKSignService.enqueueWork(
//            context,
//            Intent()
//                .setClassName(
//                    context.packageName,
//                    "${context.packageName}.services.OKSignService"
//                )
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                .setAction(OKSignService.ACTION_START_SIGN)
//        )
        // applicationId 已改为 com.zailinn.tiebalite，与代码包名不同；
        // 之前用 "${packageName}.services.OKSignService" 拼类名，组件不存在，Service 根本起不来
        ContextCompat.startForegroundService(
            context,
            Intent(context, OKSignService::class.java)
                .setAction(OKSignService.ACTION_START_SIGN)
        )
    }

    @JvmStatic
    @JvmOverloads
    fun shareText(context: Context, text: String, title: String? = null) {
        context.startActivity(Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "${if (title != null) "「$title」\n" else ""}$text\n（分享自贴吧 Lite）"
            )
        })
    }

    suspend fun reportPost(
        context: Context,
        navigator: NavHostController,
        postId: String,
    ) {
        val dialog = LoadingDialog(context).apply { show() }
        TiebaApi.getInstance()
            .checkReportPostAsync(postId)
            .doIfSuccess {
                dialog.dismiss()
                navigator.navigate("webview/${Uri.encode(it.data.url)}")
            }
            .doIfFailure {
                dialog.dismiss()
                context.toastShort(R.string.toast_load_failed)
            }
    }
}