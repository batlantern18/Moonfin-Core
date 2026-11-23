package com.moonfin.tvkeyboard.ime

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Address fields get the cue strip the Moonfin spec describes. */
class UrlFieldTest {

    @Test
    fun `uri variation is its own field mode`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI

        assertEquals(FieldMode.URL, FieldMode.from(inputType))
        assertTrue(FieldMode.URL.allowsVoice)
    }

    @Test
    fun `url fields offer insertion cues instead of words`() {
        val content = TopBarContent.of(
            fieldMode = FieldMode.URL,
            suggestions = listOf(Suggestion("moon", 4)),
        )

        assertEquals(TopBarKind.CUES, content.kind)
        assertTrue(content.visible)
        assertTrue(content.items.any { it.text == "https://" })
        assertTrue(content.items.any { it.text == ":8096" })
        assertTrue(content.items.all { it.replacePrefix == 0 })
    }

    @Test
    fun `password fields still prefer digits over cues`() {
        assertEquals(TopBarKind.DIGITS, TopBarContent.of(FieldMode.PASSWORD).kind)
    }
}
