package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.content.res.Resources
import com.moonfin.tvkeyboard.R
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

/**
 * Parses every vendored LeanKeyboard layout and fails on a broken key, popup,
 * icon, or string reference. LeanKeyboard keys are label-first, so a key is
 * valid when it has any of `codes`, `keyLabel`, or `keyIcon`.
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardLayoutParserTest {

    private lateinit var context: Context
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resources = context.resources
    }

    @Test
    fun `every vendored layout parses and has usable keys`() {
        val layouts = R.xml::class.java.fields
            .filter { it.name != "method" }
            .associate { it.name to it.getInt(null) }

        assertTrue("Expected at least 100 vendored layouts", layouts.size >= 100)

        val visited = mutableSetOf<String>()
        for ((name, id) in layouts) {
            validate(name, id, visited)
        }
    }

    @Test
    fun `every language resolves to its own vendored alphabet layout`() {
        val languages = LanguageRegistry.all(context)
        assertTrue("No languages registered", languages.isNotEmpty())

        for (language in languages) {
            val id = resources.getIdentifier(
                language.alphabetResource,
                "xml",
                context.packageName,
            )
            assertNotEquals(
                "Missing vendored layout for ${language.key} (${language.alphabetResource})",
                0,
                id,
            )

            val parser = resources.getXml(id)
            var keys = 0
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "Key") keys++
                event = parser.next()
            }
            assertTrue("${language.key} resolved to an empty layout", keys > 0)
        }
    }

    @Test
    fun `missing layout falls back to english instead of crashing`() {
        val id = KeyboardLayouts.resourceId(
            context,
            LanguageLayout("Missing", "xx_missing", azerty = false).alphabetResource,
        )
        assertNotEquals(0, id)
        assertTrue(KeyboardLayouts.resourceId(context, "definitely_not_a_layout") != 0)
    }

    private fun validate(name: String, id: Int, visited: MutableSet<String>) {
        if (!visited.add(name)) return

        val parser = resources.getXml(id)
        var keys = 0
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "Key") {
                keys++
                validateKey(name, parser, visited)
            }
            event = parser.next()
        }

        // These two are intentional placeholders in the vendored data.
        if (name != "empty_kbd" && name != "popup_symbols") {
            assertTrue("$name has no keys", keys > 0)
        }
    }

    private fun validateKey(layout: String, parser: XmlPullParser, visited: MutableSet<String>) {
        val codes = parser.getAttributeValue(ANDROID_NS, "codes")
        val label = parser.getAttributeValue(ANDROID_NS, "keyLabel")
        val icon = parser.getAttributeValue(ANDROID_NS, "keyIcon")

        assertTrue(
            "$layout has a key with no codes, label, or icon",
            !codes.isNullOrBlank() || !label.isNullOrBlank() || !icon.isNullOrBlank(),
        )

        if (label?.startsWith("@string/") == true) {
            assertReference(layout, "string", label.removePrefix("@string/"))
        }
        if (icon?.startsWith("@drawable/") == true) {
            assertReference(layout, "drawable", icon.removePrefix("@drawable/"))
        }

        parser.getAttributeValue(ANDROID_NS, "popupKeyboard")
            ?.takeIf { it.startsWith("@xml/") }
            ?.let { popup ->
                val popupName = popup.removePrefix("@xml/")
                val popupId = resources.getIdentifier(popupName, "xml", context.packageName)
                assertNotEquals("$layout references missing popup $popup", 0, popupId)
                validate(popupName, popupId, visited)
            }
    }

    private fun assertReference(layout: String, type: String, name: String) {
        val id = resources.getIdentifier(name, type, context.packageName)
        assertNotEquals("$layout references missing @$type/$name", 0, id)
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
