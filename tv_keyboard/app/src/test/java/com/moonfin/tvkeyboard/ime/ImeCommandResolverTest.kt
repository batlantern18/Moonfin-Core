package com.moonfin.tvkeyboard.ime

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ImeCommandResolverTest {

    private val locale = Locale.US

    @Test
    fun `letter key inserts its label`() {
        assertEquals(
            ImeCommand.InsertText("a"),
            ImeCommandResolver.resolve('a'.code, "a", shifted = false, locale = locale),
        )
    }

    @Test
    fun `shifted letter key inserts the uppercase letter`() {
        assertEquals(
            ImeCommand.InsertText("A"),
            ImeCommandResolver.resolve('a'.code, "a", shifted = true, locale = locale),
        )
    }

    @Test
    fun `space key inserts a single space`() {
        assertEquals(
            ImeCommand.InsertText(" "),
            ImeCommandResolver.resolve(KeyCodes.SPACE, "Space", shifted = false, locale = locale),
        )
    }

    @Test
    fun `delete key deletes before the cursor`() {
        assertEquals(
            ImeCommand.DeleteBeforeCursor,
            ImeCommandResolver.resolve(KeyCodes.DELETE, "Delete", shifted = false, locale = locale),
        )
    }

    @Test
    fun `cursor keys move the selection`() {
        assertEquals(
            ImeCommand.MoveLeft,
            ImeCommandResolver.resolve(KeyCodes.LEFT, "Left", shifted = false, locale = locale),
        )
        assertEquals(
            ImeCommand.MoveRight,
            ImeCommandResolver.resolve(KeyCodes.RIGHT, "Right", shifted = false, locale = locale),
        )
    }

    @Test
    fun `shift key toggles shift instead of inserting text`() {
        assertEquals(
            ImeCommand.ToggleShift,
            ImeCommandResolver.resolve(KeyCodes.SHIFT, "Shift", shifted = false, locale = locale),
        )
    }

    @Test
    fun `mode change key toggles mode`() {
        assertEquals(
            ImeCommand.ToggleMode,
            ImeCommandResolver.resolve(KeyCodes.MODE_CHANGE, "Mode change", shifted = false, locale = locale),
        )
    }

    @Test
    fun `unsupported keys are a no-op for the tracer bullet`() {
        assertEquals(
            ImeCommand.None,
            ImeCommandResolver.resolve(KeyCodes.LANGUAGE, "Language", shifted = false, locale = locale),
        )
    }

    @Test
    fun `voice key starts voice input`() {
        assertEquals(
            ImeCommand.VoiceInput,
            ImeCommandResolver.resolve(KeyCodes.VOICE, "Voice", shifted = false, locale = locale),
        )
    }

    @Test
    fun `clipboard key opens clipboard overlay`() {
        assertEquals(
            ImeCommand.ClipboardOverlay,
            ImeCommandResolver.resolve(KeyCodes.CLIPBOARD, "Clipboard", shifted = false, locale = locale),
        )
    }
}
