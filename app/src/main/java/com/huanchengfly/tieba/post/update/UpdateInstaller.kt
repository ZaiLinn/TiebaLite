package com.huanchengfly.tieba.post.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import android.util.Log
import java.io.File

/**
 * APK 安装器：FileProvider + ACTION_VIEW + application/vnd.android.package-archive。
 * 未授予"安装未知应用"权限时引导到系统设置页。
 */
object UpdateInstaller {

    private const val TAG = "UpdateInstaller"
    private const val AUTHORITY = "com.huanchengfly.tieba.post.share.FileProvider"

    /**
     * 尝试安装 APK。
     *
     * @return true 已拉起安装界面；false 缺少"安装未知应用"权限（已跳转系统设置页，用户授权后重试）。
     */
    fun installApk(context: Context, apk: File): Boolean {
        if (!apk.exists() || apk.length() == 0L) {
            Log.w(TAG, "apk missing: ${apk.absolutePath}")
            return false
        }
        if (!canRequestInstall(context)) {
            guideToInstallPermission(context)
            return false
        }
        return launchInstall(context, apk)
    }

    /**
     * 当前是否已授予"安装未知应用"权限。
     */
    fun canRequestInstall(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * 引导用户到"安装未知应用"授权页。
     */
    fun guideToInstallPermission(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "no settings activity for unknown sources: $e")
        }
    }

    private fun launchInstall(context: Context, apk: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(context, AUTHORITY, apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                Log.w(TAG, "no activity handles package archive")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "install failed: $e")
            false
        }
    }
}
