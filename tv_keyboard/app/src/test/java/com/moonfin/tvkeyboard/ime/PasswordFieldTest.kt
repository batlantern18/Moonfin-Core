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

/** Passwords never auto-capitalise, never lock Caps, and never offer themselves. */
@RunWith(RobolectricTestRunner::class)
class PasswordFieldTest {

    private lateinit var context: Context
    private lateinit var session: KeyboardSession

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        session = KeyboardSession(context, KeyboardPreferences(context))
    }

    private fun beginPasswordInput() {
        session.beginInput(
            EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            },
            false,
        )
    }

    @Test
    fun `shift stays off in a password field`() {
        beginPasswordInput()

        session.toggleShift()

        assertEquals(KeyboardSession.Shift.OFF, session.shift)
    }

    @Test
    fun `caps lock cannot be engaged in a password field`() {
        beginPasswordInput()

        session.lockShift()

        assertEquals(KeyboardSession.Shift.OFF, session.shift)
    }

    @Test
    fun `ordinary text fields still shift`() {
        session.beginInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)

        session.toggleShift()

        assertEquals(KeyboardSession.Shift.ON, session.shift)
    }
}
