package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.inputmethodservice.Keyboard
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.moonfin.tvkeyboard.R
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for the editor action button: affordance rendering and key press handling.
 * The button must show the correct icon/label for each editor action (Search, Next, Done, Go)
 * and pressing it must trigger the listener's onActionButton callback.
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardRootViewActionButtonTest {

    private lateinit var context: Context
    private lateinit var root: KeyboardRootView
    private var actionButtonPressed = false
    private var lastActionId = -1

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        root = KeyboardRootView(context)
        root.listener = object : KeyboardRootView.Listener {
            override fun onKey(key: android.inputmethodservice.Keyboard.Key) = Unit
            override fun onActionButton() {
                actionButtonPressed = true
            }
            override fun onSuggestionSelected(suggestion: Suggestion, index: Int) = Unit
            override fun onAccentSelected(text: String) = Unit
        }
        root.keyboard = Keyboard(context, R.xml.qwerty_en_us)
        layout()
        root.requestKeyboardFocus()
    }

    private fun layout() {
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 1080, 600)
    }

    private fun applyState(actionId: Int, fieldMode: FieldMode = FieldMode.TEXT) {
        actionButtonPressed = false
        lastActionId = actionId

        val preferences = KeyboardPreferences(context)
        val session = KeyboardSession(context, preferences)
        val info = EditorInfo().apply {
            inputType = when (fieldMode) {
                FieldMode.TEXT -> InputType.TYPE_CLASS_TEXT
                FieldMode.EMAIL -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                FieldMode.PASSWORD -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                FieldMode.URL -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                FieldMode.NUMERIC -> InputType.TYPE_CLASS_NUMBER
            }
            imeOptions = actionId
        }
        session.beginInput(info, restarting = false)

        val state = KeyboardState.from(session, preferences)
        root.apply(state, session.keyboard)
        layout()
    }

    private fun dispatchToActionButton(keyCode: Int): Boolean {
        val button = root.actionButton
        if (button == null || button.visibility != View.VISIBLE) return false
        button.requestFocus()
        val down = button.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        button.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return down
    }

    @Test
    fun `action button shows magnifier for IME_ACTION_SEARCH`() {
        applyState(EditorInfo.IME_ACTION_SEARCH)

        val button = root.actionButton
        assertNotNull("Action button should be visible", button)
        assertEquals(View.VISIBLE, button!!.visibility)
        assertEquals("Search", button.label)
        assertEquals("Search", button.contentDescription)
    }

    @Test
    fun `action button shows arrow for IME_ACTION_NEXT`() {
        applyState(EditorInfo.IME_ACTION_NEXT)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Next", button!!.label)
        assertEquals("Next", button.contentDescription)
    }

    @Test
    fun `action button shows checkmark for IME_ACTION_DONE`() {
        applyState(EditorInfo.IME_ACTION_DONE)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Done", button!!.label)
        assertEquals("Done", button.contentDescription)
    }

    @Test
    fun `action button shows Go label for IME_ACTION_GO`() {
        applyState(EditorInfo.IME_ACTION_GO)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Go", button!!.label)
        assertEquals("Go", button.contentDescription)
    }

    @Test
    fun `action button shows Enter for IME_ACTION_UNSPECIFIED`() {
        applyState(EditorInfo.IME_ACTION_UNSPECIFIED)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Enter", button!!.label)
        assertEquals("Enter", button.contentDescription)
    }

    @Test
    fun `action button shows Done for IME_ACTION_NONE`() {
        applyState(EditorInfo.IME_ACTION_NONE)

        // The session normalizes NONE to DONE so the key always performs an action.
        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Done", button!!.label)
        assertEquals("Done", button.contentDescription)
    }

    @Test
    fun `dpad center on action button triggers onActionButton`() {
        applyState(EditorInfo.IME_ACTION_SEARCH)

        val handled = dispatchToActionButton(KeyEvent.KEYCODE_DPAD_CENTER)

        assertTrue("Key event should be handled", handled)
        assertTrue("onActionButton should be called", actionButtonPressed)
    }

    @Test
    fun `enter key on action button triggers onActionButton`() {
        applyState(EditorInfo.IME_ACTION_NEXT)

        val handled = dispatchToActionButton(KeyEvent.KEYCODE_ENTER)

        assertTrue(handled)
        assertTrue(actionButtonPressed)
    }

    @Test
    fun `numpad enter on action button triggers onActionButton`() {
        applyState(EditorInfo.IME_ACTION_DONE)

        val handled = dispatchToActionButton(KeyEvent.KEYCODE_NUMPAD_ENTER)

        assertTrue(handled)
        assertTrue(actionButtonPressed)
    }

    @Test
    fun `dpad up on action button returns focus to keyboard`() {
        applyState(EditorInfo.IME_ACTION_GO)

        val button = root.actionButton!!
        button.requestFocus()
        assertTrue(button.hasFocus())

        val handled = button.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
        button.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))

        assertTrue("DPAD_UP should be handled", handled)
        assertTrue("Keyboard should have focus", root.keyboardView.hasFocus())
    }

    @Test
    fun `affordance is derived from actionId not fieldMode - SEARCH in password field still shows magnifier`() {
        // Password field with SEARCH action should still show Search affordance
        applyState(EditorInfo.IME_ACTION_SEARCH, FieldMode.PASSWORD)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Search", button!!.label)
        assertEquals("Search", button.contentDescription)
    }

    @Test
    fun `affordance is derived from actionId not fieldMode - NEXT in email field shows arrow`() {
        // Email field with NEXT action should show Next affordance
        applyState(EditorInfo.IME_ACTION_NEXT, FieldMode.EMAIL)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Next", button!!.label)
        assertEquals("Next", button.contentDescription)
    }

    @Test
    fun `affordance is derived from actionId not fieldMode - DONE in numeric field shows checkmark`() {
        // Numeric field with DONE action should show Done affordance
        applyState(EditorInfo.IME_ACTION_DONE, FieldMode.NUMERIC)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Done", button!!.label)
        assertEquals("Done", button.contentDescription)
    }

    @Test
    fun `affordance is derived from actionId not fieldMode - GO in text field shows Go label`() {
        // Text field with GO action should show Go affordance
        applyState(EditorInfo.IME_ACTION_GO, FieldMode.TEXT)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Go", button!!.label)
        assertEquals("Go", button.contentDescription)
    }

    @Test
    fun `action button is still shown when actionId is NONE and fieldMode is PASSWORD`() {
        applyState(EditorInfo.IME_ACTION_NONE, FieldMode.PASSWORD)

        val button = root.actionButton
        assertNotNull(button)
        assertEquals("Done", button!!.label)
    }

    @Test
    fun `click on action button triggers onActionButton`() {
        applyState(EditorInfo.IME_ACTION_SEARCH)

        val button = root.actionButton!!
        button.performClick()

        assertTrue("onActionButton should be called on click", actionButtonPressed)
    }

    @Test
    fun `back key on action button returns focus to keyboard`() {
        applyState(EditorInfo.IME_ACTION_DONE)

        val button = root.actionButton!!
        button.requestFocus()
        assertTrue(button.hasFocus())

        val handled = button.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        button.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))

        assertTrue("BACK should be handled", handled)
        assertTrue("Keyboard should have focus", root.keyboardView.hasFocus())
    }
}