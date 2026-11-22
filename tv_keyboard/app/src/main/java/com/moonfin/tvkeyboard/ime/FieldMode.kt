package com.moonfin.tvkeyboard.ime

import android.text.InputType

/**
 * What kind of field the user is typing into, derived once from its input type.
 *
 * This is the field's shape, not the keyboard's layout: [KeyboardSession.Mode]
 * is whether the grid shows letters, symbols or numbers, and this is what the
 * host expects to be entering. Kept free of Android views so the mapping can be
 * tested on the JVM.
 */
enum class FieldMode {
    TEXT,
    EMAIL,
    PASSWORD,
    NUMERIC,
    ;

    /** Voice typing is unavailable where dictation would expose or confuse the field. */
    val allowsVoice: Boolean get() = this != PASSWORD && this != NUMERIC

    companion object {
        fun from(inputType: Int): FieldMode {
            val inputClass = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            if (inputClass == InputType.TYPE_CLASS_NUMBER || inputClass == InputType.TYPE_CLASS_PHONE) {
                return NUMERIC
            }
            if (inputClass == InputType.TYPE_CLASS_TEXT) {
                when (variation) {
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                    -> return PASSWORD

                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                    -> return EMAIL
                }
            }
            return TEXT
        }
    }
}
