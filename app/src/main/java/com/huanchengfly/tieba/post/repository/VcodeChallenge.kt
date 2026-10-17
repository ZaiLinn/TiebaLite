package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.models.protos.PostAntiInfo
import java.io.IOException

/**
 * 回帖时服务器要求的验证码（错误码 5/6，信息在响应 data.info 里）。
 *
 * 字段和类型含义参考官方客户端 12.51.7.1（n0099/open-tbclient）的 NewVcodeActivity：
 * - 类型 4：加载 [CAPTCHA_PAGE_URL]，页面回传 `objc:jsSubmit(值)`；
 * - 类型 5：加载 [DRAG_CAPTCHA_PAGE_URL] 拖动滑块，页面回传 `objc:jsChangePosition(位置)`；
 * - 类型 6：直接加载 [picUrl]，页面回传 `objc:jsAiCodeBack(值)`；
 * - 其他：老式图片验证码，看 [picUrl] 图片手动输入。
 * 拿到的值作为 vcode，连同 [md5] 和类型重新提交回帖。
 */
data class VcodeChallenge(
    val md5: String,
    val picUrl: String,
    val type: String,
    val textImg: String = "",
    val slideImg: String = "",
) {
    val isWebCaptcha: Boolean get() = type in WEB_CAPTCHA_TYPES

    companion object {
        const val CAPTCHA_PAGE_URL = "https://tieba.baidu.com/mo/q/captcha"
        const val DRAG_CAPTCHA_PAGE_URL = "https://tieba.baidu.com/n/captcha-drag"
        private val WEB_CAPTCHA_TYPES = setOf("4", "5", "6")

        fun from(info: PostAntiInfo?): VcodeChallenge? {
            info ?: return null
            if (info.vcode_md5.isEmpty() && info.vcode_pic_url.isEmpty()) return null
            return VcodeChallenge(
                md5 = info.vcode_md5,
                picUrl = info.vcode_pic_url,
                type = info.vcode_type,
                textImg = info.vcode_extra?.textimg.orEmpty(),
                slideImg = info.vcode_extra?.slideimg.orEmpty(),
            )
        }
    }
}

/** 回帖需要先完成验证码；[message] 是服务器的提示 */
class NeedVcodeException(val challenge: VcodeChallenge, message: String) : IOException(message)
