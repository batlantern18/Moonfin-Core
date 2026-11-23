package com.moonfin.tvkeyboard.ime

/**
 * The module through which the keyboard changes the field the user is typing into.
 *
 * One user intent arrives as one call here, and the implementation reports back
 * once, so the caller never has to re-run a refresh ritual of its own. Reads sit
 * on the same interface so nothing outside the implementation touches the field.
 */
interface TextEditor {

    /** Append a typed word character to the composing region. */
    fun compose(text: String)

    /** The word currently composed in the field, or an empty string. */
    fun composition(): String

    /** Insert text at the cursor, finishing any composition first. */
    fun insert(text: String)

    /** Remove the selection, the last composed character, or the character before the cursor. */
    fun deleteBeforeCursor()

    fun moveCursorLeft()

    fun moveCursorRight()

    /** Replace the word the suggestion was built from, honouring auto-space. */
    fun applySuggestion(suggestion: Suggestion)

    /** Insert a whole token from the clipboard, honouring auto-space. */
    fun paste(text: String)

    /** Run the field's editor action, e.g. Search or Done. */
    fun performAction(actionId: Int)

    /** Text between the start of the field and the cursor, capped at [max]. */
    fun textBeforeCursor(max: Int): String

    /** Text after the cursor, capped at [max]. The echo line needs both sides. */
    fun textAfterCursor(max: Int): String

    /** Whether the field currently has a non-empty selection. */
    fun hasSelection(): Boolean
}
