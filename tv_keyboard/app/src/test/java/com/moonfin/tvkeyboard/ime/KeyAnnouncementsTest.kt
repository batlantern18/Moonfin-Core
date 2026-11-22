package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyAnnouncementsTest {

    @Test
    fun `a shifted letter is announced as a capital`() {
        assertEquals("A, capital", KeyAnnouncements.describe("A", 'a'.code, shifted = true))
    }

    @Test
    fun `an unshifted letter is announced as lowercase`() {
        assertEquals("a, lowercase", KeyAnnouncements.describe("a", 'a'.code, shifted = false))
    }

    @Test
    fun `the shift key reports its own state`() {
        assertEquals("Shift, off", KeyAnnouncements.describe("Shift", KeyCodes.SHIFT, shifted = false))
        assertEquals("Shift, on", KeyAnnouncements.describe("Shift", KeyCodes.SHIFT, shifted = true))
    }

    @Test
    fun `function keys are announced by their label`() {
        assertEquals("Del", KeyAnnouncements.describe("Del", KeyCodes.DELETE, shifted = false))
        assertEquals("Space", KeyAnnouncements.describe("Space", KeyCodes.SPACE, shifted = false))
    }

    @Test
    fun `a key with no label is still announced`() {
        assertEquals("Key", KeyAnnouncements.describe("", 0, shifted = false))
    }
}
