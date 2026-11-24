package com.moonfin.tvkeyboard.ime

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** A finished dictation hands D-pad focus to the primary action key. */
@RunWith(RobolectricTestRunner::class)
class KeyboardRootViewFocusTest {

    private fun root(): KeyboardRootView = KeyboardRootView(RuntimeEnvironment.getApplication())

    @Test
    fun `the handoff focuses the primary action key`() {
        val root = root()
        root.showActionButton(ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_SEARCH))

        root.requestActionFocus()

        assertTrue(root.findFocus() === root.actionButton)
        assertTrue(root.actionButton!!.isFocused)
    }

    @Test
    fun `the handoff keeps focus on the keys when there is no action button`() {
        val root = root()
        root.requestKeyboardFocus()

        root.requestActionFocus()

        assertTrue(root.findFocus() is KeyboardView)
    }

    @Test
    fun `a hidden action button is not a handoff target`() {
        val root = root()
        root.showActionButton(ActionAffordance.affordanceFor(EditorInfo.IME_ACTION_GO))
        root.hideActionButton()
        root.requestKeyboardFocus()

        root.requestActionFocus()

        assertTrue(root.findFocus() is KeyboardView)
    }
}
