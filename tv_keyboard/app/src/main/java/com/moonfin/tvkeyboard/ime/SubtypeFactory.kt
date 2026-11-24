package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.view.inputmethod.InputMethodSubtype

/**
 * Builds the IME subtypes advertised to the Android system picker. The input
 * method XML holds one static fallback; enabled languages are published as
 * additional subtypes so disabling one can remove it at the next input session.
 */
object SubtypeFactory {

    const val KEYBOARD_MODE = "keyboard"

    fun build(context: Context, languages: List<LanguageLayout>): List<InputMethodSubtype> =
        languages.map { language ->
            InputMethodSubtype.InputMethodSubtypeBuilder()
                .setSubtypeLocale(SubtypeResolver.localeTag(language))
                .setSubtypeMode(KEYBOARD_MODE)
                .setSubtypeExtraValue(SubtypeResolver.extraValue(language))
                .setSubtypeNameResId(
                    context.resources.getIdentifier(
                        SubtypeResolver.labelResourceName(language),
                        "string",
                        context.packageName,
                    ),
                )
                .setIsAsciiCapable(true)
                .build()
        }
}
