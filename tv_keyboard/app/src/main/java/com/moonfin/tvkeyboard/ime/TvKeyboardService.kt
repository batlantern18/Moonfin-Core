package com.moonfin.tvkeyboard.ime

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
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

    private var voiceInputController: VoiceInputController? = null
    private var clipboardController: ClipboardController? = null
    private var feedbackController: FeedbackController? = null
    private lateinit var editor: InputConnectionEditor
    private lateinit var renderCoalescer: RenderCoalescer

    private var currentVoiceState: VoiceState = VoiceState.IDLE
    private var currentVoiceError: String = ""

    override fun onCreate() {
        super.onCreate()
        preferences = KeyboardPreferences(this)
        session = KeyboardSession(this, preferences)

        val words = BundledWordList.words(this)
        suggestionEngine = SuggestionEngine { prefix ->
            BundledWordList.completionsFor(prefix, words)
        }

        feedbackController = FeedbackController(
            this,
            preferences.hapticFeedback,
            preferences.keyClickSound,
        )

        editor = InputConnectionEditor(
            connection = { currentInputConnection },
            autoSpace = { preferences.autoSpace },
            onChanged = { requestRender() },
        )
        renderCoalescer = RenderCoalescer(
            scheduler = { block ->
                val view = rootView
                if (view == null) {
                    block()
                } else {
                    view.post(block)
                }
            },
            render = { renderNow() },
        )

        voiceInputController = VoiceInputController(this, { text ->
            editor.insert(text)
        }, { message ->
            currentVoiceState = VoiceState.ERROR
            currentVoiceError = message
            requestRender()
        }).also { controller ->
            controller.onStateChanged = { state, error ->
                currentVoiceState = state
                currentVoiceError = error
                requestRender()
            }
        }

        clipboardController = ClipboardController(this, { text ->
            editor.paste(text)
        }, { })
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onEvaluateInputViewShown(): Boolean {
        if (preferences.hideForPhysicalKeyboard && isHardKeyboardConnected()) return false
        if (preferences.keepOnScreen) return true
        return super.onEvaluateInputViewShown()
    }

    private fun isHardKeyboardConnected(): Boolean {
        val config = resources.configuration
        return config.keyboard != Configuration.KEYBOARD_NOKEYS &&
            config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (preferences.hideForPhysicalKeyboard) {
            updateInputViewShown()
        }
    }

    override fun onCreateInputView(): View {
        val view = KeyboardRootView(this)
        view.listener = this
        view.keyboardView.feedbackController = feedbackController
        view.onLanguageSelected = { index -> onLanguageSelected(index) }
        view.onLanguagePickerCancelled = {
            rootView?.dismissLanguagePicker()
        }
        view.clipboardOverlay.onPaste = { item ->
            editor.paste(item.text)
            rootView?.requestKeyboardFocus()
        }
        rootView = view
        renderNow()
        view.requestKeyboardFocus()
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        session.beginInput(attribute, restarting)
        requestRender()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        requestRender()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        voiceInputController?.stop()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        voiceInputController?.stop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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
                editor.insert(command.text)
                session.consumeShift()
            }
            ImeCommand.DeleteBeforeCursor -> editor.deleteBeforeCursor()
            ImeCommand.MoveLeft -> editor.moveCursorLeft()
            ImeCommand.MoveRight -> editor.moveCursorRight()
            ImeCommand.ToggleShift -> session.toggleShift()
            ImeCommand.LockShift -> session.lockShift()
            ImeCommand.ToggleMode -> session.toggleMode()
            ImeCommand.CycleLanguage -> {
                session.cycleLanguage()
                preferences.selectedLanguageKey = session.languageKey
            }
            ImeCommand.VoiceInput -> startVoiceInput()
            ImeCommand.ClipboardOverlay -> showClipboardOverlay()
            ImeCommand.None -> Unit
        }
        requestRender()
    }

    override fun onLongPressShift() {
        session.lockShift()
        requestRender()
    }

    /** Long-press the language or space key: open the language picker. */
    override fun onLongPressLanguage() {
        val view = rootView ?: return
        view.showLanguagePicker(
            languages = session.languages,
            currentIndex = session.languageIndex,
            cyclic = preferences.cyclicNavigation,
            rtl = session.language.isRtl,
        )
    }

    /** A key with a vendored popup layout was held: open its mini keyboard. */
    override fun onLongPressKey(key: android.inputmethodservice.Keyboard.Key) {
        if (!AccentPopup.hasPopup(key)) return
        rootView?.showAccentPopup(key.popupResId, session.isShifted, session.locale)
    }

    /** A character was chosen in the popup; one-shot Shift is consumed. */
    override fun onAccentSelected(text: String) {
        rootView?.let { feedbackController?.onPopupSelection(it) }
        editor.insert(text)
        session.consumeShift()
        requestRender()
    }

    /** A language was chosen in the picker overlay. */
    private fun onLanguageSelected(index: Int) {
        session.selectLanguage(index)
        preferences.selectedLanguageKey = session.languageKey
        requestRender()
    }

    override fun onActionButton() {
        editor.performAction(session.actionId)
    }

    override fun onSuggestionSelected(suggestion: Suggestion, index: Int) {
        editor.applySuggestion(suggestion)
        rootView?.requestKeyboardFocus()
    }

    private fun renderNow() {
        val view = rootView ?: return
        val suggestions = if (preferences.suggestionsEnabled) currentSuggestions() else emptyList()
        val state = KeyboardState.from(
            session = session,
            preferences = preferences,
            suggestions = suggestions,
            voiceState = currentVoiceState,
            voiceError = currentVoiceError,
        )
        feedbackController?.updatePreferences(preferences.hapticFeedback, preferences.keyClickSound)
        view.apply(state, session.keyboard)
        setInputViewBackground(view)
    }

    private fun currentSuggestions(): List<Suggestion> {
        val textBeforeCursor = editor.textBeforeCursor(SuggestionEngine.MAX_PREFIX_LOOKBEHIND)
        val mode = suggestionEngine.modeFor(session.inputType)
        return suggestionEngine.suggestions(
            textBeforeCursor = textBeforeCursor,
            mode = mode,
            domains = commonEmailDomains,
            limit = SuggestionEngine.MAX_SUGGESTIONS,
        )
    }

    private fun setInputViewBackground(view: KeyboardRootView) {
        view.setBackgroundColor(session.palette.panel)
    }

    private fun requestRender() {
        renderCoalescer.request()
    }

    private fun startVoiceInput() {
        if (!FieldMode.from(session.inputType).allowsVoice) return
        voiceInputController?.start()
    }

    private fun showClipboardOverlay() {
        val clips = clipboardController?.snapshot()?.filter { it.text.isNotBlank() } ?: emptyList()
        val view = rootView ?: return
        view.showClipboardOverlay(clips)
    }

    companion object {
        private val commonEmailDomains = listOf(
            "@gmail.com", "@yahoo.com", "@hotmail.com", "@outlook.com",
            "@icloud.com", "@aol.com", "@protonmail.com", "@zoho.com",
            "@mail.com", "@yandex.com"
        )
    }
}
