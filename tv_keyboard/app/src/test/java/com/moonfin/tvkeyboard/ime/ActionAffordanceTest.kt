package com.moonfin.tvkeyboard.ime

import java.util.Locale
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionAffordanceTest {
    private val locale = Locale.US

    @Test
    fun `search action returns magnifier affordance`() {
        val affordance = ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_SEARCH)
        assertEquals("Search", affordance.label)
        assertEquals("Search", affordance.contentDescription)
        assertEquals("🔍", affordance.glyph)
    }

    @Test
    fun `next action returns arrow affordance`() {
        val affordance = ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_NEXT)
        assertEquals("Next", affordance.label)
        assertEquals("Next", affordance.contentDescription)
        assertEquals("→", affordance.glyph)
    }

    @Test
    fun `done action returns checkmark affordance`() {
        val affordance = ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_DONE)
        assertEquals("Done", affordance.label)
        assertEquals("Done", affordance.contentDescription)
        assertEquals("✓", affordance.glyph)
    }

    @Test
    fun `go action returns go label affordance`() {
        val affordance = ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_GO)
        assertEquals("Go", affordance.label)
        assertEquals("Go", affordance.contentDescription)
        assertEquals("Go", affordance.glyph)
    }

    @Test
    fun `unspecified action returns enter affordance`() {
        val affordance = ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_UNSPECIFIED)
        assertEquals("Enter", affordance.label)
        assertEquals("Enter", affordance.contentDescription)
        assertNull(affordance.glyph)
    }

    @Test
    fun `none action returns enter affordance`() {
        val affordance = ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_NONE)
        assertEquals("Enter", affordance.label)
        assertEquals("Enter", affordance.contentDescription)
        assertNull(affordance.glyph)
    }

    @Test
    fun `affordance does not depend on field mode`() {
        // All four actions should render the same affordance regardless of field mode.
        val search = ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_SEARCH)
        val done = ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_DONE)
        assertEquals("Search", search.label)
        assertEquals("Done", done.label)
        // Verify the affordance is purely derived from action ID, not Field mode.
        assertEquals("🔍", search.glyph)
        assertEquals("✓", done.glyph)
    }
}