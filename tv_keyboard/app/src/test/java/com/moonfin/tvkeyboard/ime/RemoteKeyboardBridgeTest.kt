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
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteKeyboardBridgeTest {

    private lateinit var editor: TextEditor
    private var enabled = false
    private var actionId = 0
    private lateinit var fakeTransport: FakeRemoteKeyboardTransport
    private lateinit var bridge: RemoteKeyboardBridge

    class FakeRemoteKeyboardTransport : RemoteKeyboardTransport {
        var listener: RemoteKeyboardListener? = null
            private set

        override fun setListener(listener: RemoteKeyboardListener?) {
            this.listener = listener
        }

        fun emitKeyDown(keyCode: Int, event: KeyEvent? = null): Boolean =
            listener?.onKeyDown(keyCode, event) ?: false

        fun emitCommitText(text: String): Boolean =
            listener?.onCommitText(text) ?: false
    }

    @Before
    fun setUp() {
        editor = mock(TextEditor::class.java)
        enabled = true
        actionId = 6 // EditorInfo.IME_ACTION_DONE
        fakeTransport = FakeRemoteKeyboardTransport()
        bridge = RemoteKeyboardBridge(
            editor = { editor },
            isEnabled = { enabled },
            actionId = { actionId },
            transport = fakeTransport,
        )
    }

    @Test
    fun `keystrokes appear in focused field through Text editor - backspace`() {
        val handled = fakeTransport.emitKeyDown(KeyEvent.KEYCODE_DEL)

        assertTrue(handled)
        verify(editor).deleteBeforeCursor()
    }

    @Test
    fun `keystrokes appear in focused field through Text editor - cursor left`() {
        val handled = fakeTransport.emitKeyDown(KeyEvent.KEYCODE_DPAD_LEFT)

        assertTrue(handled)
        verify(editor).moveCursorLeft()
    }

    @Test
    fun `keystrokes appear in focused field through Text editor - cursor right`() {
        val handled = fakeTransport.emitKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT)

        assertTrue(handled)
        verify(editor).moveCursorRight()
    }

    @Test
    fun `keystrokes appear in focused field through Text editor - space`() {
        val handled = fakeTransport.emitKeyDown(KeyEvent.KEYCODE_SPACE)

        assertTrue(handled)
        verify(editor).insert(" ")
    }

    @Test
    fun `keystrokes appear in focused field through Text editor - enter action`() {
        val handled = fakeTransport.emitKeyDown(KeyEvent.KEYCODE_ENTER)

        assertTrue(handled)
        verify(editor).performAction(6)
    }

    @Test
    fun `keystrokes appear in focused field through Text editor - printable character key`() {
        val event = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, 0)
        val handled = fakeTransport.emitKeyDown(KeyEvent.KEYCODE_A, event)

        assertTrue(handled)
        verify(editor).insert("a")
    }

    @Test
    fun `keystrokes appear in focused field through Text editor - shifted uppercase character`() {
        val event = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_B, 0, KeyEvent.META_SHIFT_ON)
        val handled = fakeTransport.emitKeyDown(KeyEvent.KEYCODE_B, event)

        assertTrue(handled)
        verify(editor).insert("B")
    }

    @Test
    fun `committed strings arrive as one Edit transaction with one render`() {
        var renderCount = 0
        val mockConnection = mock(InputConnection::class.java)
        val realEditor = InputConnectionEditor(
            connection = { mockConnection },
            autoSpace = { true },
            onChanged = { renderCount++ },
        )

        val bridgeWithRealEditor = RemoteKeyboardBridge(
            editor = { realEditor },
            isEnabled = { true },
            actionId = { 0 },
        )

        val handled = bridgeWithRealEditor.onCommitText("Moonfin TV")

        assertTrue(handled)
        verify(mockConnection).commitText("Moonfin TV", 1)
        assertEquals("Committed string must trigger exactly one render transaction", 1, renderCount)
    }

    @Test
    fun `disabling the switch stops delivery of keystrokes`() {
        enabled = false

        val handled = fakeTransport.emitKeyDown(KeyEvent.KEYCODE_A)

        assertFalse(handled)
        verifyNoInteractions(editor)
    }

    @Test
    fun `disabling the switch stops delivery of committed strings`() {
        enabled = false

        val handled = fakeTransport.emitCommitText("Secret")

        assertFalse(handled)
        verifyNoInteractions(editor)
    }

    @Test
    fun `toggling switch from enabled to disabled stops subsequent delivery`() {
        fakeTransport.emitCommitText("First")
        verify(editor).insert("First")

        enabled = false
        val handledSecond = fakeTransport.emitCommitText("Second")

        assertFalse(handledSecond)
        verify(editor, never()).insert("Second")
    }
}
