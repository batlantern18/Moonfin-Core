package com.moonfin.tvkeyboard.ime

import android.content.Context
import com.moonfin.tvkeyboard.R
import java.util.Locale

/**
 * One entry from the vendored LeanKeyboard `languages.xml` array. The array
 * format is `display name|locale code|board variant|layout code`, where the
 * last two fields are optional. Regional variants keep their own locale code
 * (`en_gb`) but may share a physical board through `layout code` (`en_us`).
 */
data class LanguageLayout(
    val displayName: String,
    val code: String,
    val azerty: Boolean = false,
    /** Distinguishes two boards for one locale, e.g. Hindi transliteration. */
    val layoutVariant: String? = null,
    /** Locale code of the vendored board that backs this language. */
    val layoutCode: String = code,
) {
    /** Unique preference key: `en_us` and `en_us_azerty` are distinct layouts. */
    val key: String
        get() = buildString {
            append(code)
            if (azerty) append("_azerty")
            if (!layoutVariant.isNullOrBlank()) append('_').append(layoutVariant)
        }

    /** Resource name of the alphabet layout, e.g. `azerty_fr`. */
    val alphabetResource: String
        get() = (if (azerty) "azerty_" else "qwerty_") + layoutCode

    /** Locale used for case mapping while typing. */
    val locale: Locale
        get() = Locale.forLanguageTag(code.replace('_', '-'))

    /**
     * Whether the layout reads right-to-left (Arabic, Hebrew, Persian, Urdu).
     * Used to mirror the keyboard and picker instead of fighting the text
     * direction.
     */
    val isRtl: Boolean
        get() = RTL_LANGUAGES.contains(code.substringBefore('_'))

    companion object {
        /** Language primary codes that lay out right-to-left. */
        val RTL_LANGUAGES = setOf("ar", "he", "fa", "ur")
    }
}

object LanguageRegistry {

    @Volatile
    private var cached: List<LanguageLayout>? = null

    fun all(context: Context): List<LanguageLayout> =
        cached ?: parse(context.resources.getStringArray(R.array.additional_languages))
            .also { cached = it }

    internal fun parse(entries: Array<String>): List<LanguageLayout> =
        entries.mapNotNull { entry ->
            val parts = entry.split('|').map { it.trim() }
            if (parts.size < 2) return@mapNotNull null
            val name = parts[0]
            val code = parts[1]
            if (name.isEmpty() || code.isEmpty()) return@mapNotNull null
            val board = parts.getOrNull(2).orEmpty()
            val azerty = board.equals("azerty", ignoreCase = true)
            val variant = board.takeIf { it.isNotEmpty() && !azerty }
            val layoutCode = parts.getOrNull(3)?.takeIf { it.isNotEmpty() } ?: code
            LanguageLayout(
                displayName = name,
                code = code,
                azerty = azerty,
                layoutVariant = variant,
                layoutCode = layoutCode,
            )
        }
}
