package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locale-to-layout resolution is pure so JVM tests can state what a system
 * subtype selection must do without an IME or an Android runtime.
 */
class SubtypeResolverTest {

    private val english = LanguageLayout("English", "en_us", azerty = false)
    private val englishAzerty = LanguageLayout("English (AZERTY)", "en_us", azerty = true)
    private val french = LanguageLayout("Français", "fr", azerty = false)
    private val frenchAzerty = LanguageLayout("Français (AZERTY)", "fr", azerty = true)
    private val korean = LanguageLayout("한국어", "ko_kr", azerty = false)
    private val all = listOf(english, englishAzerty, french, frenchAzerty, korean)

    @Test
    fun `locale tag uses the Android locale form`() {
        assertEquals("en_US", SubtypeResolver.localeTag(english))
        assertEquals("es_US", SubtypeResolver.localeTag(LanguageLayout("Español", "es_us", false)))
        assertEquals("ko_KR", SubtypeResolver.localeTag(korean))
        assertEquals("fr", SubtypeResolver.localeTag(french))
    }

    @Test
    fun `extra value carries the Moonfin marker and board variant`() {
        assertEquals("moonfin,AsciiCapable", SubtypeResolver.extraValue(english))
        assertEquals("moonfin,AsciiCapable,layout=azerty", SubtypeResolver.extraValue(englishAzerty))
    }

    @Test
    fun `label resource name is stable for every layout key`() {
        assertEquals("subtype_en_us", SubtypeResolver.labelResourceName(english))
        assertEquals("subtype_en_us_azerty", SubtypeResolver.labelResourceName(englishAzerty))
        assertEquals("subtype_ko_kr", SubtypeResolver.labelResourceName(korean))
    }

    @Test
    fun `resolve finds the layout named by the subtype locale`() {
        assertEquals(english, SubtypeResolver.resolve(all, "en_US"))
        assertEquals(korean, SubtypeResolver.resolve(all, "ko-KR"))
        assertEquals(
            frenchAzerty,
            SubtypeResolver.resolve(all, "fr", "moonfin,AsciiCapable,layout=azerty"),
        )
    }

    @Test
    fun `resolve picks the azerty board when the subtype asks for it`() {
        assertEquals(
            englishAzerty,
            SubtypeResolver.resolve(all, "en_us", "AsciiCapable,moonfin,layout=azerty"),
        )
        assertEquals(
            frenchAzerty,
            SubtypeResolver.resolve(all, "fr", SubtypeResolver.extraValue(frenchAzerty)),
        )
    }

    @Test
    fun `resolve defaults to the plain board for the same locale`() {
        assertEquals(english, SubtypeResolver.resolve(all, "en_US", "moonfin,AsciiCapable"))
        assertEquals(french, SubtypeResolver.resolve(all, "fr"))
    }

    @Test
    fun `resolve falls back from a regional locale to the registered language`() {
        assertEquals(french, SubtypeResolver.resolve(listOf(french), "fr_CA"))
        assertEquals(english, SubtypeResolver.resolve(listOf(english), "EN"))
    }

    @Test
    fun `resolve ignores unknown locales unrelated extras and blank input`() {
        assertNull(SubtypeResolver.resolve(all, "xx_XX"))
        assertNull(SubtypeResolver.resolve(all, ""))
        assertNull(SubtypeResolver.resolve(all, null))
        assertEquals(
            english,
            SubtypeResolver.resolve(all, "en_US", "AsciiCapable,moonfin,unknown=value"),
        )
    }
}
