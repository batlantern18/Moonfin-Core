package com.moonfin.tvkeyboard.ime

import android.content.Context
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Locale

/**
 * The Use system languages preference decides the layout a fresh input
 * session starts on. A layout the user already picked still wins.
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardSessionLanguagePreferenceTest {

    private lateinit var context: Context
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.FRENCH)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    private fun session(useSystemLanguages: Boolean, selected: String? = null): KeyboardSession {
        val prefs = KeyboardPreferences(context)
        prefs.reset()
        prefs.useSystemLanguages = useSystemLanguages
        prefs.setEnabledLanguageKeys(listOf("en_us", "fr"))
        if (selected != null) prefs.selectedLanguageKey = selected
        return KeyboardSession(context, prefs)
    }

    @Test
    fun `session starts on the device locale layout when use system languages is on`() {
        val session = session(useSystemLanguages = true)

        session.beginInput(null, false)

        assertEquals("fr", session.languageKey)
    }

    @Test
    fun `session starts on English when use system languages is off`() {
        val session = session(useSystemLanguages = false)

        session.beginInput(null, false)

        assertEquals("en_us", session.languageKey)
    }

    @Test
    fun `session keeps an explicitly chosen layout`() {
        val session = session(useSystemLanguages = true, selected = "en_us")

        session.beginInput(null, false)

        assertEquals("en_us", session.languageKey)
    }
}
