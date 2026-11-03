package com.moonfin.tvkeyboard.prefs

import android.content.Context
import android.content.SharedPreferences
import com.moonfin.tvkeyboard.ime.LanguageLayout
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.VisualTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Tests for KeyboardPreferences persistence and defaults.
 * These verify the settings round-trip through SharedPreferences.
 */
class KeyboardPreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var preferences: KeyboardPreferences

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)

        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSharedPreferences(KeyboardPreferences.FILE, Context.MODE_PRIVATE))
            .thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.putStringSet(anyString(), any())).thenReturn(editor)

        preferences = KeyboardPreferences(context)
    }

    @Test
    fun `theme defaults to MOONFIN`() {
        `when`(prefs.getString(KeyboardPreferences.KEY_THEME, null)).thenReturn(null)
        assertEquals(VisualTheme.MOONFIN, preferences.theme)
    }

    @Test
    fun `theme persists and restores`() {
        `when`(prefs.getString(KeyboardPreferences.KEY_THEME, null)).thenReturn(VisualTheme.NEON_PULSE.id)
        assertEquals(VisualTheme.NEON_PULSE, preferences.theme)
    }

    @Test
    fun `style defaults to DOCKED`() {
        `when`(prefs.getString(KeyboardPreferences.KEY_STYLE, null)).thenReturn(null)
        assertEquals(KeyboardStyle.DOCKED, preferences.style)
    }

    @Test
    fun `style persists and restores`() {
        `when`(prefs.getString(KeyboardPreferences.KEY_STYLE, null)).thenReturn(KeyboardStyle.GLASS.id)
        assertEquals(KeyboardStyle.GLASS, preferences.style)
    }

    @Test
    fun `keepOnScreen defaults to true`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_KEEP_ON_SCREEN, true)).thenReturn(true)
        assertTrue(preferences.keepOnScreen)
    }

    @Test
    fun `enlarge defaults to false`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_ENLARGE, false)).thenReturn(false)
        assertFalse(preferences.enlarge)
    }

    @Test
    fun `suggestionsEnabled defaults to true`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_SUGGESTIONS, true)).thenReturn(true)
        assertTrue(preferences.suggestionsEnabled)
    }

    @Test
    fun `cyclicNavigation defaults to false`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_CYCLIC, false)).thenReturn(false)
        assertFalse(preferences.cyclicNavigation)
    }

    @Test
    fun `autoSpace defaults to true`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_AUTO_SPACE, true)).thenReturn(true)
        assertTrue(preferences.autoSpace)
    }

    @Test
    fun `hideForPhysicalKeyboard defaults to false`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_HIDE_PHYSICAL, false)).thenReturn(false)
        assertFalse(preferences.hideForPhysicalKeyboard)
    }

    @Test
    fun `voiceEnabled defaults to true`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_VOICE, true)).thenReturn(true)
        assertTrue(preferences.voiceEnabled)
    }

    @Test
    fun `autodetectLanguage defaults to false`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_AUTODETECT, false)).thenReturn(false)
        assertFalse(preferences.autodetectLanguage)
    }

    @Test
    fun `enabledLanguageKeys defaults to English`() {
        `when`(prefs.getStringSet(KeyboardPreferences.KEY_LANGUAGES, null)).thenReturn(null)
        assertEquals(setOf("en_us"), preferences.enabledLanguageKeys)
    }

    @Test
    fun `enabledLanguageKeys persists and restores`() {
        `when`(prefs.getStringSet(KeyboardPreferences.KEY_LANGUAGES, null))
            .thenReturn(setOf("en_us", "fr"))
        assertEquals(setOf("en_us", "fr"), preferences.enabledLanguageKeys)
    }
}