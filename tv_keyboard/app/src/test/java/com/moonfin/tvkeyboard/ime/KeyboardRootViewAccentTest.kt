package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.inputmethodservice.Keyboard
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import com.moonfin.tvkeyboard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import java.util.Locale

/**
 * The root view owns the popup overlay: opening it above the keyboard, routing
 * D-pad keys to it, committing the chosen text, and returning focus to the key
 * that opened it.
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardRootViewAccentTest {

    private lateinit var context: Context
    private lateinit var root: KeyboardRootView
    private val committed = mutableListOf<String>()
    private val keysPressed = mutableListOf<Int>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        root = KeyboardRootView(context)
        root.listener = object : KeyboardRootView.Listener, KeyboardView.LongPressListener {
            override fun onKey(key: Keyboard.Key) {
                keysPressed += key.codes.first()
            }

            override fun onActionButton() = Unit
            override fun onSuggestionSelected(suggestion: Suggestion, index: Int) = Unit
            override fun onAccentSelected(text: String) {
                committed += text
            }

            override fun onLongPressShift() = Unit
            override fun onLongPressLanguage() = Unit
            override fun onLongPressKey(key: Keyboard.Key) {
                root.showAccentPopup(key.popupResId, shifted = false, locale = Locale.US)
            }
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

    private fun openPopup(shifted: Boolean = false, locale: Locale = Locale.US) {
        root.showAccentPopup(R.xml.accent_e, shifted, locale)
        layout()
    }

    private fun dispatch(keyCode: Int) {
        root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun holdCenter() {
        root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700))
    }

    private fun releaseCenter() {
        root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
    }

    @Test
    fun `tapping a popup key types the base character once`() {
        dispatch(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf(49), keysPressed)
    }

    @Test
    fun `holding center opens the popup without typing the base character`() {
        holdCenter()

        assertTrue(root.isAccentPopupVisible)
        assertTrue(keysPressed.isEmpty())

        releaseCenter()

        assertTrue(root.isAccentPopupVisible)
        assertTrue(keysPressed.isEmpty())
    }

    @Test
    fun `hold, navigate, select commits the chosen accent and returns focus`() {
        // Move the main cursor onto the `e` key, whose popup is accent_e.
        dispatch(KeyEvent.KEYCODE_DPAD_DOWN)
        dispatch(KeyEvent.KEYCODE_DPAD_RIGHT)
        dispatch(KeyEvent.KEYCODE_DPAD_RIGHT)

        holdCenter()
        releaseCenter()
        // Android lays out the newly visible overlay before the next key event.
        layout()

        dispatch(KeyEvent.KEYCODE_DPAD_RIGHT)
        dispatch(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("è"), committed)
        assertTrue(keysPressed.isEmpty())
        assertTrue(root.findFocus() is KeyboardView)
    }

    @Test
    fun `opening the popup makes it visible`() {
        openPopup()

        assertTrue(root.isAccentPopupVisible)
    }

    @Test
    fun `center commits the focused accent`() {
        openPopup()
        dispatch(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("e"), committed)
        assertFalse(root.isAccentPopupVisible)
    }

    @Test
    fun `d-pad moves then center commits the chosen accent`() {
        openPopup()
        dispatch(KeyEvent.KEYCODE_DPAD_RIGHT)
        dispatch(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("è"), committed)
    }

    @Test
    fun `shifted popup commits the uppercase accent`() {
        openPopup(shifted = true)
        dispatch(KeyEvent.KEYCODE_DPAD_RIGHT)
        dispatch(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("È"), committed)
    }

    @Test
    fun `back cancels without committing`() {
        openPopup()
        dispatch(KeyEvent.KEYCODE_BACK)

        assertTrue(committed.isEmpty())
        assertFalse(root.isAccentPopupVisible)
    }

    @Test
    fun `selecting returns focus to the key that opened the popup`() {
        dispatch(KeyEvent.KEYCODE_DPAD_RIGHT)
        val before = (root.findFocus() as KeyboardView).focusedKeyIndex()

        openPopup()
        dispatch(KeyEvent.KEYCODE_DPAD_CENTER)

        val focus = root.findFocus()
        assertTrue(focus is KeyboardView)
        assertEquals(before, (focus as KeyboardView).focusedKeyIndex())
    }
}
