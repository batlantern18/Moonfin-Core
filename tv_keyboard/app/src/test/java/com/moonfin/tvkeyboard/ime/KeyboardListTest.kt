package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardListTest {

    private val active = KeyboardList.Entry("com.moonfin/.ime.TvKeyboardService", "Moonfin TV Keyboard", true)
    private val other = KeyboardList.Entry("com.google.android.inputmethod/.Gboard", "Gboard", false)

    @Test
    fun `one line per keyboard in install order`() {
        val text = KeyboardList.format(listOf(active, other), activeMarker = "active", emptyText = "none")

        assertEquals(
            "• Moonfin TV Keyboard — active\n• Gboard",
            text,
        )
    }

    @Test
    fun `only the active keyboard carries the marker`() {
        val text = KeyboardList.format(
            listOf(
                active.copy(active = false),
                other.copy(active = true),
            ),
            activeMarker = "active",
            emptyText = "none",
        )

        assertEquals(1, Regex("active").findAll(text).count())
        assertEquals("• Moonfin TV Keyboard\n• Gboard — active", text)
    }

    @Test
    fun `no keyboards reads as the empty message`() {
        assertEquals("none", KeyboardList.format(emptyList(), activeMarker = "active", emptyText = "none"))
    }
}
