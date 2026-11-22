package com.moonfin.tvkeyboard.ime

import java.util.Locale

/**
 * LeanKeyboard's reserved key codes (the vendored XML depends on these exact
 * negative values).
 */
object KeyCodes {
    const val SHIFT = -1
    const val MODE_CHANGE = -2
    const val LEFT = -3
    const val RIGHT = -4
    const val DELETE = -5
    const val VOICE = -7
    const val LANGUAGE = -9
    const val CLIPBOARD = -10
    const val SPACE = 32
}

enum class KeyAction {
    TEXT,
    SPACE,
    DELETE,
    CURSOR_LEFT,
    CURSOR_RIGHT,
    SHIFT,
    MODE_CHANGE,
    LANGUAGE,
    VOICE,
    CLIPBOARD,
    NONE,
}

/** Pure mapping from a key's primary code to what the editor should do. */
object TextCommitter {

    fun resolve(keyCode: Int): KeyAction = when (keyCode) {
        KeyCodes.SHIFT -> KeyAction.SHIFT
        KeyCodes.MODE_CHANGE -> KeyAction.MODE_CHANGE
        KeyCodes.LEFT -> KeyAction.CURSOR_LEFT
        KeyCodes.RIGHT -> KeyAction.CURSOR_RIGHT
        KeyCodes.DELETE -> KeyAction.DELETE
        KeyCodes.VOICE -> KeyAction.VOICE
        KeyCodes.LANGUAGE -> KeyAction.LANGUAGE
        KeyCodes.CLIPBOARD -> KeyAction.CLIPBOARD
        KeyCodes.SPACE -> KeyAction.SPACE
        else -> if (keyCode > 0) KeyAction.TEXT else KeyAction.NONE
    }

    /**
     * Applies Shift/Caps to a key label. Non-letters pass through untouched and
     * the layout's locale drives the case mapping (Turkish i/İ, for example).
     */
    fun applyShift(label: CharSequence, shifted: Boolean, locale: Locale): CharSequence {
        if (!shifted) return label
        val text = label.toString()
        if (text.isEmpty()) return label
        if (text.length > 2) return label
        return text.uppercase(locale)
    }

    /** Text a key should insert, or null when the key is not a plain text key. */
    fun textFor(keyCode: Int, label: CharSequence?, shifted: Boolean, locale: Locale): String? {
        if (resolve(keyCode) != KeyAction.TEXT) return null
        val base: CharSequence = label?.takeIf { it.isNotEmpty() } ?: codePointText(keyCode) ?: return null
        return applyShift(base, shifted, locale).toString()
    }

    fun codePointText(keyCode: Int): String? {
        if (keyCode <= 0) return null
        return try {
            String(Character.toChars(keyCode))
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
