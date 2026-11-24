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

    /**
     * Android's `imeSubtypeLocale` form: `en_us` -> `en_US`,
     * `en_au` -> `en_AU`. Script codes such as `Hant` keep their casing so
     * future script-qualified locales remain representable.
     */
    fun localeTag(language: LanguageLayout): String {
        val parts = language.code.replace('-', '_').split('_')
        return parts.mapIndexed { index, part ->
            when {
                index == 0 -> part.lowercase(Locale.ROOT)
                part.length == 4 -> part.replaceFirstChar { it.titlecase(Locale.ROOT) }
                else -> part.uppercase(Locale.ROOT)
            }
        }.joinToString("_")
    }

    /**
     * Extra value sent with every subtype: the marker lets the IME recognise
     * its own subtypes, and the layout key distinguishes boards that share a
     * locale (English AZERTY, Hindi transliteration).
     */
    fun extraValue(language: LanguageLayout): String {
        // Older Android versions read ASCII capability from the extra value.
        val base = "$MOONFIN_MARKER,AsciiCapable"
        val tokens = buildList {
            if (language.azerty) add("$LAYOUT_EXTRA_KEY=$AZERTY_VALUE")
            language.layoutVariant?.let { add("$LAYOUT_EXTRA_KEY=$it") }
        }
        return if (tokens.isEmpty()) base else "$base,${tokens.joinToString(",")}"
    }

    /** Name of the string resource used as the subtype's label. */
    fun labelResourceName(language: LanguageLayout): String =
        "subtype_${language.key.lowercase(Locale.ROOT)}"

    /**
     * Resolve the layout for a subtype chosen in system settings. An exact
     * locale match wins; a regional locale with no registered board of its own
     * may fall back only to a bare language board, never to a different
     * regional board such as the English default.
     */
    fun resolve(
        languages: List<LanguageLayout>,
        locale: String?,
        extraValue: String? = null,
    ): LanguageLayout? {
        val target = locale?.takeIf { it.isNotBlank() }?.let(::normalize) ?: return null
        val exact = languages.filter { normalize(it.code) == target }
        val candidates = when {
            exact.isNotEmpty() -> exact
            target.contains('_') -> {
                // `en_NZ` must not quietly become `en_US`: only a bare `en`
                // entry is a legitimate language-level fallback.
                val bareLanguage = target.substringBefore('_')
                languages.filter { normalize(it.code) == bareLanguage }
            }
            else -> languages.filter { normalize(it.code).substringBefore('_') == target }
        }
        if (candidates.isEmpty()) return null

        val wanted = layoutFromExtra(extraValue)
        return candidates.firstOrNull { matchesLayout(it, wanted) } ?: candidates.first()
    }

    private fun matchesLayout(language: LanguageLayout, wanted: String?): Boolean =
        when {
            wanted == null -> !language.azerty && language.layoutVariant == null
            wanted.equals(AZERTY_VALUE, ignoreCase = true) -> language.azerty
            else -> language.layoutVariant?.equals(wanted, ignoreCase = true) == true
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
