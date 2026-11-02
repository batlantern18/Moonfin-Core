package com.moonfin.tvkeyboard.ime

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences

/**
 * The Moonfin Android TV input method with suggestions row, action button,
 * and full keyboard integration.
 */
class TvKeyboardService : InputMethodService(), KeyboardRootView.Listener, KeyboardView.LongPressListener {

    private lateinit var preferences: KeyboardPreferences
    private lateinit var session: KeyboardSession
    private lateinit var suggestionEngine: SuggestionEngine
    private var rootView: KeyboardRootView? = null

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        session = KeyboardSession(this, preferences)
        suggestionEngine = SuggestionEngine { _ ->
            // Editor completions would be requested via InputConnection.getCompletions
            // For now, we rely on domain suggestions for email/web fields
            emptyList()
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val view = KeyboardRootView(this)
        view.listener = this
        rootView = view
        syncView()
        view.requestKeyboardFocus()
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        session.beginInput(attribute, restarting)
        syncView()
        updateSuggestions()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        syncView()
        updateSuggestions()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Let root view handle zone navigation first
            val handled = rootView?.onKeyDown(keyCode, event) ?: false
            if (!handled) {
                requestHideSelf(0)
            }
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
                updateSuggestions()
            }
            ImeCommand.DeleteBeforeCursor -> {
                deleteBeforeCursor()
                updateSuggestions()
            }
            ImeCommand.MoveLeft -> sendCursorKey(KeyEvent.KEYCODE_DPAD_LEFT)
            ImeCommand.MoveRight -> sendCursorKey(KeyEvent.KEYCODE_DPAD_RIGHT)
            ImeCommand.ToggleShift -> session.toggleShift()
            ImeCommand.LockShift -> session.lockShift()
            ImeCommand.ToggleMode -> session.toggleMode()
            ImeCommand.None -> Unit
        }
        syncView()
    }

    override fun onActionButton() {
        val connection = currentInputConnection ?: return
        connection.performEditorAction(session.actionId)
    }

    override fun onSuggestionSelected(suggestion: Suggestion, index: Int) {
        val connection = currentInputConnection ?: return
        val replaceLength = suggestion.replacePrefix
        if (replaceLength > 0) {
            connection.deleteSurroundingText(replaceLength, 0)
        }
        connection.commitText(suggestion.text, 1)
        if (preferences.autoSpace) {
            connection.commitText(" ", 1)
        }
        rootView?.requestKeyboardFocus()
        clearSuggestions()
    }

    private fun syncView() {
        updateSuggestions()
        val view = rootView ?: return
        view.palette = session.palette
        view.cyclicNavigation = preferences.cyclicNavigation
        view.locale = session.locale
        view.shifted = session.isShifted
        view.keyboard = session.keyboard
        view.suggestionsEnabled = preferences.suggestionsEnabled

        if (session.actionLabel.isNotEmpty()) {
            view.showActionButton(session.actionLabel)
        } else {
            view.hideActionButton()
        }
        setInputViewBackground(view)
    }

    private fun setInputViewBackground(view: KeyboardRootView) {
        view.setBackgroundColor(session.palette.panel)
    }

    private fun updateSuggestions() {
        val view = rootView ?: return
        if (!preferences.suggestionsEnabled) {
            view.clearSuggestions()
            return
        }
        val connection = currentInputConnection ?: return
        val textBeforeCursor = connection.getTextBeforeCursor(SuggestionEngine.MAX_PREFIX_LOOKBEHIND, 0)?.toString() ?: ""
        val mode = suggestionEngine.modeFor(session.inputType)
        val domains = commonEmailDomains
        val suggestions = suggestionEngine.suggestions(
            textBeforeCursor = textBeforeCursor,
            mode = mode,
            domains = domains,
            limit = SuggestionEngine.MAX_SUGGESTIONS
        )
        view.setSuggestions(suggestions)
    }

    private fun clearSuggestions() {
        rootView?.clearSuggestions()
    }

    private fun commit(text: String) {
        updateSuggestions()
        currentInputConnection?.commitText(text, 1)
    }

    private fun deleteBeforeCursor() {
        updateSuggestions()
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

    companion object {
        /** Common email domains for DOMAIN suggestion mode. */
        private val commonEmailDomains = listOf(
            "@gmail.com", "@yahoo.com", "@hotmail.com", "@outlook.com",
            "@icloud.com", "@aol.com", "@protonmail.com", "@zoho.com",
            "@mail.com", "@yandex.com"
        )
    }
}
