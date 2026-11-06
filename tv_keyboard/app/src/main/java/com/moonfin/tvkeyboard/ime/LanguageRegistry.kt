package com.moonfin.tvkeyboard.ime

import android.content.Context
import com.moonfin.tvkeyboard.R
import java.util.Locale

/** One entry from the vendored LeanKeyboard `languages.xml` array. */
data class LanguageLayout(
    val displayName: String,
    val code: String,
    val azerty: Boolean,
) {
    /** Unique preference key: `en_us` and `en_us_azerty` are distinct layouts. */
    val key: String
        get() = if (azerty) "${code}_azerty" else code

    /** Resource name of the alphabet layout, e.g. `azerty_fr`. */
    val alphabetResource: String
        get() = (if (azerty) "azerty_" else "qwerty_") + code

    /** Locale used for case mapping while typing. */
    val locale: Locale
        get() = Locale.forLanguageTag(code.replace('_', '-'))

    /**
     * Whether the layout reads right-to-left (Arabic, Hebrew, Persian). Used to
     * mirror the keyboard and picker instead of fighting the text direction.
     */
    val isRtl: Boolean
        get() = RTL_LANGUAGES.contains(code.substringBefore('_'))

    companion object {
        /** Language primary codes that lay out right-to-left. */
        val RTL_LANGUAGES = setOf("ar", "he", "fa")
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
            val parts = entry.split('|')
            if (parts.size < 2) return@mapNotNull null
            val name = parts[0].trim()
            val code = parts[1].trim()
            if (name.isEmpty() || code.isEmpty()) return@mapNotNull null
            LanguageLayout(
                displayName = name,
                code = code,
                azerty = parts.getOrNull(2)?.trim().equals("azerty", ignoreCase = true),
            )
        }
}
