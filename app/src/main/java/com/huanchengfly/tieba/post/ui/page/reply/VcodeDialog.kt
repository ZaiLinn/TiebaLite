package com.huanchengfly.tieba.post.ui.page.reply

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.request.DisplayRequest
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.VcodeChallenge
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.AccountUtil
import org.json.JSONObject

private const val TAG = "VcodeDialog"
private const val CALLBACK_PREFIX = "objc:"

/**
 * 回帖验证码弹窗。网页验证码（类型 4/5/6）在 WebView 里完成，页面通过跳转 `objc:xxx(参数)`
 * 与客户端通信；其他类型显示验证码图片，手动输入。拿到验证值后调用 [onResult]。
 *
 * 协议参考官方客户端 12.51.7.1 的 NewVcodeActivity 及其三个 presenter。
 */
@Composable
fun VcodeDialog(
    challenge: VcodeChallenge,
    message: String,
    onResult: (vcode: String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ExtendedTheme.colors.windowBackground,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.title_vcode),
                    style = MaterialTheme.typography.h6,
                    color = ExtendedTheme.colors.text,
                )
                if (message.isNotEmpty()) {
                    Text(text = message, style = MaterialTheme.typography.body2, color = ExtendedTheme.colors.textSecondary)
                }
                if (challenge.isWebCaptcha) {
                    WebCaptcha(challenge = challenge, onResult = onResult, onClose = onDismiss)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.button_cancel)) }
                    }
                } else {
                    ImageCaptcha(challenge = challenge, onResult = onResult, onDismiss = onDismiss)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebCaptcha(
    challenge: VcodeChallenge,
    onResult: (String) -> Unit,
    onClose: () -> Unit,
) {
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnClose by rememberUpdatedState(onClose)
    val pageUrl = when (challenge.type) {
        "4" -> VcodeChallenge.CAPTCHA_PAGE_URL
        "5" -> VcodeChallenge.DRAG_CAPTCHA_PAGE_URL
        else -> challenge.picUrl
    }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (challenge.type == "5") 340.dp else 420.dp),
        factory = { context ->
            AccountUtil.getBduss()?.let {
                CookieManager.getInstance().setCookie("https://tieba.baidu.com", AccountUtil.getBdussCookie(it))
            }
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        handleCallback(view, request.url.toString())

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                        handleCallback(view, url)

                    private fun handleCallback(view: WebView, url: String): Boolean {
                        if (!url.startsWith(CALLBACK_PREFIX)) return false
                        Log.i(TAG, "callback type=${challenge.type}: ${url.substringBefore('(')}")
                        val arg = callbackArgument(url)?.let(Uri::decode)
                        when {
                            url.contains("loadReady") -> view.runJs(
                                "handleFreshCaptcha",
                                challenge.picUrl, challenge.slideImg, challenge.textImg
                            )

                            url.contains("jsChangeVcode") -> arg?.let { view.runJs(it, challenge.picUrl) }
                            url.contains("jsChangePosition") || url.contains("jsSubmit") ->
                                arg?.takeIf { it.isNotEmpty() }?.let(currentOnResult)

                            url.contains("jsAiCodeBack") ->
                                if (arg.isNullOrEmpty() || arg == "0") currentOnClose() else currentOnResult(arg)
                            // finish（成功动画结束）、jumpToFeedback 等不需要处理
                        }
                        return true
                    }
                }
                loadUrl(pageUrl)
            }
        },
    )
}

@Composable
private fun ImageCaptcha(
    challenge: VcodeChallenge,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    AsyncImage(
        request = DisplayRequest(LocalContext.current, challenge.picUrl),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
    )
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        singleLine = true,
        placeholder = { Text(text = stringResource(id = R.string.hint_vcode)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.button_cancel)) }
        TextButton(onClick = { onResult(input.trim()) }, enabled = input.isNotBlank()) {
            Text(text = stringResource(id = R.string.button_sure_default))
        }
    }
}

/** `objc:name(参数)` → 参数；与官方 VcodeTool.getJsCallback 一致，取第一个 "(" 与第一个 ")" 之间 */
internal fun callbackArgument(url: String): String? {
    val start = url.indexOf('(')
    val end = url.indexOf(')')
    if (start == -1 || end == -1 || start + 1 > end) return null
    return url.substring(start + 1, end)
}

/** 调用页面上的 window.[function]，参数按 JS 字符串转义 */
private fun WebView.runJs(function: String, vararg args: String) {
    if (!function.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*"))) return
    val joined = args.joinToString(",") { JSONObject.quote(it) }
    evaluateJavascript("window.$function($joined)", null)
}
