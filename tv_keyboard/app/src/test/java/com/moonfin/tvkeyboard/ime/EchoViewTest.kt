package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The echo line collapses when empty and takes a row when there is text. */
@RunWith(RobolectricTestRunner::class)
class EchoViewTest {

    private fun measure(view: EchoView): Int {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        return view.measuredHeight
    }

    @Test
    fun `an empty echo takes no space`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val view = EchoView(context)

        assertEquals(0, measure(view))
    }

    @Test
    fun `echoed text opens the line`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val view = EchoView(context)

        view.echo = EchoState.of(before = "moonfin", after = "", isPassword = false)

        assertTrue(measure(view) > 0)
        assertEquals(View.VISIBLE, view.visibility)
    }

    @Test
    fun `a password echo is bullets`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val view = EchoView(context)

        view.echo = EchoState.of(before = "0620", after = "", isPassword = true)

        assertEquals("\u2022\u2022\u2022\u2022", view.echo.displayText)
    }
}
