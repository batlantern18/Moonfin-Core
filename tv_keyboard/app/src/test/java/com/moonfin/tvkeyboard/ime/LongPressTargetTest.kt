package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class LongPressTargetTest {

    @Test
    fun `space opens the system input method picker`() {
        assertEquals(LongPressTarget.INPUT_METHOD_PICKER, LongPressTarget.of(KeyCodes.SPACE))
    }

    @Test
    fun `language key opens the system language settings`() {
        assertEquals(LongPressTarget.LANGUAGE_SETTINGS, LongPressTarget.of(KeyCodes.LANGUAGE))
    }

    @Test
    fun `shift still locks on hold`() {
        assertEquals(LongPressTarget.SHIFT_LOCK, LongPressTarget.of(KeyCodes.SHIFT))
    }

    @Test
    fun `every other key holds to open its own popup`() {
        assertEquals(LongPressTarget.KEY_POPUP, LongPressTarget.of('a'.code))
        assertEquals(LongPressTarget.KEY_POPUP, LongPressTarget.of(KeyCodes.DELETE))
        assertEquals(LongPressTarget.KEY_POPUP, LongPressTarget.of(KeyCodes.MODE_CHANGE))
        assertEquals(LongPressTarget.KEY_POPUP, LongPressTarget.of(null))
    }
}
