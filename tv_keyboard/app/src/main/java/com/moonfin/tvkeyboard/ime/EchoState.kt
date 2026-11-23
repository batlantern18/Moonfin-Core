package com.moonfin.tvkeyboard.ime

/**
 * What the echo line above the keys should show.
 *
 * The keyboard repeats the field's text above the key grid because on a TV the
 * host field can sit far from the keys or be dimmed behind the keyboard window.
 * A password field shows bullets and never the text itself; the caret index is
 * expressed in the displayed string so the view never has to guess.
 *
 * Kept free of Android types so the projection can be tested on the JVM.
 */
data class EchoState(
    val text: String = "",
    val caret: Int = 0,
    val isPassword: Boolean = false,
) {

    val visible: Boolean get() = text.isNotEmpty()

    /** The string the view draws: the text itself, or one bullet per character. */
    val displayText: String
        get() = if (isPassword) {
            BULLET.toString().repeat(text.length.coerceAtMost(MAX_PASSWORD_BULLETS))
        } else {
            text
        }

    /** Caret index inside [displayText]. */
    val displayCaret: Int
        get() = if (isPassword) displayText.length else caret.coerceIn(0, text.length)

    companion object {
        /** Long fields are echoed from the tail; the caret is what matters. */
        const val MAX_TEXT = 160

        /** Passwords echo a fixed number of bullets no matter how long they are. */
        const val MAX_PASSWORD_BULLETS = 24

        private const val BULLET = '\u2022'

        /**
         * Projects the text around the cursor into one line. [before] and
         * [after] are the field contents on either side of the caret; only the
         * tail of a long field is kept, and the caret moves with it.
         */
        fun of(before: String, after: String, isPassword: Boolean): EchoState {
            val combined = before + after
            val text = if (combined.length > MAX_TEXT) combined.takeLast(MAX_TEXT) else combined
            val dropped = combined.length - text.length
            val caret = (before.length - dropped).coerceIn(0, text.length)
            return EchoState(text = text, caret = caret, isPassword = isPassword)
        }
    }
}
