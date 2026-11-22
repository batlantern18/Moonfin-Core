package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopBarContentTest {

    private val words = listOf(Suggestion("world", 2), Suggestion("word", 2))

    @Test
    fun `word suggestions fill the strip for ordinary text fields`() {
        val content = TopBarContent.of(FieldMode.TEXT, suggestions = words)

        assertEquals(TopBarKind.SUGGESTIONS, content.kind)
        assertEquals(words, content.items)
        assertTrue(content.visible)
    }

    @Test
    fun `email fields show domain chips`() {
        val domains = listOf(Suggestion("@gmail.com", 0))

        val content = TopBarContent.of(FieldMode.EMAIL, suggestions = domains)

        assertEquals(TopBarKind.DOMAINS, content.kind)
        assertEquals(domains, content.items)
    }

    @Test
    fun `password fields show digits and symbols`() {
        val content = TopBarContent.of(FieldMode.PASSWORD)

        assertEquals(TopBarKind.DIGITS, content.kind)
        assertTrue(content.items.any { it.text == "1" })
        assertTrue(content.items.any { it.text == "@" })
    }

    @Test
    fun `voice replaces whatever the field would otherwise show`() {
        val content = TopBarContent.of(
            fieldMode = FieldMode.PASSWORD,
            voiceState = VoiceState.LISTENING,
            suggestions = words,
        )

        assertEquals(TopBarKind.VOICE, content.kind)
        assertTrue(content.items.isEmpty())
        assertTrue(content.visible)
    }

    @Test
    fun `a voice error keeps its message`() {
        val content = TopBarContent.of(
            fieldMode = FieldMode.TEXT,
            voiceState = VoiceState.ERROR,
            voiceError = "No microphone",
        )

        assertEquals(TopBarKind.VOICE, content.kind)
        assertEquals("No microphone", content.voiceError)
    }

    @Test
    fun `nothing to show collapses the strip`() {
        assertFalse(TopBarContent.of(FieldMode.TEXT).visible)
        assertFalse(TopBarContent.EMPTY.visible)
        assertEquals(TopBarKind.EMPTY, TopBarContent.of(FieldMode.TEXT).kind)
    }

    @Test
    fun `disabled suggestions leave the strip empty`() {
        val content = TopBarContent.of(
            fieldMode = FieldMode.TEXT,
            suggestions = words,
            suggestionsEnabled = false,
        )

        assertEquals(TopBarKind.EMPTY, content.kind)
    }

    @Test
    fun `numeric fields keep the strip empty because the keys already are digits`() {
        assertEquals(TopBarKind.EMPTY, TopBarContent.of(FieldMode.NUMERIC).kind)
    }
}
