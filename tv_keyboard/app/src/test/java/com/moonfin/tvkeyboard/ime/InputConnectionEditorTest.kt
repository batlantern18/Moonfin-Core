package com.moonfin.tvkeyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

/** Key events need the framework's real classes, so this class runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
class InputConnectionEditorTest {

    private lateinit var connection: InputConnection
    private lateinit var editor: InputConnectionEditor
    private var autoSpace = false
    private var changes = 0

    @Before
    fun setUp() {
        connection = mock(InputConnection::class.java)
        autoSpace = false
        changes = 0
        editor = InputConnectionEditor(
            connection = { connection },
            autoSpace = { autoSpace },
            onChanged = { changes++ },
        )
    }

    @Test
    fun `insert commits the text and reports one change`() {
        editor.insert("a")

        verify(connection).commitText("a", 1)
        assertEquals(1, changes)
    }

    @Test
    fun `insert never adds a space even when auto-space is on`() {
        autoSpace = true

        editor.insert("a")

        verify(connection, never()).commitText(" ", 1)
    }

    @Test
    fun `edit without a connection is a no-op and reports nothing`() {
        val disconnected = InputConnectionEditor(
            connection = { null },
            autoSpace = { true },
            onChanged = { changes++ },
        )

        disconnected.insert("a")
        disconnected.deleteBeforeCursor()
        disconnected.moveCursorLeft()
        disconnected.applySuggestion(Suggestion("apple", 2))
        disconnected.paste("clip")
        disconnected.performAction(1)

        assertEquals(0, changes)
        assertEquals("", disconnected.textBeforeCursor(10))
        assertFalse(disconnected.hasSelection())
    }

    @Test
    fun `delete clears the selection instead of the character before the cursor`() {
        `when`(connection.getSelectedText(0)).thenReturn("chosen")

        editor.deleteBeforeCursor()

        verify(connection).commitText("", 1)
        verify(connection, never()).deleteSurroundingText(1, 0)
        assertEquals(1, changes)
    }

    @Test
    fun `delete removes the character before the cursor when there is no selection`() {
        `when`(connection.getSelectedText(0)).thenReturn(null)
        `when`(connection.deleteSurroundingText(1, 0)).thenReturn(true)

        editor.deleteBeforeCursor()

        verify(connection).deleteSurroundingText(1, 0)
        verify(connection, never()).sendKeyEvent(org.mockito.ArgumentMatchers.any(KeyEvent::class.java))
    }

    @Test
    fun `delete falls back to a key event when the field refuses surrounding deletion`() {
        `when`(connection.getSelectedText(0)).thenReturn(null)
        `when`(connection.deleteSurroundingText(1, 0)).thenReturn(false)

        editor.deleteBeforeCursor()

        verify(connection, times(2)).sendKeyEvent(
            org.mockito.ArgumentMatchers.argThat<KeyEvent> { it.keyCode == KeyEvent.KEYCODE_DEL },
        )
    }

    @Test
    fun `applySuggestion replaces the prefix and appends one space when auto-space is on`() {
        autoSpace = true

        editor.applySuggestion(Suggestion("apple", 2))

        verify(connection).deleteSurroundingText(2, 0)
        verify(connection).commitText("apple", 1)
        verify(connection).commitText(" ", 1)
        assertEquals(1, changes)
    }

    @Test
    fun `applySuggestion appends no space when auto-space is off`() {
        editor.applySuggestion(Suggestion("apple", 2))

        verify(connection).commitText("apple", 1)
        verify(connection, never()).commitText(" ", 1)
    }

    @Test
    fun `applySuggestion with no prefix only commits`() {
        editor.applySuggestion(Suggestion("@gmail.com", 0))

        verify(connection, never()).deleteSurroundingText(0, 0)
        verify(connection).commitText("@gmail.com", 1)
    }

    @Test
    fun `paste appends one space when auto-space is on`() {
        autoSpace = true

        editor.paste("clip")

        verify(connection).commitText("clip", 1)
        verify(connection).commitText(" ", 1)
    }

    @Test
    fun `paste appends no space when auto-space is off`() {
        editor.paste("clip")

        verify(connection).commitText("clip", 1)
        verify(connection, never()).commitText(" ", 1)
    }

    @Test
    fun `dictation commits one whole token plus a space when auto-space is on`() {
        autoSpace = true

        editor.dictate("hello world")

        val ordered = org.mockito.Mockito.inOrder(connection)
        ordered.verify(connection).commitText("hello world", 1)
        ordered.verify(connection).commitText(" ", 1)
        assertEquals(1, changes)
    }

    @Test
    fun `dictation commits no space when auto-space is off`() {
        editor.dictate("hello world")

        verify(connection).commitText("hello world", 1)
        verify(connection, never()).commitText(" ", 1)
    }

    @Test
    fun `dictation finishes the composed word before committing the result`() {
        editor.compose("hel")

        editor.dictate("hello world")

        val ordered = org.mockito.Mockito.inOrder(connection)
        ordered.verify(connection).finishComposingText()
        ordered.verify(connection).commitText("hello world", 1)
        assertEquals("", editor.composition())
    }

    @Test
    fun `performAction forwards the action id`() {
        editor.performAction(3)

        verify(connection).performEditorAction(3)
        assertEquals(1, changes)
    }

    @Test
    fun `cursor moves send a down and an up key event`() {
        editor.moveCursorLeft()

        verify(connection).sendKeyEvent(
            org.mockito.ArgumentMatchers.argThat<KeyEvent> {
                it.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && it.action == KeyEvent.ACTION_DOWN
            },
        )
        verify(connection).sendKeyEvent(
            org.mockito.ArgumentMatchers.argThat<KeyEvent> {
                it.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && it.action == KeyEvent.ACTION_UP
            },
        )
    }

    @Test
    fun `typing composes instead of committing`() {
        editor.compose("a")

        verify(connection).setComposingText("a", 1)
        verify(connection, never()).commitText("a", 1)
        assertEquals("a", editor.composition())
    }

    @Test
    fun `each letter extends the same composition`() {
        editor.compose("a")
        editor.compose("b")

        verify(connection).setComposingText("ab", 1)
    }

    @Test
    fun `delete inside a word shortens the composition instead of the field`() {
        editor.compose("ab")

        editor.deleteBeforeCursor()

        verify(connection).setComposingText("a", 1)
        verify(connection, never()).deleteSurroundingText(1, 0)
        assertEquals("a", editor.composition())
    }

    @Test
    fun `deleting the last composed character ends the composition`() {
        editor.compose("a")

        editor.deleteBeforeCursor()

        verify(connection).setComposingText("", 1)
        verify(connection).finishComposingText()
        assertEquals("", editor.composition())
    }

    @Test
    fun `a suggestion replaces the composed word and finishes it`() {
        autoSpace = true
        editor.compose("ap")
        editor.compose("p")

        editor.applySuggestion(Suggestion("apple", 3))

        verify(connection).setComposingText("apple", 1)
        verify(connection).finishComposingText()
        verify(connection).commitText(" ", 1)
        verify(connection, never()).deleteSurroundingText(3, 0)
        assertEquals("", editor.composition())
    }

    @Test
    fun `a domain chip commits directly without a trailing space`() {
        autoSpace = true

        editor.applySuggestion(Suggestion("@gmail.com", 0))

        verify(connection).commitText("@gmail.com", 1)
        verify(connection, never()).commitText(" ", 1)
    }

    @Test
    fun `a space finishes the composed word before it is inserted`() {
        editor.compose("hi")

        editor.insert(" ")

        verify(connection).finishComposingText()
        verify(connection).commitText(" ", 1)
        assertEquals("", editor.composition())
    }

    @Test
    fun `a new connection does not inherit the previous composition`() {
        editor.compose("ab")
        val next = mock(InputConnection::class.java)
        val switched = InputConnectionEditor(
            connection = { next },
            autoSpace = { false },
            onChanged = { changes++ },
        )

        switched.compose("c")

        verify(next).setComposingText("c", 1)
        assertEquals("c", switched.composition())
    }

    @Test
    fun `textBeforeCursor and hasSelection read through the same seam`() {
        `when`(connection.getTextBeforeCursor(60, 0)).thenReturn("hello")

        assertEquals("hello", editor.textBeforeCursor(60))
        assertFalse(editor.hasSelection())

        `when`(connection.getSelectedText(0)).thenReturn("x")
        assertTrue(editor.hasSelection())
    }
}
