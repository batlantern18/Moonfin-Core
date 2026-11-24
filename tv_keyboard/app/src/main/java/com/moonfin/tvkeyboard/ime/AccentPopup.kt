package com.moonfin.tvkeyboard.ime

import android.inputmethodservice.Keyboard
import com.moonfin.tvkeyboard.R
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
     * True only for letter keys whose popup holds accented variants. Shift-only
     * popups (digits and plain letters), the slash set and the voice/clipboard
     * set are not accents, so they must not show the long-press hint dot.
     */
    fun isAccentBearing(key: Keyboard.Key): Boolean = isAccentBearing(key.popupResId)

    /** Resource-id form of [isAccentBearing], usable without a [Keyboard.Key]. */
    fun isAccentBearing(popupResId: Int): Boolean = popupResId in ACCENT_POPUPS

    private val ACCENT_POPUPS = setOf(
        R.xml.accent_a,
        R.xml.accent_ar_qm,
        R.xml.accent_ar_r,
        R.xml.accent_bg_i,
        R.xml.accent_c,
        R.xml.accent_d,
        R.xml.accent_de_b,
        R.xml.accent_e,
        R.xml.accent_fa_a,
        R.xml.accent_g,
        R.xml.accent_i,
        R.xml.accent_is_a,
        R.xml.accent_is_e,
        R.xml.accent_is_i,
        R.xml.accent_is_o,
        R.xml.accent_is_u,
        R.xml.accent_is_y,
        R.xml.accent_k,
        R.xml.accent_ka_q,
        R.xml.accent_ko_kr_e,
        R.xml.accent_ko_kr_o,
        R.xml.accent_ko_kr_p,
        R.xml.accent_ko_kr_q,
        R.xml.accent_ko_kr_r,
        R.xml.accent_ko_kr_t,
        R.xml.accent_ko_kr_w,
        R.xml.accent_l,
        R.xml.accent_n,
        R.xml.accent_o,
        R.xml.accent_s,
        R.xml.accent_t,
        R.xml.accent_u,
        R.xml.accent_y,
        R.xml.accent_z,
    )

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
