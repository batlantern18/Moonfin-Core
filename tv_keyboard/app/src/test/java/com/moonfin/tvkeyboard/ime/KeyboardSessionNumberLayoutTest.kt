package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for automatic number layout for numeric/phone fields.
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardSessionNumberLayoutTest {

    private lateinit var context: Context
    private lateinit var session: KeyboardSession

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        session = KeyboardSession(context, KeyboardPreferences(context))
    }

    @Test
    fun testNumericFieldShowsNumbersLayout() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        session.beginInput(editorInfo, false)
        assertEquals(KeyboardSession.Mode.NUMBERS, session.mode)
    }

    @Test
    fun testPhoneFieldShowsNumbersLayout() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_PHONE
        }
        session.beginInput(editorInfo, false)
        assertEquals(KeyboardSession.Mode.NUMBERS, session.mode)
    }

    @Test
    fun testTextFieldShowsAlphabetLayout() {
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        session.beginInput(editorInfo, false)
        assertEquals(KeyboardSession.Mode.ALPHABET, session.mode)
    }

    @Test
    fun testDefaultShowsAlphabetLayout() {
        session.beginInput(null, false)
        assertEquals(KeyboardSession.Mode.ALPHABET, session.mode)
    }
}
