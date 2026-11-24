package com.moonfin.tvkeyboard.ime

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.provider.Settings
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
    var remoteKeyboardBridge: RemoteKeyboardBridge? = null
        private set
    private lateinit var editor: InputConnectionEditor
    private lateinit var renderCoalescer: RenderCoalescer

    private var currentVoiceState: VoiceState = VoiceState.IDLE
    private var currentVoiceError: String = ""
    private var placement = WindowPlacement()

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

        remoteKeyboardBridge = RemoteKeyboardBridge(
            editor = { editor },
            isEnabled = { preferences.remoteKeyboardEnabled },
            actionId = { session.actionId },
        )
    }

    /** Attach an external companion remote transport (e.g. Android TV remote service). */
    fun attachRemoteTransport(transport: RemoteKeyboardTransport) {
        remoteKeyboardBridge?.let { transport.setListener(it) }
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
        applyWindowPlacement(info)
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

    /**
     * Hardware keys and host-side edits change the field without going through
     * the editor, so the echo line is refreshed here as well.
     */
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd,
        )
        requestRender()
    }

    /**
     * Keys the host app left unhandled arrive here, because the IME window is
     * never focusable. They have to go through the view tree: only that path
     * reaches the focused key grid, the popups and the action button.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (rootView?.dispatchKeyEvent(event) == true) return true
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            requestHideSelf(0)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (rootView?.dispatchKeyEvent(event) == true) return true
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onKey(key: android.inputmethodservice.Keyboard.Key) {
        val keyCode = key.codes.firstOrNull() ?: return
        when (val command = ImeCommandResolver.resolve(keyCode, key.label, session.isShifted, session.locale)) {
            is ImeCommand.ComposeText -> {
                editor.compose(command.text)
                session.consumeShift()
            }
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

    /** Space held: hand the choice of keyboard to the system picker. */
    override fun onLongPressSpace() {
        inputMethodManager?.showInputMethodPicker()
    }

    /** Language key held: open the system settings where languages are managed. */
    override fun onLongPressLanguage() {
        val imeId = ComponentName(this, TvKeyboardService::class.java).flattenToShortString()
        val subtypes = Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
            .putExtra(Settings.EXTRA_INPUT_METHOD_ID, imeId)
        if (startSystemScreen(subtypes)) return
        if (startSystemScreen(Intent(Settings.ACTION_LOCALE_SETTINGS))) return
        inputMethodManager?.showInputMethodPicker()
    }

    private val inputMethodManager: InputMethodManager?
        get() = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

    /** Opens a system screen from an IME; false when this device has none. */
    private fun startSystemScreen(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    /**
     * Shrinks the input window to its compact width and puts it where the host
     * asked. Done on every input start because `privateImeOptions` belongs to
     * the field, not to the service.
     */
    private fun applyWindowPlacement(info: EditorInfo?) {
        placement = WindowPlacement.parse(info?.privateImeOptions).withStyle(preferences.style)
        val inputWindow = window?.window ?: return
        val width = WindowPlacement.widthPx(resources.displayMetrics.widthPixels, placement.widthFraction)
        inputWindow.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        inputWindow.setGravity(
            Gravity.BOTTOM or when (placement.alignment) {
                HorizontalAlignment.LEFT -> Gravity.START
                HorizontalAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
                HorizontalAlignment.RIGHT -> Gravity.END
            },
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
        val echo = EchoState.of(
            before = editor.textBeforeCursor(ECHO_CONTEXT_CHARS),
            after = editor.textAfterCursor(ECHO_CONTEXT_CHARS),
            isPassword = FieldMode.from(session.inputType) == FieldMode.PASSWORD,
        )
        val state = KeyboardState.from(
            session = session,
            preferences = preferences,
            suggestions = suggestions,
            voiceState = currentVoiceState,
            voiceError = currentVoiceError,
            echo = echo,
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
        /** Enough text on each side of the caret to draw a readable echo line. */
        private const val ECHO_CONTEXT_CHARS = 200

        private val commonEmailDomains = listOf(
            "@gmail.com", "@yahoo.com", "@hotmail.com", "@outlook.com",
            "@icloud.com", "@aol.com", "@protonmail.com", "@zoho.com",
            "@mail.com", "@yandex.com"
        )
    }
}
