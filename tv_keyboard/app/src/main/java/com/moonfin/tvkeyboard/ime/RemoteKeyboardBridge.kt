package com.moonfin.tvkeyboard.ime

import android.view.KeyEvent

/**
 * Bridges companion phone remote keystrokes and committed text into the focused
 * field through [TextEditor].
 */
class RemoteKeyboardBridge(
    private val editor: () -> TextEditor?,
    private val isEnabled: () -> Boolean,
    private val actionId: () -> Int = { 0 },
    transport: RemoteKeyboardTransport? = null,
) : RemoteKeyboardListener {

    init {
        transport?.setListener(this)
    }

    override fun onCommitText(text: String): Boolean {
        if (!isEnabled()) return false
        val ed = editor() ?: return false
        ed.insert(text)
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isEnabled()) return false
        val ed = editor() ?: return false

        when (keyCode) {
            KeyCodes.DELETE, KeyEvent.KEYCODE_DEL -> {
                ed.deleteBeforeCursor()
                return true
            }
            KeyCodes.LEFT, KeyEvent.KEYCODE_DPAD_LEFT -> {
                ed.moveCursorLeft()
                return true
            }
            KeyCodes.RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                ed.moveCursorRight()
                return true
            }
            KeyCodes.SPACE, KeyEvent.KEYCODE_SPACE -> {
                ed.insert(" ")
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                val action = actionId()
                if (action != 0) {
                    ed.performAction(action)
                } else {
                    ed.insert("\n")
                }
                return true
            }
            else -> {
                // Check if the event carries a printable Unicode character
                val charFromEvent = event?.let {
                    val unicode = it.getUnicodeChar(it.metaState)
                    if (unicode > 0) String(Character.toChars(unicode)) else null
                }
                if (charFromEvent != null) {
                    ed.insert(charFromEvent)
                    return true
                }

                // Fallback for standard key codes when event is null or has no unicode
                val charFromKeyCode = when (keyCode) {
                    in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> {
                        val c = ('a'.code + (keyCode - KeyEvent.KEYCODE_A)).toChar()
                        c.toString()
                    }
                    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                        val c = ('0'.code + (keyCode - KeyEvent.KEYCODE_0)).toChar()
                        c.toString()
                    }
                    else -> TextCommitter.codePointText(keyCode)
                }

                if (charFromKeyCode != null) {
                    ed.insert(charFromKeyCode)
                    return true
                }

                return false
            }
        }
    }
}

