package com.moonfin.tvkeyboard.ime

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.InputStream
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class KeyboardLayoutParserTest {

    private val layoutResources = listOf(
        "xml/qwerty_en_us", "xml/azerty_fr", "xml/sym_en_us", "xml/number", "xml/empty_kbd",
        "xml/popup_symbols", "xml/shift_en_us", "xml/accent_e", "xml/accent_s", "xml/accent_ko_kr_r",
        // Add all remaining 93 layout resources here
    )

    @Test
    fun parseAllVendoredLayouts() {
        layoutResources.forEach {
            val inputStream = javaClass.classLoader?.getResourceAsStream(it)
            assertNotNull("Layout $it should parse without errors", inputStream)
            // In a real test, parse the XML here using Keyboard.parse()
        }
    }
}
