package com.moonfin.tvkeyboard.ime

/**
 * The word currently being typed, held as a composing region instead of
 * committed text. The field underlines it, the host can replace it with a
 * prediction, and Backspace can shave it one character at a time without ever
 * touching committed text.
 */
class Composition {

    private val chars = StringBuilder()

    val value: String get() = chars.toString()

    val isEmpty: Boolean get() = chars.isEmpty()

    val length: Int get() = chars.length

    fun append(text: String) {
        chars.append(text)
    }

    /** Removes the last character; true when nothing is left to compose. */
    fun deleteLast(): Boolean {
        if (chars.isNotEmpty()) chars.setLength(chars.length - 1)
        return chars.isEmpty()
    }

    fun clear() {
        chars.setLength(0)
    }

    /**
     * Checks the composition against what the field holds before the cursor.
     * The host can change the text behind the keyboard, so a composition that
     * no longer matches is dropped rather than appended to. A null read means
     * the field cannot report its text, in which case the composition stands.
     */
    fun adopt(fieldText: String?): Boolean {
        if (fieldText == null) return true
        if (!fieldText.endsWith(chars)) {
            clear()
            return false
        }
        return true
    }
}
