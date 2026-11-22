package com.moonfin.tvkeyboard.ime

import android.text.InputType

/** A tappable suggestion and how much of the current word it replaces. */
data class Suggestion(val text: String, val replacePrefix: Int)

fun interface CompletionProvider {
    fun completions(prefix: String): List<String>
}

enum class SuggestionMode { DEFAULT, DOMAIN, AUTO_COMPLETE }

/**
 * Mirrors LeanKeyboard's suggestion factory: email/web fields get a list of
 * common domains appended to what was typed; auto-complete fields use the
 * completions the editor reported.
 */
class SuggestionEngine(
    private val provider: CompletionProvider = CompletionProvider { emptyList() },
) {

    fun modeFor(inputType: Int): SuggestionMode {
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (inputClass == InputType.TYPE_CLASS_TEXT &&
            (variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        ) {
            return SuggestionMode.DOMAIN
        }
        return if (inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0) {
            SuggestionMode.AUTO_COMPLETE
        } else {
            SuggestionMode.DEFAULT
        }
    }

    fun suggestions(
        textBeforeCursor: String,
        mode: SuggestionMode,
        domains: List<String> = emptyList(),
        limit: Int = MAX_SUGGESTIONS,
    ): List<Suggestion> {
        val prefix = wordPrefix(textBeforeCursor)
        return when (mode) {
            SuggestionMode.DOMAIN -> domains
                .asSequence()
                .distinct()
                .take(limit)
                .map { Suggestion(it, 0) }
                .toList()

            SuggestionMode.AUTO_COMPLETE, SuggestionMode.DEFAULT -> provider.completions(prefix)
                .asSequence()
                .filter { it.isNotEmpty() }
                .filter { prefix.isEmpty() || it.startsWith(prefix, ignoreCase = true) }
                .distinct()
                .take(limit)
                .map { Suggestion(it, prefix.length) }
                .toList()
        }
    }

    /** The word being typed before the cursor, used for replacement length. */
    fun wordPrefix(textBeforeCursor: String): String {
        val text = textBeforeCursor.takeLast(MAX_PREFIX_LOOKBEHIND).trimEnd()
        val lastBreak = text.indexOfLast { it.isWhitespace() }
        return if (lastBreak >= 0) text.substring(lastBreak + 1) else text
    }

    companion object {
        const val MAX_SUGGESTIONS = 10
        const val MAX_PREFIX_LOOKBEHIND = 60
    }
}
