package com.moonfin.tvkeyboard.ime

/**
 * Spoken text for a focused key. TalkBack on TV hears only the view's
 * description, so it has to carry the label *and* whether that label will be
 * typed as a capital — the glyph alone cannot say.
 */
object KeyAnnouncements {

    fun describe(label: String, keyCode: Int, shifted: Boolean): String = when {
        keyCode == KeyCodes.SHIFT -> "Shift, " + if (shifted) "on" else "off"
        label.isEmpty() -> "Key"
        label.length == 1 && label[0].isLetter() ->
            label + ", " + if (label[0].isUpperCase()) "capital" else "lowercase"
        else -> label
    }
}
