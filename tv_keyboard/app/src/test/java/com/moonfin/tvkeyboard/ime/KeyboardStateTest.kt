package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.VisualTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Verifies [KeyboardState.from] collapses the live session and preferences into
 * the immutable aggregate, and that the derived view getters read the same
 * values the views are now fed.
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardStateTest {

    private lateinit var context: Context
    private lateinit var preferences: KeyboardPreferences
    private lateinit var session: KeyboardSession

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        preferences = KeyboardPreferences(context)
        session = KeyboardSession(context, preferences)
    }

    @Test
    fun `from maps every session field into the snapshot`() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        session.beginInput(info, restarting = false)
        session.toggleShift()        // OFF -> ON
        session.toggleMode()         // ALPHABET -> SYMBOLS

        val state = KeyboardState.from(session, preferences)

        assertEquals(KeyboardSession.Mode.SYMBOLS, state.session.mode)
        assertEquals(KeyboardSession.Shift.ON, state.session.shift)
        assertEquals(KeyboardLayouts.SYMBOLS_RESOURCE, state.session.layoutResource)
        assertEquals("Search", state.session.actionLabel)
        assertEquals(EditorInfo.IME_ACTION_SEARCH, state.session.actionId)
        assertEquals(InputType.TYPE_CLASS_TEXT, state.session.inputType)
    }

    @Test
    fun `derived view getters read through the snapshot`() {
        preferences.theme = VisualTheme.NEON_PULSE
        preferences.style = KeyboardStyle.GLASS
        preferences.cyclicNavigation = true
        preferences.suggestionsEnabled = false

        val suggestion = Suggestion("hello", 0)
        val state = KeyboardState.from(session, preferences, listOf(suggestion))

        assertTrue(state.cyclic)
        assertFalse(state.suggestionsVisible) // suggestions row is disabled
        assertEquals(listOf(suggestion), state.suggestions)
        assertEquals(
            com.moonfin.tvkeyboard.theme.ThemeCatalog.palette(VisualTheme.NEON_PULSE, KeyboardStyle.GLASS),
            state.palette,
        )
    }

    @Test
    fun `suggestions are visible only when the row is enabled and non-empty`() {
        val suggestion = Suggestion("word", 4)
        val enabled = KeyboardState.from(session, preferences, listOf(suggestion))
        assertTrue(enabled.suggestionsVisible)

        val disabled = KeyboardState.from(
            session,
            preferences.apply { suggestionsEnabled = false },
            listOf(suggestion),
        )
        assertFalse(disabled.suggestionsVisible)

        val empty = KeyboardState.from(session, preferences, emptyList())
        assertFalse(empty.suggestionsVisible)
    }

    @Test
    fun `selecting a language moves the snapshot forward`() {
        preferences.enabledLanguageKeys = setOf("en_us", "fr")
        session.beginInput(null, restarting = false)
        assertEquals(0, session.languageIndex)

        session.selectLanguageByKey("fr")
        val state = KeyboardState.from(session, preferences)

        assertEquals(1, state.session.languageIndex)
        assertEquals("fr", state.session.languageKey)
        assertEquals("qwerty_fr", state.session.layoutResource)
    }

    @Test
    fun `two identical renders compare equal`() {
        val a = KeyboardState.from(session, preferences)
        val b = KeyboardState.from(session, preferences)
        assertEquals(a, b)
    }
}
