package com.moonfin.tvkeyboard.ime

/**
 * What holding a key does. Reference behaviour: Space opens the system keyboard
 * picker, the language key opens the system language settings, Shift locks, and
 * everything else that can hold opens its own popup.
 */
enum class LongPressTarget {
    SHIFT_LOCK,
    INPUT_METHOD_PICKER,
    LANGUAGE_SETTINGS,
    KEY_POPUP,
    ;

    companion object {
        fun of(keyCode: Int?): LongPressTarget = when (keyCode) {
            KeyCodes.SHIFT -> SHIFT_LOCK
            KeyCodes.SPACE -> INPUT_METHOD_PICKER
            KeyCodes.LANGUAGE -> LANGUAGE_SETTINGS
            else -> KEY_POPUP
        }
    }
}
