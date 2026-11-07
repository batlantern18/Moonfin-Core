package com.moonfin.tvkeyboard.ime

import android.inputmethodservice.Keyboard
import java.util.Locale

/**
 * Decides which keys open an accent popup and what a chosen accent inserts.
 * Kept free of [android.view.View] so the rules are unit-testable off-device.
 */
object AccentPopup {

    /** True when the key carries a popup layout of accent choices. */
    fun hasPopup(key: Keyboard.Key): Boolean = key.popupResId != 0

    /**
     * The text a chosen accent key inserts. A label is uppercased per the
     * active one-shot Shift (so `à` becomes `À`); a key with no label falls
     * back to its code point.
     */
    fun textFor(key: Keyboard.Key, shifted: Boolean, locale: Locale): String? {
        val label = key.label
        if (label != null && label.isNotEmpty()) {
            return TextCommitter.applyShift(label, shifted, locale).toString()
        }
        val code = key.codes.firstOrNull() ?: return null
        return TextCommitter.codePointText(code)
    }
}
