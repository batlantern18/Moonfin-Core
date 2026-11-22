package com.moonfin.tvkeyboard.ime

import android.content.Context

/**
 * Loads the bundled common-word list from assets. Words are sorted alphabetically
 * for deterministic test output. The list is loaded once and cached.
 */
object BundledWordList {

    @Volatile private var cached: List<String>? = null

    fun words(context: Context): List<String> =
        cached ?: load(context).also { cached = it }

    private fun load(context: Context): List<String> {
        return try {
            context.assets.open("common_words_en.txt")
                .bufferedReader()
                .readLines()
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Pure prefix search over a word list — no Android types. Testable on JVM. */
    fun completionsFor(prefix: String, wordList: List<String>, limit: Int = 10): List<String> {
        if (prefix.length < 2) return emptyList()  // require at least 2 chars
        val lower = prefix.lowercase()
        return wordList
            .filter { it.startsWith(lower) && it != lower }
            .take(limit)
    }
}
