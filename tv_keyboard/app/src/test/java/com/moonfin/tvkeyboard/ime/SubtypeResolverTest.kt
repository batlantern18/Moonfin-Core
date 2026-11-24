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
    fun `locale tag carries the region for every reference variant`() {
        val regional = listOf(
            "en_au" to "en_AU", "en_ca" to "en_CA", "en_gb" to "en_GB",
            "fr_be" to "fr_BE", "fr_ca" to "fr_CA", "fr_ch" to "fr_CH",
            "de_at" to "de_AT", "de_be" to "de_BE", "de_ch" to "de_CH",
            "es_mx" to "es_MX", "es_es" to "es_ES",
            "pt_br" to "pt_BR", "pt_pt" to "pt_PT",
            "nl_be" to "nl_BE", "it_ch" to "it_CH", "ja_jp" to "ja_JP",
            "zh_hk" to "zh_HK", "zh_cn" to "zh_CN", "zh_tw" to "zh_TW",
        )
        regional.forEach { (code, tag) ->
            assertEquals(code, tag, SubtypeResolver.localeTag(LanguageLayout(code, code, false)))
        }
    }

    @Test
    fun `extra value carries the Moonfin marker and board variant`() {
        assertEquals("moonfin,AsciiCapable", SubtypeResolver.extraValue(english))
        assertEquals("moonfin,AsciiCapable,layout=azerty", SubtypeResolver.extraValue(englishAzerty))
        val transliteration = LanguageLayout("Hindi (transliteration)", "hi", false, layoutVariant = "trans")
        assertEquals("moonfin,AsciiCapable,layout=trans", SubtypeResolver.extraValue(transliteration))
        assertEquals("hi_trans", transliteration.key)
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
    fun `resolve prefers an exact regional match over another variant`() {
        val uk = LanguageLayout("English (UK)", "en_gb", false)
        val au = LanguageLayout("English (Australia)", "en_au", false)
        val us = LanguageLayout("English", "en_us", false)
        val allVariants = listOf(us, au, uk)
        assertEquals(uk, SubtypeResolver.resolve(allVariants, "en_GB"))
        assertEquals(au, SubtypeResolver.resolve(allVariants, "en-AU"))
        assertEquals(us, SubtypeResolver.resolve(allVariants, "en_US"))
        assertNull(
            "An unregistered region must not pick a different regional board",
            SubtypeResolver.resolve(allVariants, "en_NZ"),
        )
    }

    @Test
    fun `resolve selects a layout variant carried in the extra value`() {
        val hindi = LanguageLayout("हिन्दी", "hi", false)
        val transliteration = LanguageLayout("Hindi (transliteration)", "hi", false, layoutVariant = "trans")
        val allVariants = listOf(hindi, transliteration)
        assertEquals(hindi, SubtypeResolver.resolve(allVariants, "hi"))
        assertEquals(
            transliteration,
            SubtypeResolver.resolve(allVariants, "hi", SubtypeResolver.extraValue(transliteration)),
        )
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
