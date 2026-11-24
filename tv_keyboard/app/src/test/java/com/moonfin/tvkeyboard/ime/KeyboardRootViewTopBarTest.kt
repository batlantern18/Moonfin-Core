package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The strip owns its height: empty it collapses, populated it pushes the keys down. */
@RunWith(RobolectricTestRunner::class)
class KeyboardRootViewTopBarTest {

    private fun measure(root: KeyboardRootView): Int {
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.AT_MOST),
        )
        return root.measuredHeight
    }

    @Test
    fun `the strip collapses when there is nothing to show`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val root = KeyboardRootView(context)

        root.topBar.content = TopBarContent.EMPTY
        val collapsed = measure(root)

        root.topBar.content = TopBarContent.of(FieldMode.PASSWORD)
        val expanded = measure(root)

        assertTrue(expanded > collapsed)
    }

    @Test
    fun `the voice plate keeps the strip visible`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val root = KeyboardRootView(context)

        root.topBar.content = TopBarContent.EMPTY
        val collapsed = measure(root)

        root.topBar.content = TopBarContent.of(
            fieldMode = FieldMode.TEXT,
            voiceState = VoiceState.LISTENING,
        )
        val listening = measure(root)

        assertTrue(listening > collapsed)
    }

    @Test
    fun `the voice plate announces the latest partial transcript`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val root = KeyboardRootView(context)

        root.topBar.content = TopBarContent.of(
            fieldMode = FieldMode.TEXT,
            voiceState = VoiceState.LISTENING,
            voicePartial = "hello wor",
        )

        assertEquals("hello wor", root.topBar.contentDescription)
    }
}
