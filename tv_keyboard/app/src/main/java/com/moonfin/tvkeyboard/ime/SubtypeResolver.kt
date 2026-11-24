package com.moonfin.tvkeyboard.ime

import java.util.Locale

/**
 * Pure mapping between an IME subtype's locale/extra value and a Moonfin
 * layout. Kept free of Android types so the system-picker contract can be
 * tested on the JVM.
 */
object SubtypeResolver {

    const val MOONFIN_MARKER = "moonfin"
    const val LAYOUT_EXTRA_KEY = "layout"
    const val AZERTY_VALUE = "azerty"

    /** Android's `imeSubtypeLocale` form, e.g. `en_us` -> `en_US`. */
    fun localeTag(language: LanguageLayout): String {
        val parts = language.code.replace('-', '_').split('_')
        return if (parts.size > 1) {
            "${parts[0].lowercase(Locale.ROOT)}_${parts[1].uppercase(Locale.ROOT)}"
        } else {
            parts[0].lowercase(Locale.ROOT)
        }
    }

    /**
     * Extra value sent with every subtype: the marker lets the IME recognise
     * its own subtypes, and the layout key distinguishes boards that share a
     * locale (English, French, Bulgarian, Turkish).
     */
    fun extraValue(language: LanguageLayout): String {
        // Older Android versions read ASCII capability from the extra value.
        val base = "$MOONFIN_MARKER,AsciiCapable"
        return if (language.azerty) "$base,$LAYOUT_EXTRA_KEY=$AZERTY_VALUE" else base
    }

    /** Name of the string resource used as the subtype's label. */
    fun labelResourceName(language: LanguageLayout): String =
        "subtype_${language.key.lowercase(Locale.ROOT)}"

    /**
     * Resolve the layout for a subtype chosen in system settings. An exact
     * locale match wins; a regional locale such as `fr_CA` falls back to the
     * language. Variant selection comes from the `layout` extra value, with
     * the plain board as the default.
     */
    fun resolve(
        languages: List<LanguageLayout>,
        locale: String?,
        extraValue: String? = null,
    ): LanguageLayout? {
        val target = locale?.takeIf { it.isNotBlank() }?.let(::normalize) ?: return null
        val exact = languages.filter { normalize(it.code) == target }
        val candidates = exact.ifEmpty {
            val language = target.substringBefore('_')
            languages.filter { normalize(it.code).substringBefore('_') == language }
        }
        if (candidates.isEmpty()) return null

        val wantsAzerty = layoutFromExtra(extraValue)?.equals(AZERTY_VALUE, ignoreCase = true) == true
        return candidates.firstOrNull { it.azerty == wantsAzerty } ?: candidates.first()
    }

    private fun normalize(value: String): String =
        value.trim().replace('-', '_').lowercase(Locale.ROOT)

    private fun layoutFromExtra(extraValue: String?): String? =
        extraValue?.split(',')
            ?.map { it.trim() }
            ?.firstNotNullOfOrNull { entry ->
                val separator = entry.indexOf('=')
                if (separator < 0) return@firstNotNullOfOrNull null
                if (entry.substring(0, separator).equals(LAYOUT_EXTRA_KEY, ignoreCase = true)) {
                    entry.substring(separator + 1).trim()
                } else {
                    null
                }
            }
}
