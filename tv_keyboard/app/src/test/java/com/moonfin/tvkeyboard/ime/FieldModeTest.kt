package com.moonfin.tvkeyboard.ime

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldModeTest {

    @Test
    fun `plain text is text entry`() {
        assertEquals(FieldMode.TEXT, FieldMode.from(InputType.TYPE_CLASS_TEXT))
    }

    @Test
    fun `email variations are email entry`() {
        assertEquals(
            FieldMode.EMAIL,
            FieldMode.from(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
        )
        assertEquals(
            FieldMode.EMAIL,
            FieldMode.from(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS),
        )
    }

    @Test
    fun `password variations are password entry`() {
        val text = InputType.TYPE_CLASS_TEXT
        assertEquals(FieldMode.PASSWORD, FieldMode.from(text or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertEquals(FieldMode.PASSWORD, FieldMode.from(text or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
        assertEquals(FieldMode.PASSWORD, FieldMode.from(text or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
    }

    @Test
    fun `number and phone fields are numeric`() {
        assertEquals(FieldMode.NUMERIC, FieldMode.from(InputType.TYPE_CLASS_NUMBER))
        assertEquals(FieldMode.NUMERIC, FieldMode.from(InputType.TYPE_CLASS_PHONE))
    }

    @Test
    fun `voice is allowed only where dictation is sensible`() {
        assertTrue(FieldMode.TEXT.allowsVoice)
        assertTrue(FieldMode.EMAIL.allowsVoice)
        assertFalse(FieldMode.PASSWORD.allowsVoice)
        assertFalse(FieldMode.NUMERIC.allowsVoice)
    }
}
