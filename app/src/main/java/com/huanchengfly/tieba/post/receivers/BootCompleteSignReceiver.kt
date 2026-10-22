package com.huanchengfly.tieba.post.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.Util
import com.huanchengfly.tieba.post.utils.appPreferences
import java.util.Calendar

class BootCompleteSignReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val autoSign = context.appPreferences.autoSign
            if (autoSign) {
                val autoSignTimeStr = context.appPreferences.autoSignTime
                // 开机时今天的签到时间已过且今天还没签过，就补签一次
                if (Util.getTimeInMillis(autoSignTimeStr) <= System.currentTimeMillis()) {
                    val signDay = context.appPreferences.signDay
                    if (signDay != Calendar.getInstance()[Calendar.DAY_OF_MONTH]) {
                        runCatching { TiebaUtil.startSign(context) }
                    }
                }
                runCatching { TiebaUtil.initAutoSign(context) }
            }
        }
    }
}
