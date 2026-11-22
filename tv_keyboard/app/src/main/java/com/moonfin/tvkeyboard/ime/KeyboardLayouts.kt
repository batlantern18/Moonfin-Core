package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.inputmethodservice.Keyboard
import com.moonfin.tvkeyboard.R

/**
 * Resolves the vendored LeanKeyboard layout XML. Alphabet layouts are
 * `qwerty_<code>` (or `azerty_<code>`), symbols are always `sym_en_us` and a
 * numeric field uses the four-column `number` layout, matching LeanKeyboard.
 */
object KeyboardLayouts {

    const val SYMBOLS_RESOURCE = "sym_en_us"
    const val NUMBERS_RESOURCE = "number"
    const val FALLBACK_RESOURCE = "qwerty_en_us"

    fun alphabet(context: Context, language: LanguageLayout): Keyboard =
        Keyboard(context, resourceId(context, language.alphabetResource))

    fun symbols(context: Context): Keyboard =
        Keyboard(context, resourceId(context, SYMBOLS_RESOURCE))

    fun numbers(context: Context): Keyboard =
        Keyboard(context, resourceId(context, NUMBERS_RESOURCE))

    /** Returns the fallback layout instead of crashing on a missing resource. */
    fun resourceId(context: Context, name: String): Int {
        val id = context.resources.getIdentifier(name, "xml", context.packageName)
        return if (id != 0) id else R.xml.qwerty_en_us
    }
}
