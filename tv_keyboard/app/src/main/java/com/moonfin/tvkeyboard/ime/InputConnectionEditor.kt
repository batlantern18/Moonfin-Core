package com.moonfin.tvkeyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * The production [TextEditor]: applies each edit to the live [InputConnection]
 * and reports the change exactly once, so the caller renders once per edit
 * transaction.
 *
 * Typed word characters are composed, not committed: the field holds a composing
 * region that predictions can replace, and Backspace shortens it. Whole tokens —
 * a space, punctuation, an accent, a paste, a suggestion — finish the composition
 * first so nothing is left underlined.
 *
 * The connection is read through [connection] on every call because the IME
 * swaps it per input session; a missing connection is a no-op, not an error.
 */
class InputConnectionEditor(
    private val connection: () -> InputConnection?,
    private val autoSpace: () -> Boolean,
    private val onChanged: () -> Unit,
    private val onTextCommitted: (String) -> Unit = {},
) : TextEditor {

    private val composition = Composition()
    private var lastConnection: InputConnection? = null

    override fun compose(text: String) {
        if (text.isEmpty()) return
        edit { connection ->
            composition.adopt(connection.getTextBeforeCursor(composition.length, 0)?.toString())
            composition.append(text)
            connection.setComposingText(composition.value, 1)
        }
    }

    override fun composition(): String = composition.value

    override fun insert(text: String) = edit { connection ->
        finishComposition(connection)
        connection.commitText(text, 1)
        onTextCommitted(text)
    }

    override fun deleteBeforeCursor() = edit { connection ->
        composition.adopt(connection.getTextBeforeCursor(composition.length, 0)?.toString())
        val selected = connection.getSelectedText(0)
        when {
            !selected.isNullOrEmpty() -> {
                composition.clear()
                connection.commitText("", 1)
            }
            !composition.isEmpty -> {
                if (composition.deleteLast()) {
                    connection.setComposingText("", 1)
                    connection.finishComposingText()
                } else {
                    connection.setComposingText(composition.value, 1)
                }
            }
            !connection.deleteSurroundingText(1, 0) -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        }
    }

    override fun moveCursorLeft() = edit { connection ->
        finishComposition(connection)
        sendKey(connection, KeyEvent.KEYCODE_DPAD_LEFT)
    }

    override fun moveCursorRight() = edit { connection ->
        finishComposition(connection)
        sendKey(connection, KeyEvent.KEYCODE_DPAD_RIGHT)
    }

    override fun applySuggestion(suggestion: Suggestion) = edit { connection ->
        if (suggestion.replacePrefix > 0) {
            composition.adopt(connection.getTextBeforeCursor(composition.length, 0)?.toString())
            if (!composition.isEmpty) {
                // Predictions replace the composing region itself, so the field
                // learns the replacement atomically.
                connection.setComposingText(suggestion.text, 1)
                connection.finishComposingText()
                composition.clear()
            } else {
                connection.deleteSurroundingText(suggestion.replacePrefix, 0)
                connection.commitText(suggestion.text, 1)
            }
            if (autoSpace()) connection.commitText(" ", 1)
        } else {
            // A domain or a digit chip: there is no word to replace, and a
            // trailing space would break an address mid-typing.
            finishComposition(connection)
            connection.commitText(suggestion.text, 1)
        }
        onTextCommitted(suggestion.text)
    }

    override fun paste(text: String) = edit { connection ->
        finishComposition(connection)
        connection.commitText(text, 1)
        if (autoSpace()) connection.commitText(" ", 1)
        onTextCommitted(text)
    }

    override fun performAction(actionId: Int) = edit { connection ->
        finishComposition(connection)
        connection.performEditorAction(actionId)
    }

    override fun textBeforeCursor(max: Int): String =
        connection()?.getTextBeforeCursor(max, 0)?.toString().orEmpty()

    override fun textAfterCursor(max: Int): String =
        connection()?.getTextAfterCursor(max, 0)?.toString().orEmpty()

    override fun hasSelection(): Boolean =
        connection()?.getSelectedText(0)?.isNotEmpty() == true

    /** Ends the composing region without changing what it committed. */
    private fun finishComposition(connection: InputConnection) {
        if (composition.isEmpty) return
        connection.finishComposingText()
        composition.clear()
    }

    private fun sendKey(connection: InputConnection, keyCode: Int) {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun edit(block: (InputConnection) -> Unit) {
        val connection = connection() ?: return
        if (connection !== lastConnection) {
            // A new input session never inherits the old field's composition.
            lastConnection = connection
            composition.clear()
        }
        block(connection)
        onChanged()
    }
}
