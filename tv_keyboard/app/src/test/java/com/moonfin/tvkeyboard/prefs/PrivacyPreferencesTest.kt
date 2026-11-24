package com.moonfin.tvkeyboard.prefs

import android.content.Context
import android.content.SharedPreferences
import com.moonfin.tvkeyboard.ime.LanguageLayout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.Locale

/**
 * The two privacy controls promised by the product reference:
 * "Share usage statistics" and "Use system languages".
 */
class PrivacyPreferencesTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var preferences: KeyboardPreferences
    private val originalLocale: Locale = Locale.getDefault()

    private val languages = listOf(
        LanguageLayout("English", "en_us", azerty = false),
        LanguageLayout("Français", "fr", azerty = false),
        LanguageLayout("Deutsch", "de", azerty = false),
    )

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

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `share usage statistics defaults off`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_SHARE_USAGE_STATISTICS, false))
            .thenReturn(false)
        assertFalse(preferences.shareUsageStatistics)
    }

    @Test
    fun `share usage statistics persists when enabled`() {
        preferences.shareUsageStatistics = true

        verify(editor).putBoolean(KeyboardPreferences.KEY_SHARE_USAGE_STATISTICS, true)
        verify(editor).apply()

        `when`(prefs.getBoolean(KeyboardPreferences.KEY_SHARE_USAGE_STATISTICS, false))
            .thenReturn(true)
        assertTrue(preferences.shareUsageStatistics)
    }

    @Test
    fun `toggling share usage statistics writes nothing else`() {
        preferences.shareUsageStatistics = true

        // No analytics opt-in, no network flag, no side-effect: one boolean write.
        verify(editor, times(1)).putBoolean(anyString(), anyBoolean())
        verify(editor, times(0)).putString(anyString(), anyString())
        verify(editor, times(0)).putStringSet(anyString(), any())
    }

    @Test
    fun `use system languages defaults on`() {
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_USE_SYSTEM_LANGUAGES, true))
            .thenReturn(true)
        assertTrue(preferences.useSystemLanguages)
    }

    @Test
    fun `use system languages persists when disabled`() {
        preferences.useSystemLanguages = false

        verify(editor).putBoolean(KeyboardPreferences.KEY_USE_SYSTEM_LANGUAGES, false)

        `when`(prefs.getBoolean(KeyboardPreferences.KEY_USE_SYSTEM_LANGUAGES, true))
            .thenReturn(false)
        assertFalse(preferences.useSystemLanguages)
    }

    @Test
    fun `initial layout follows the device locale when use system languages is on`() {
        Locale.setDefault(Locale.FRENCH)
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_USE_SYSTEM_LANGUAGES, true))
            .thenReturn(true)

        assertEquals("fr", preferences.initialLanguage(languages).key)
    }

    @Test
    fun `initial layout falls back to English when use system languages is off`() {
        Locale.setDefault(Locale.FRENCH)
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_USE_SYSTEM_LANGUAGES, true))
            .thenReturn(false)

        assertEquals("en_us", preferences.initialLanguage(languages).key)
    }

    @Test
    fun `a persisted language choice still wins over the device locale`() {
        Locale.setDefault(Locale.FRENCH)
        `when`(prefs.getBoolean(KeyboardPreferences.KEY_USE_SYSTEM_LANGUAGES, true))
            .thenReturn(true)
        `when`(prefs.getString(KeyboardPreferences.KEY_SELECTED_LANGUAGE, null))
            .thenReturn("de")

        assertEquals("de", preferences.initialLanguage(languages).key)
    }
}
