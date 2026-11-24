package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.view.inputmethod.InputMethodSubtype
import com.moonfin.tvkeyboard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.w3c.dom.Element
import org.xmlpull.v1.XmlPullParser
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Acceptance for issue #57: the IME advertises locale-bearing subtypes, every
 * enabled language can be built into one, and Moonfin handles the real subtype
 * change callback instead of inventing its own API.
 */
@RunWith(RobolectricTestRunner::class)
class ImeSubtypeDeclarationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `service meta-data points at the input method xml resource`() {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val document = factory.newDocumentBuilder().parse(manifestFile())
        val services = document.getElementsByTagName("service")
        var found = false
        for (i in 0 until services.length) {
            val service = services.item(i) as Element
            if (service.getAttributeNS(ANDROID_NS, "name") != ".ime.TvKeyboardService") continue
            found = true
            val metadata = service.getElementsByTagName("meta-data").item(0) as Element
            assertEquals("android.view.im", metadata.getAttributeNS(ANDROID_NS, "name"))
            assertEquals("@xml/method", metadata.getAttributeNS(ANDROID_NS, "resource"))
        }
        assertTrue("TvKeyboardService is not declared", found)
    }

    @Test
    fun `input method xml declares a keyboard subtype with locale and label`() {
        val parser = context.resources.getXml(R.xml.method)
        var subtypes = 0
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "subtype") {
                subtypes++
                val locale = parser.getAttributeValue(ANDROID_NS, "imeSubtypeLocale")
                val label = parser.getAttributeValue(ANDROID_NS, "label")
                assertTrue("subtype needs a locale", !locale.isNullOrBlank())
                assertTrue("subtype needs a string label", label?.startsWith("@string/") == true)
                assertEquals("keyboard", parser.getAttributeValue(ANDROID_NS, "imeSubtypeMode"))
                assertEquals("true", parser.getAttributeValue(ANDROID_NS, "isAsciiCapable"))
            }
            event = parser.next()
        }
        assertTrue("No IME subtype declared", subtypes > 0)
    }

    @Test
    fun `every registered language has a subtype label`() {
        val languages = LanguageRegistry.all(context)
        assertTrue("No languages registered", languages.isNotEmpty())
        for (language in languages) {
            val name = SubtypeResolver.labelResourceName(language)
            val id = context.resources.getIdentifier(name, "string", context.packageName)
            assertNotEquals("Missing subtype label $name", 0, id)
            assertTrue("Empty subtype label $name", context.resources.getString(id).isNotBlank())
        }
    }

    @Test
    fun `factory declares one subtype per enabled language`() {
        val enabledKeys = setOf("en_us", "en_us_azerty", "fr", "ko_kr")
        val languages = LanguageRegistry.all(context).filter { it.key in enabledKeys }
        assertEquals(enabledKeys.size, languages.size)

        val subtypes = SubtypeFactory.build(context, languages)
        assertEquals(languages.size, subtypes.size)
        for ((language, subtype) in languages.zip(subtypes)) {
            assertEquals(SubtypeResolver.localeTag(language), subtype.locale)
            assertEquals(SubtypeFactory.KEYBOARD_MODE, subtype.mode)
            assertTrue("${language.key} must be ASCII capable", subtype.isAsciiCapable)
            assertNotEquals("${language.key} has no label", 0, subtype.nameResId)
            assertEquals(
                "layout variant must reach the IME",
                SubtypeResolver.extraValue(language),
                subtype.extraValue,
            )
        }
    }

    @Test
    fun `every built subtype resolves back to its own layout`() {
        val all = LanguageRegistry.all(context)
        val subtypes = SubtypeFactory.build(context, all)
        for ((language, subtype) in all.zip(subtypes)) {
            val resolved = SubtypeResolver.resolve(all, subtype.locale, subtype.extraValue)
            assertEquals("${language.key} cannot be selected from its subtype", language.key, resolved?.key)
        }
    }

    @Test
    fun `factory reflects the currently enabled language set`() {
        val all = LanguageRegistry.all(context)
        val before = SubtypeFactory.build(context, all)
        assertTrue(before.map { it.locale }.contains("ru"))

        val after = SubtypeFactory.build(context, all.filter { it.key != "ru" })
        assertNotEquals(before.size, after.size)
        assertTrue("Disabled language still has a subtype", after.none { it.locale == "ru" })
    }

    @Test
    fun `service overrides the real subtype change callback`() {
        val callback = TvKeyboardService::class.java.getDeclaredMethod(
            "onCurrentInputMethodSubtypeChanged",
            InputMethodSubtype::class.java,
        )
        assertTrue(
            "onCurrentInputMethodSubtypeChanged must stay a framework callback",
            java.lang.reflect.Modifier.isProtected(callback.modifiers),
        )
    }

    private fun manifestFile(): File {
        val candidates = listOf(
            "src/main/AndroidManifest.xml",
            "app/src/main/AndroidManifest.xml",
            "tv_keyboard/app/src/main/AndroidManifest.xml",
        )
        return candidates.map(::File).firstOrNull { it.isFile }
            ?: throw AssertionError("AndroidManifest.xml not found under $candidates")
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
