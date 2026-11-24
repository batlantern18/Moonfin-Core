package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {

    private val commonDomains = listOf(
        "@gmail.com", "@yahoo.com", "@hotmail.com", "@outlook.com",
        "@icloud.com", "@aol.com", "@protonmail.com", "@zoho.com",
        "@mail.com", "@yandex.com"
    )

    private val provider = CompletionProvider { _ ->
        listOf("completion1", "completion2", "completion3")
    }

    private val engine = SuggestionEngine(provider)

    @Test
    fun `modeFor returns DOMAIN for email address variation`() {
        val mode = engine.modeFor(
            android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        assertEquals(SuggestionMode.DOMAIN, mode)
    }

    @Test
    fun `modeFor returns DOMAIN for web email address variation`() {
        val mode = engine.modeFor(
            android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        )
        assertEquals(SuggestionMode.DOMAIN, mode)
    }

    @Test
    fun `modeFor returns AUTO_COMPLETE when auto complete flag is set`() {
        val mode = engine.modeFor(
            android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        )
        assertEquals(SuggestionMode.AUTO_COMPLETE, mode)
    }

    @Test
    fun `modeFor returns DEFAULT for plain text`() {
        val mode = engine.modeFor(android.text.InputType.TYPE_CLASS_TEXT)
        assertEquals(SuggestionMode.DEFAULT, mode)
    }

    @Test
    fun `domain suggestions return full domain with zero replacePrefix`() {
        val suggestions = engine.suggestions(
            textBeforeCursor = "test",
            mode = SuggestionMode.DOMAIN,
            domains = commonDomains
        )

        assertEquals(10, suggestions.size)
        assertEquals("@gmail.com", suggestions[0].text)
        assertEquals(0, suggestions[0].replacePrefix)
        assertEquals("@yahoo.com", suggestions[1].text)
        assertEquals(0, suggestions[1].replacePrefix)
    }

    @Test
    fun `domain suggestions are limited to MAX_SUGGESTIONS`() {
        val manyDomains = (1..15).map { "@domain$it.com" }
        val suggestions = engine.suggestions(
            textBeforeCursor = "test",
            mode = SuggestionMode.DOMAIN,
            domains = manyDomains
        )
        assertEquals(SuggestionEngine.MAX_SUGGESTIONS, suggestions.size)
    }

    @Test
    fun `auto complete suggestions replace the typed prefix`() {
        val suggestions = engine.suggestions(
            textBeforeCursor = "hello com",
            mode = SuggestionMode.AUTO_COMPLETE
        )
        assertEquals(3, suggestions.size)
        assertEquals("completion1", suggestions[0].text)
        assertEquals(3, suggestions[0].replacePrefix) // "com".length
        assertEquals("completion2", suggestions[1].text)
        assertEquals(3, suggestions[1].replacePrefix)
    }

    @Test
    fun `auto complete filters by prefix case-insensitively`() {
        val suggestions = engine.suggestions(
            textBeforeCursor = "COMP",
            mode = SuggestionMode.AUTO_COMPLETE
        )

        assertEquals(3, suggestions.size)
        suggestions.forEach { assertTrue(it.text.startsWith("completion", ignoreCase = true)) }
    }

    @Test
    fun `auto complete returns empty when no prefix match`() {
        val suggestions = engine.suggestions(
            textBeforeCursor = "xyz",
            mode = SuggestionMode.AUTO_COMPLETE
        )
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `default mode behaves like auto complete`() {
        val suggestions = engine.suggestions(
            textBeforeCursor = "com",
            mode = SuggestionMode.DEFAULT
        )
        assertEquals(3, suggestions.size)
        suggestions.forEach { assertTrue(it.text.startsWith("completion", ignoreCase = true)) }
        assertEquals(3, suggestions[0].replacePrefix)
    }

    @Test
    fun `wordPrefix extracts the word before cursor`() {
        val result1 = engine.wordPrefix("hello world")
        val result2 = engine.wordPrefix("hello world ")
        val result3 = engine.wordPrefix("test")
        val result4 = engine.wordPrefix("")
        val result5 = engine.wordPrefix("a".repeat(70) + " word")
        println("result1='$result1' result2='$result2' result3='$result3' result4='$result4' result5='$result5'")
        assertEquals("world", result1)
        assertEquals("world", result2)
        assertEquals("test", result3)
        assertEquals("", result4)
        assertEquals("word", result5)
    }

    @Test
    fun `wordPrefix handles only the last MAX_PREFIX_LOOKBEHIND characters`() {
        val longText = "a".repeat(100) + " finalword"
        assertEquals("finalword", engine.wordPrefix(longText))
    }

    @Test
    fun `domain suggestions appear with full email addresses`() {
        val savedEmails = listOf("user@gmail.com", "test@yahoo.com")
        val suggestions = engine.suggestions(
            textBeforeCursor = "",
            mode = SuggestionMode.DOMAIN,
            domains = savedEmails + commonDomains, // Saved emails first
        )

        // Saved emails should appear first
        assertEquals("user@gmail.com", suggestions[0].text)
        assertEquals("test@yahoo.com", suggestions[1].text)
        // Then common domains
        assertEquals("@gmail.com", suggestions[2].text)
        assertEquals(0, suggestions[0].replacePrefix)
        assertEquals(0, suggestions[2].replacePrefix)
    }

    @Test
    fun `domain suggestions replace typed prefix when textBeforeCursor is set`() {
        // User has typed "user@" and sees saved email suggestion
        val savedEmails = listOf("user@gmail.com", "test@yahoo.com")
        val suggestions = engine.suggestions(
            textBeforeCursor = "user@",
            mode = SuggestionMode.DOMAIN,
            domains = savedEmails + commonDomains,
        )

        // Saved email should appear first with replacePrefix = length of "user@"
        assertEquals("user@gmail.com", suggestions[0].text)
        assertEquals(5, suggestions[0].replacePrefix) // "user@".length = 5
        assertEquals("test@yahoo.com", suggestions[1].text)
        assertEquals("@gmail.com", suggestions[2].text)
    }

    @Test
    fun `domain suggestions with partial typed text`() {
        // User has typed "user@gmai" (partial match for "user@gmail.com")
        val savedEmails = listOf("user@gmail.com")
        val suggestions = engine.suggestions(
            textBeforeCursor = "user@gmai",
            mode = SuggestionMode.DOMAIN,
            domains = savedEmails,
        )

        // Suggestion should replace the typed "user@gmai" with full email
        assertEquals("user@gmail.com", suggestions[0].text)
        assertEquals(9, suggestions[0].replacePrefix) // "user@gmai".length = 9
    }

    @Test
    fun `domain chips append to a typed local part`() {
        val suggestions = engine.suggestions(
            textBeforeCursor = "test",
            mode = SuggestionMode.DOMAIN,
            domains = listOf("@gmail.com"),
        )

        assertEquals("@gmail.com", suggestions[0].text)
        assertEquals(0, suggestions[0].replacePrefix)
    }

    @Test
    fun `domain suggestions are limited and deduped`() {
        val manyDomains = listOf("email1@test.com", "email2@test.com", "email3@test.com") + commonDomains
        val suggestions = engine.suggestions(
            textBeforeCursor = "",
            mode = SuggestionMode.DOMAIN,
            domains = manyDomains,
            limit = 5,
        )

        assertEquals(5, suggestions.size)
        // First 5 should be from the combined list
        assertTrue(suggestions.map { it.text }.contains("email1@test.com"))
        assertTrue(suggestions.map { it.text }.contains("@gmail.com"))
    }
}