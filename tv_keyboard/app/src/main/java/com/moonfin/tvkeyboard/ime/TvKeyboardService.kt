package com.moonfin.tvkeyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences

/**
 * The Moonfin Android TV input method. Letters, space, backspace, cursor keys,
 * Shift, and Back are wired for the tracer bullet; symbols, suggestions, voice,
 * clipboard, and accents arrive in later tickets.
 */
class TvKeyboardService : InputMethodService(), KeyboardView.Listener, KeyboardView.LongPressListener {

    private lateinit var preferences: KeyboardPreferences
    private lateinit var session: KeyboardSession
    private var keyboardView: KeyboardView? = null

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        session = KeyboardSession(this, preferences)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val view = KeyboardView(this)
        view.listener = this
        keyboardView = view
        syncView()
        view.requestFocus()
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        session.beginInput(attribute, restarting)
        syncView()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        syncView()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            requestHideSelf(0)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKey(key: android.inputmethodservice.Keyboard.Key) {
        val keyCode = key.codes.firstOrNull() ?: return
        when (val command = ImeCommandResolver.resolve(keyCode, key.label, session.isShifted, session.locale)) {
            is ImeCommand.InsertText -> {
                commit(command.text)
                session.consumeShift()
            }
            ImeCommand.DeleteBeforeCursor -> deleteBeforeCursor()
            ImeCommand.MoveLeft -> sendCursorKey(KeyEvent.KEYCODE_DPAD_LEFT)
            ImeCommand.MoveRight -> sendCursorKey(KeyEvent.KEYCODE_DPAD_RIGHT)
            ImeCommand.ToggleShift -> session.toggleShift()
            ImeCommand.LockShift -> session.lockShift()
            ImeCommand.ToggleMode -> session.toggleMode()
            ImeCommand.None -> Unit
        }
        syncView()
    }

    override fun onLongPressShift() {
        session.lockShift()
        syncView()
    }

    private fun syncView() {
        val view = keyboardView ?: return
        view.palette = session.palette
        view.cyclic = preferences.cyclicNavigation
        view.locale = session.locale
        view.shifted = session.isShifted
        view.keyboard = session.keyboard
        setInputViewBackground(view)
    }

    private fun setInputViewBackground(view: KeyboardView) {
        view.setBackgroundColor(session.palette.panel)
    }

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun deleteBeforeCursor() {
        val connection = currentInputConnection ?: return
        val selection = connection.getSelectedText(0)
        if (!selection.isNullOrEmpty()) {
            connection.commitText("", 1)
            return
        }
        if (!connection.deleteSurroundingText(1, 0)) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    private fun sendCursorKey(keyCode: Int) {
        val connection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
