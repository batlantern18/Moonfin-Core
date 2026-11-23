package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoStateTest {

    @Test
    fun `echo mirrors the text and puts the caret at the cursor`() {
        val echo = EchoState.of(before = "moon", after = "fin", isPassword = false)

        assertEquals("moonfin", echo.text)
        assertEquals(4, echo.caret)
        assertEquals("moonfin", echo.displayText)
        assertEquals(4, echo.displayCaret)
        assertTrue(echo.visible)
    }

    @Test
    fun `password fields echo one bullet per character and never the text`() {
        val echo = EchoState.of(before = "0620", after = "", isPassword = true)

        assertEquals("0620", echo.text)
        assertEquals("\u2022\u2022\u2022\u2022", echo.displayText)
        assertEquals(4, echo.displayCaret)
        assertFalse(echo.displayText.contains("0"))
    }

    @Test
    fun `long fields keep the tail and the caret moves with it`() {
        val before = "a".repeat(EchoState.MAX_TEXT + 20) + "tail"
        val echo = EchoState.of(before = before, after = "", isPassword = false)

        assertEquals(EchoState.MAX_TEXT, echo.text.length)
        assertTrue(echo.text.endsWith("tail"))
        assertEquals(echo.text.length, echo.caret)
    }

    @Test
    fun `an empty field hides the line`() {
        assertFalse(EchoState.of(before = "", after = "", isPassword = false).visible)
        assertEquals("", EchoState().displayText)
    }

    @Test
    fun `passwords cap the number of bullets`() {
        val echo = EchoState.of(
            before = "x".repeat(EchoState.MAX_PASSWORD_BULLETS + 8),
            after = "",
            isPassword = true,
        )

        assertEquals(EchoState.MAX_PASSWORD_BULLETS, echo.displayText.length)
    }
}
