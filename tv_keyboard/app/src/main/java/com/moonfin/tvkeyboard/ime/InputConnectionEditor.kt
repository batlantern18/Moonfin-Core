package com.moonfin.tvkeyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * The production [TextEditor]: applies each edit to the live [InputConnection]
 * and reports the change exactly once, so the caller renders once per edit
 * transaction.
 *
 * The connection is read through [connection] on every call because the IME
 * swaps it per input session; a missing connection is a no-op, not an error.
 */
class InputConnectionEditor(
    private val connection: () -> InputConnection?,
    private val autoSpace: () -> Boolean,
    private val onChanged: () -> Unit,
) : TextEditor {

    override fun insert(text: String) = edit { it.commitText(text, 1) }

    override fun deleteBeforeCursor() = edit { connection ->
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            connection.commitText("", 1)
        } else if (!connection.deleteSurroundingText(1, 0)) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    override fun moveCursorLeft() = sendKey(KeyEvent.KEYCODE_DPAD_LEFT)

    override fun moveCursorRight() = sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)

    override fun applySuggestion(suggestion: Suggestion) = edit { connection ->
        if (suggestion.replacePrefix > 0) {
            connection.deleteSurroundingText(suggestion.replacePrefix, 0)
        }
        connection.commitText(suggestion.text, 1)
        if (autoSpace()) connection.commitText(" ", 1)
    }

    override fun paste(text: String) = edit { connection ->
        connection.commitText(text, 1)
        if (autoSpace()) connection.commitText(" ", 1)
    }

    override fun performAction(actionId: Int) = edit { it.performEditorAction(actionId) }

    override fun textBeforeCursor(max: Int): String =
        connection()?.getTextBeforeCursor(max, 0)?.toString().orEmpty()

    override fun hasSelection(): Boolean =
        connection()?.getSelectedText(0)?.isNotEmpty() == true

    private fun sendKey(keyCode: Int) = edit { connection ->
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun edit(block: (InputConnection) -> Unit) {
        val connection = connection() ?: return
        block(connection)
        onChanged()
    }
}
