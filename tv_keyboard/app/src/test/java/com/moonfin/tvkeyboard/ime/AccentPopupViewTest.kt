package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.inputmethodservice.Keyboard
import android.view.KeyEvent
import android.view.View
import com.moonfin.tvkeyboard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Robolectric coverage of the popup's open, navigate, select and cancel path.
 * The popup is driven through its public key handling, so the test observes
 * the same behaviour a remote produces.
 */
@RunWith(RobolectricTestRunner::class)
class AccentPopupViewTest {

    private lateinit var context: Context
    private lateinit var popup: AccentPopupView
    private var selected: Keyboard.Key? = null
    private var cancelled = false

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        popup = AccentPopupView(context)
        popup.onSelected = { selected = it }
        popup.onCancel = { cancelled = true }
    }

    private fun open(resId: Int, rtl: Boolean = false) {
        popup.rtl = rtl
        popup.show(resId)
        popup.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.AT_MOST),
        )
        popup.layout(0, 0, 1080, popup.measuredHeight)
    }

    private fun press(keyCode: Int) {
        popup.onKeyDown(keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }

    @Test
    fun `opening focuses the first choice and is visible`() {
        open(R.xml.accent_e)

        assertTrue(popup.isVisible)
        assertEquals(0, popup.focusedIndex)
    }

    @Test
    fun `right and center select the next accent`() {
        open(R.xml.accent_e)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals("è", selected?.label?.toString())
        assertFalse(popup.isVisible)
    }

    @Test
    fun `a shifted number popup selects its symbol`() {
        open(R.xml.shift_1)
        press(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals("!", selected?.label?.toString())
    }

    @Test
    fun `enter selects like center`() {
        open(R.xml.accent_e)
        press(KeyEvent.KEYCODE_ENTER)

        assertEquals("e", selected?.label?.toString())
    }

    @Test
    fun `the voice popup exposes its clipboard key`() {
        open(R.xml.accent_voice)
        press(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(KeyCodes.CLIPBOARD, selected?.codes?.firstOrNull())
    }

    @Test
    fun `down past the bottom row cancels without selecting`() {
        open(R.xml.accent_e)
        press(KeyEvent.KEYCODE_DPAD_DOWN)

        assertTrue(cancelled)
        assertFalse(popup.isVisible)
        assertNull(selected)
    }

    @Test
    fun `back cancels without selecting`() {
        open(R.xml.accent_e)
        press(KeyEvent.KEYCODE_BACK)

        assertTrue(cancelled)
        assertFalse(popup.isVisible)
        assertNull(selected)
    }

    @Test
    fun `right-to-left layouts mirror the choices`() {
        open(R.xml.accent_e, rtl = true)

        // The first key is now the rightmost, so Right has nowhere to go and
        // Left walks back through the mirrored row.
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(0, popup.focusedIndex)

        press(KeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals(1, popup.focusedIndex)
    }

    @Test
    fun `cyclic popup wraps left to the last choice`() {
        popup.cyclic = true
        open(R.xml.accent_e)

        press(KeyEvent.KEYCODE_DPAD_LEFT)

        assertEquals(8, popup.focusedIndex)
    }

    @Test
    fun `up on a single-row popup stays put`() {
        popup.cyclic = true
        open(R.xml.accent_e)

        press(KeyEvent.KEYCODE_DPAD_UP)

        assertEquals(0, popup.focusedIndex)
    }
}
