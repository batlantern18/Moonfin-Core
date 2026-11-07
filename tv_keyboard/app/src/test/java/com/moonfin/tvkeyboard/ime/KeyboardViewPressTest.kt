package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.inputmethodservice.Keyboard
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.moonfin.tvkeyboard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Press handling on the main keyboard: a tap types the key once, a hold on a
 * key with a vendored popup opens the mini keyboard instead of typing first.
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardViewPressTest {

    private lateinit var context: Context
    private lateinit var view: KeyboardView
    private val typed = mutableListOf<Int>()
    private val popupsOpened = mutableListOf<Int>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        view = KeyboardView(context)
        view.listener = object : KeyboardView.Listener, KeyboardView.LongPressListener {
            override fun onKey(key: Keyboard.Key) {
                typed += key.codes.first()
            }

            override fun onLongPressShift() = Unit
            override fun onLongPressLanguage() = Unit
            override fun onLongPressKey(key: Keyboard.Key) {
                popupsOpened += key.popupResId
            }
        }
        view.keyboard = Keyboard(context, R.xml.qwerty_en_us)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 1080, 400)
    }

    /** The first key of the vendored layout is `1`, which has a popup. */
    private fun firstKeyCentre(): Pair<Float, Float> {
        val keyboard = Keyboard(context, R.xml.qwerty_en_us)
        val cell = KeyboardGeometry.scaled(
            keys = keyboard.keys.mapIndexed { index, key ->
                RawKeyRect(index, key.x, key.y, key.width, key.height)
            },
            layoutWidth = keyboard.minWidth,
            layoutHeight = keyboard.height,
            viewWidth = 1080,
        ).first()
        return cell.centerX to cell.centerY
    }

    private fun pressKey(keyCode: Int) {
        view.onKeyDown(keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }

    private fun releaseKey(keyCode: Int) {
        view.onKeyUp(keyCode, KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun waitForLongPress() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700))
    }

    private fun touch(action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(0L, 0L, action, x, y, 0)
        view.onTouchEvent(event)
        event.recycle()
    }

    @Test
    fun `short center press types the base character once`() {
        pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
        releaseKey(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf(49), typed)
        assertTrue(popupsOpened.isEmpty())
    }

    @Test
    fun `long center press opens the popup without typing the base character`() {
        pressKey(KeyEvent.KEYCODE_DPAD_CENTER)
        waitForLongPress()
        releaseKey(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf(R.xml.shift_1), popupsOpened)
        assertTrue(typed.isEmpty())
    }

    @Test
    fun `short tap types the base character once`() {
        val (x, y) = firstKeyCentre()
        touch(MotionEvent.ACTION_DOWN, x, y)
        touch(MotionEvent.ACTION_UP, x, y)

        assertEquals(listOf(49), typed)
        assertTrue(popupsOpened.isEmpty())
    }

    @Test
    fun `long tap opens the popup without typing the base character`() {
        val (x, y) = firstKeyCentre()
        touch(MotionEvent.ACTION_DOWN, x, y)
        waitForLongPress()
        touch(MotionEvent.ACTION_UP, x, y)

        assertEquals(listOf(R.xml.shift_1), popupsOpened)
        assertTrue(typed.isEmpty())
    }
}
