package com.huanchengfly.tieba.post.ui.page.reply

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VcodeDialogTest {
    @Test
    fun `callback argument is the text between the parentheses`() {
        assertEquals("123,45", callbackArgument("objc:jsChangePosition(123,45)"))
        assertEquals("cb_1", callbackArgument("objc:jsChangeVcode(cb_1)"))
        assertEquals("", callbackArgument("objc:jsSubmit()"))
    }

    @Test
    fun `callback without parentheses has no argument`() {
        assertNull(callbackArgument("objc:loadReady"))
        assertNull(callbackArgument("objc:finish)("))
    }
}
