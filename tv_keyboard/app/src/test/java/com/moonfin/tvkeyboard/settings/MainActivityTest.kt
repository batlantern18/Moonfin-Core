package com.moonfin.tvkeyboard.settings

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the TV-safe settings activity. These verify the public contract:
 * IME component name strings, intent action strings for system settings.
 */
class MainActivityTest {

    @Test
    fun `imeComponent returns the correct component name strings`() {
        val packageName = "com.moonfin.tvkeyboard"
        val className = "com.moonfin.tvkeyboard.ime.TvKeyboardService"
        
        assertEquals(packageName, MainActivity.IME_PACKAGE_NAME)
        assertEquals(className, MainActivity.IME_CLASS_NAME)
    }

    @Test
    fun `intent actions are correct`() {
        assertEquals("android.settings.INPUT_METHOD_SETTINGS", MainActivity.ACTION_ENABLE_KEYBOARD)
        assertEquals("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS", MainActivity.ACTION_CHOOSE_KEYBOARD)
    }

    @Test
    fun `extra key for choose keyboard is correct`() {
        assertEquals("android.provider.extra.INPUT_METHOD_ID", MainActivity.EXTRA_INPUT_METHOD_ID)
    }
}