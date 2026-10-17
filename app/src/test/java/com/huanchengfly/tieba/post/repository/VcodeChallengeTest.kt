package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.models.protos.PostAntiInfo
import com.huanchengfly.tieba.post.api.models.protos.VcodeExtra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VcodeChallengeTest {
    @Test
    fun `drag captcha info is read from the anti info`() {
        val challenge = VcodeChallenge.from(
            PostAntiInfo(
                need_vcode = "1",
                vcode_md5 = "md5",
                vcode_pic_url = "https://example.com/pic",
                vcode_type = "5",
                vcode_extra = VcodeExtra(textimg = "text.png", slideimg = "slide.png"),
            )
        )!!
        assertEquals(VcodeChallenge("md5", "https://example.com/pic", "5", "text.png", "slide.png"), challenge)
        assertTrue(challenge.isWebCaptcha)
    }

    @Test
    fun `old image captcha is not a web captcha`() {
        val challenge = VcodeChallenge.from(PostAntiInfo(vcode_md5 = "md5", vcode_pic_url = "p", vcode_type = "1"))!!
        assertFalse(challenge.isWebCaptcha)
    }

    @Test
    fun `missing anti info or empty vcode yields null`() {
        assertNull(VcodeChallenge.from(null))
        assertNull(VcodeChallenge.from(PostAntiInfo(need_vcode = "1")))
    }
}
