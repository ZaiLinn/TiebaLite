package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display

/**
 * 请求当前分辨率下刷新率最高的显示模式（例如 120Hz）。
 *
 * 只在与当前模式分辨率相同的模式里挑：supportedModes 包含所有分辨率，直接取刷新率最高的
 * 可能选中另一个分辨率，系统会拒绝或折中成别的刷新率。是否最终生效仍由系统策略决定。
 */
fun Activity.requestHighestRefreshRate() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    try {
        val display: Display = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else null)
            ?: @Suppress("DEPRECATION") windowManager.defaultDisplay
            ?: return
        val current = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate }
            ?: return
        AppLog.d(
            "RefreshRate",
            "current ${current.modeId} ${current.physicalWidth}x${current.physicalHeight}@${current.refreshRate}, " +
                "supported " + display.supportedModes.joinToString { "${it.modeId}:${it.physicalWidth}x${it.physicalHeight}@${it.refreshRate}" } +
                ", choose ${best.modeId}@${best.refreshRate}"
        )
        val params = window.attributes
        if (params.preferredDisplayModeId == best.modeId) return
        params.preferredDisplayModeId = best.modeId
        window.attributes = params
        Log.i("RefreshRate", "request mode ${best.modeId}: ${best.physicalWidth}x${best.physicalHeight}@${best.refreshRate}")
    } catch (e: Exception) {
        Log.w("RefreshRate", "request highest refresh rate failed", e)
    }
}
