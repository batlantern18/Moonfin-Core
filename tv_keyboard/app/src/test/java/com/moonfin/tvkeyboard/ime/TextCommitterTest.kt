package com.moonfin.tvkeyboard.ime

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextCommitterTest {

    @Test
    fun `reserved key codes map to their editor actions`() {
        assertEquals(KeyAction.SHIFT, TextCommitter.resolve(KeyCodes.SHIFT))
        assertEquals(KeyAction.MODE_CHANGE, TextCommitter.resolve(KeyCodes.MODE_CHANGE))
        assertEquals(KeyAction.CURSOR_LEFT, TextCommitter.resolve(KeyCodes.LEFT))
        assertEquals(KeyAction.CURSOR_RIGHT, TextCommitter.resolve(KeyCodes.RIGHT))
        assertEquals(KeyAction.DELETE, TextCommitter.resolve(KeyCodes.DELETE))
        assertEquals(KeyAction.SPACE, TextCommitter.resolve(KeyCodes.SPACE))
    }

    @Test
    fun `letter key codes map to text`() {
        assertEquals(KeyAction.TEXT, TextCommitter.resolve('a'.code))
    }

    @Test
    fun `shift uppercases a letter key using the layout locale`() {
        assertEquals("A", TextCommitter.textFor('a'.code, "a", shifted = true, locale = Locale.US))
    }

    @Test
    fun `unshifted letters keep their label`() {
        assertEquals("a", TextCommitter.textFor('a'.code, "a", shifted = false, locale = Locale.US))
    }

    @Test
    fun `turkish locale uppercases i to a dotted capital`() {
        assertEquals("İ", TextCommitter.textFor('i'.code, "i", shifted = true, locale = Locale.forLanguageTag("tr")))
    }

    @Test
    fun `non letter labels are never uppercased`() {
        assertEquals("Mode change", TextCommitter.applyShift("Mode change", shifted = true, locale = Locale.US))
    }

    @Test
    fun `space has no text payload because it is an action`() {
        assertNull(TextCommitter.textFor(KeyCodes.SPACE, "Space", shifted = false, locale = Locale.US))
    }

    @Test
    fun `code points without a label fall back to their character`() {
        assertEquals("@", TextCommitter.textFor('@'.code, null, shifted = false, locale = Locale.US))
    }
}
