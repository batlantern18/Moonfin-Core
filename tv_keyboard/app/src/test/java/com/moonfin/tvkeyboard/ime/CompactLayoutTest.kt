package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.inputmethodservice.Keyboard
import com.moonfin.tvkeyboard.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Every vendored layout still fits when the window is compact instead of full screen. */
@RunWith(RobolectricTestRunner::class)
class CompactLayoutTest {

    private fun cellsFor(context: Context, resId: Int, width: Int): List<NavKey> {
        val keyboard = Keyboard(context, resId)
        return KeyboardGeometry.scaled(
            keys = keyboard.keys.mapIndexed { index, key ->
                RawKeyRect(index, key.x, key.y, key.width, key.height)
            },
            layoutWidth = keyboard.minWidth,
            layoutHeight = keyboard.height,
            viewWidth = width,
        )
    }

    @Test
    fun `alphabet, symbols and numbers all fit the compact width`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val compact = WindowPlacement.widthPx(1920)

        listOf(R.xml.qwerty_en_us, R.xml.sym_en_us, R.xml.number).forEach { resId ->
            val cells = cellsFor(context, resId, compact)

            assertTrue("layout $resId produced no keys", cells.isNotEmpty())
            assertFalse(
                "layout $resId overflows the compact width",
                cells.any { it.left < -0.5f || it.right > compact + 0.5f },
            )
            assertTrue(
                "layout $resId is not using the compact width",
                cells.maxOf { it.right } > compact * 0.9f,
            )
        }
    }

    @Test
    fun `the same layout scales down with the window`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val full = cellsFor(context, R.xml.qwerty_en_us, 1920)
        val compact = cellsFor(context, R.xml.qwerty_en_us, WindowPlacement.widthPx(1920))

        assertTrue(full.first().right > compact.first().right)
        assertTrue(compact.first().right > 0f)
    }
}
