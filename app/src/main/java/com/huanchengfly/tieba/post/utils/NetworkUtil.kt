package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import com.huanchengfly.tieba.post.App

fun isNetworkConnected(context: Context = App.INSTANCE): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        val activeNetwork = connectivityManager.activeNetworkInfo ?: return false
        return activeNetwork.isConnected
    } else {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

object NetworkUtil {
    fun isNetworkConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val mConnectivityManager = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mNetworkInfo = mConnectivityManager.activeNetworkInfo ?: return false
        return mNetworkInfo.isConnected
    }

    /**
     * 列表里每张图片都会判断一次是否 Wi-Fi（决定加载大图还是小图），
     * 每次都查 ConnectivityManager 是一次跨进程调用，快速滑动时会卡主线程。
     * 结果缓存 [WIFI_STATE_CACHE_MS]，网络切换后最多晚这么久生效。
     */
    private const val WIFI_STATE_CACHE_MS = 3_000L

    @Volatile
    private var cachedWifiConnected = false

    @Volatile
    private var cachedWifiCheckedAt = 0L

    fun isWifiConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val now = SystemClock.elapsedRealtime()
        if (cachedWifiCheckedAt != 0L && now - cachedWifiCheckedAt < WIFI_STATE_CACHE_MS) {
            return cachedWifiConnected
        }
        val mConnectivityManager = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connected = mConnectivityManager
            .getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected == true
        cachedWifiConnected = connected
        cachedWifiCheckedAt = now
        return connected
    }

    fun isMobileConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val mConnectivityManager = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mMobileNetworkInfo = mConnectivityManager
            .getNetworkInfo(ConnectivityManager.TYPE_MOBILE) ?: return false
        return mMobileNetworkInfo.isConnected
    }
}