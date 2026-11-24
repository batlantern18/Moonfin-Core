package com.moonfin.tvkeyboard.ime

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Acceptance for issue #58: every locale in the product reference's
 * "Language and internationalization support" table is a real, selectable
 * subtype with its own label and board, and no regional selection collapses
 * onto another language or onto the English default.
 */
@RunWith(RobolectricTestRunner::class)
class ReferenceLanguageVariantsTest {

    private lateinit var context: Context
    private lateinit var all: List<LanguageLayout>

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        all = LanguageRegistry.all(context)
    }

    /** Codes from the reference table plus the two Portuguese variants. */
    private val expectedCodes = listOf(
        // English
        "en_us", "en_au", "en_ca", "en_gb",
        // Western Europe
        "nl", "nl_be", "fr", "fr_be", "fr_ca", "fr_ch",
        "de", "de_at", "de_be", "de_ch", "ga", "it", "it_ch",
        "pt", "pt_br", "pt_pt", "es_us", "es_mx", "es_es",
        // Northern Europe
        "da", "et", "fi", "lt", "nb", "sv",
        // Central and Eastern Europe
        "sq", "bg", "hr", "cs", "el", "hu", "mk", "pl", "ro", "ru",
        "sr", "sk", "sl", "uk",
        // Middle East and South Asia
        "ar", "bn", "fa", "he", "hi", "pa", "tr", "ur",
        // East and Southeast Asia
        "my", "zh_hk", "zh_cn", "zh_tw", "fil", "id", "ja_jp",
        "ko_kr", "ms", "th", "vi",
        // Africa and Central Asia
        "af", "uz",
    )

    @Test
    fun `every reference locale is registered`() {
        val codes = all.map { it.code }.toSet()
        expectedCodes.forEach { code ->
            assertTrue("Missing reference locale $code", code in codes)
        }
        assertEquals("Two layouts share a preference key", all.size, all.map { it.key }.toSet().size)
    }

    @Test
    fun `named regional variants have their own locale and board`() {
        val named = mapOf(
            "en_au" to "en_AU", "en_ca" to "en_CA", "en_gb" to "en_GB",
            "fr_be" to "fr_BE", "fr_ca" to "fr_CA", "fr_ch" to "fr_CH",
            "de_at" to "de_AT", "de_be" to "de_BE", "de_ch" to "de_CH",
            "es_mx" to "es_MX", "pt_br" to "pt_BR", "pt_pt" to "pt_PT",
        )
        val layouts = mapOf(
            "en_au" to "qwerty_en_au", "en_ca" to "qwerty_en_ca", "en_gb" to "qwerty_en_gb",
            "fr_be" to "qwerty_fr_be", "fr_ca" to "qwerty_fr_ca", "fr_ch" to "qwerty_fr_ch",
            "de_at" to "qwerty_de_at", "de_be" to "qwerty_de_be", "de_ch" to "qwerty_de_ch",
            "es_mx" to "qwerty_es_mx", "pt_br" to "qwerty_pt_br", "pt_pt" to "qwerty_pt_pt",
        )
        for ((code, locale) in named) {
            val language = all.single { it.code == code && !it.azerty && it.layoutVariant == null }
            assertEquals("$code has the wrong IME locale", locale, SubtypeResolver.localeTag(language))
            assertEquals("$code has the wrong board", layouts[code], language.alphabetResource)
            assertNotEquals(
                "$code has no vendored board",
                0,
                context.resources.getIdentifier(language.alphabetResource, "xml", context.packageName),
            )
        }
    }

    @Test
    fun `every language advertises a labelled subtype that resolves back to itself`() {
        val subtypes = SubtypeFactory.build(context, all)
        assertEquals(all.size, subtypes.size)
        for ((language, subtype) in all.zip(subtypes)) {
            assertEquals("${language.key} has the wrong locale", SubtypeResolver.localeTag(language), subtype.locale)
            assertNotEquals("${language.key} has no label resource", 0, subtype.nameResId)
            assertTrue(
                "${language.key} has a blank label",
                context.resources.getString(subtype.nameResId).isNotBlank(),
            )
            val resolved = SubtypeResolver.resolve(all, subtype.locale, subtype.extraValue)
            assertNotNull("${language.key} cannot be selected from its subtype", resolved)
            assertEquals(language.key, resolved!!.key)
        }
    }

    @Test
    fun `every language board is a real vendored resource`() {
        for (language in all) {
            val id = context.resources.getIdentifier(language.alphabetResource, "xml", context.packageName)
            assertNotEquals("${language.key} points at a missing board", 0, id)
        }
    }

    @Test
    fun `an unregistered region never collapses onto another regional board`() {
        assertNull("Unknown English region fell back to the default", SubtypeResolver.resolve(all, "en_NZ"))
        assertNull("Unknown Spanish region fell back to Spanish (US)", SubtypeResolver.resolve(all, "es_AR"))
        assertEquals("fr", SubtypeResolver.resolve(all, "fr_LU")?.key)
    }

    @Test
    fun `Hindi transliteration is a distinct selectable subtype`() {
        val hindi = all.single { it.key == "hi" }
        val transliteration = all.single { it.key == "hi_trans" }
        assertEquals("hi", SubtypeResolver.localeTag(hindi))
        assertEquals("hi", SubtypeResolver.localeTag(transliteration))
        assertNotEquals(SubtypeResolver.extraValue(hindi), SubtypeResolver.extraValue(transliteration))
        assertEquals(
            "hi",
            SubtypeResolver.resolve(all, "hi", SubtypeResolver.extraValue(hindi))?.key,
        )
        assertEquals(
            "hi_trans",
            SubtypeResolver.resolve(all, "hi", SubtypeResolver.extraValue(transliteration))?.key,
        )
    }
}
