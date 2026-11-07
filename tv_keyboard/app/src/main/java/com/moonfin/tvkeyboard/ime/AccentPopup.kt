package com.moonfin.tvkeyboard.ime

import android.inputmethodservice.Keyboard
import java.util.Locale

/**
 * Decides which keys open an accent popup and what a chosen accent inserts.
 * Kept free of [android.view.View] so the rules are unit-testable off-device.
 */
object AccentPopup {

    /** True when the key carries a popup layout of accent choices. */
    fun hasPopup(key: Keyboard.Key): Boolean = hasPopup(key.popupResId)

    /** True when a parsed key references a vendored popup layout. */
    fun hasPopup(popupResId: Int): Boolean = popupResId != 0

    /**
     * The text a chosen accent key inserts. Labels are uppercased per the
     * active one-shot Shift (so `à` becomes `À`); a key with no label falls
     * back to its code point, which Shift uppercases the same way.
     */
    fun textFor(key: Keyboard.Key, shifted: Boolean, locale: Locale): String? =
        textFor(key.label, key.codes.firstOrNull(), shifted, locale)

    /**
     * Label-first text for one popup key. A missing label uses the key's code
     * point, and a key with neither (a function key such as Delete) inserts
     * nothing. The locale drives the case mapping, matching the main keyboard.
     */
    fun textFor(
        label: CharSequence?,
        code: Int?,
        shifted: Boolean,
        locale: Locale,
    ): String? {
        val keyCode = code ?: 0
        return when (TextCommitter.resolve(keyCode)) {
            KeyAction.TEXT -> {
                val base: CharSequence = if (!label.isNullOrEmpty()) {
                    label
                } else {
                    TextCommitter.codePointText(keyCode) ?: return null
                }
                TextCommitter.applyShift(base, shifted, locale).toString()
            }
            KeyAction.SPACE -> " "
            else -> null
        }
    }
}
