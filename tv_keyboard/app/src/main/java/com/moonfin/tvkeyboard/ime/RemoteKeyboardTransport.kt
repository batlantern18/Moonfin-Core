package com.moonfin.tvkeyboard.ime

import android.view.KeyEvent

/**
 * Transport interface for receiving input from a companion phone remote.
 */
interface RemoteKeyboardTransport {
    fun setListener(listener: RemoteKeyboardListener?)
}

/**
 * Listener for companion phone remote input events.
 */
interface RemoteKeyboardListener {
    fun onKeyDown(keyCode: Int, event: KeyEvent? = null): Boolean
    fun onKey(keyCode: Int, event: KeyEvent? = null): Boolean = onKeyDown(keyCode, event)
    fun onCommitText(text: String): Boolean
    fun onText(text: String): Boolean = onCommitText(text)
}
