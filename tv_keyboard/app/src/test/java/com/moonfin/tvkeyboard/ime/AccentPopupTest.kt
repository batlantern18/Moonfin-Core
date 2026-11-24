package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import com.moonfin.tvkeyboard.R

/**
 * The rules behind the mini keyboard: which keys offer a popup and what text a
 * chosen key inserts. Kept on the JVM so shift, locale, and code-point fallback
 * are pinned without an Android view.
 */
class AccentPopupTest {

    @Test
    fun `unshifted selection inserts the label as-is`() {
        assertEquals("à", AccentPopup.textFor("à", 224, shifted = false, locale = Locale.FRANCE))
    }

    @Test
    fun `shifted selection uppercases an accent label`() {
        assertEquals("À", AccentPopup.textFor("à", 224, shifted = true, locale = Locale.FRANCE))
    }

    @Test
    fun `the active locale decides the case mapping`() {
        assertEquals("İ", AccentPopup.textFor("i", 105, shifted = true, locale = Locale("tr", "TR")))
    }

    @Test
    fun `a key without a label falls back to its code point`() {
        assertEquals("é", AccentPopup.textFor(null, 233, shifted = false, locale = Locale.US))
    }

    @Test
    fun `shift also uppercases the code-point fallback`() {
        assertEquals("É", AccentPopup.textFor(null, 233, shifted = true, locale = Locale.US))
    }

    @Test
    fun `a key with no text at all inserts nothing`() {
        assertNull(AccentPopup.textFor(null, KeyCodes.DELETE, shifted = false, locale = Locale.US))
        assertNull(AccentPopup.textFor(null, null, shifted = false, locale = Locale.US))
    }

    @Test
    fun `a command key never inserts its label`() {
        assertNull(AccentPopup.textFor("Clipboard", KeyCodes.CLIPBOARD, shifted = false, locale = Locale.US))
        assertNull(AccentPopup.textFor("Mic", KeyCodes.VOICE, shifted = false, locale = Locale.US))
    }

    @Test
    fun `a popup space key inserts a space`() {
        assertEquals(" ", AccentPopup.textFor("Space", KeyCodes.SPACE, shifted = false, locale = Locale.US))
    }

    @Test
    fun `only keys with a vendored popup resource offer the mini keyboard`() {
        assertTrue(AccentPopup.hasPopup(popupResId = 1))
        assertFalse(AccentPopup.hasPopup(popupResId = 0))
    }

    @Test
    fun `hint dot is shown for keys whose popup carries accented variants`() {
        assertTrue(AccentPopup.isAccentBearing(R.xml.accent_a))
        assertTrue(AccentPopup.isAccentBearing(R.xml.accent_e))
        assertTrue(AccentPopup.isAccentBearing(R.xml.accent_n))
        assertTrue(AccentPopup.isAccentBearing(R.xml.accent_ko_kr_e))
    }

    @Test
    fun `hint dot is hidden on shift, punctuation and command popups`() {
        assertFalse(AccentPopup.isAccentBearing(R.xml.shift_q))
        assertFalse(AccentPopup.isAccentBearing(R.xml.shift_1))
        assertFalse(AccentPopup.isAccentBearing(R.xml.accent_slash))
        assertFalse(AccentPopup.isAccentBearing(R.xml.accent_voice))
        assertFalse(AccentPopup.isAccentBearing(0))
    }
}
