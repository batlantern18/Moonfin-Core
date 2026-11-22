package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.inputmethodservice.Keyboard
import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import java.util.Locale

/**
 * Per-input-connection state: shift, mode, active language, and the cache of
 * parsed [Keyboard] objects. The view and the service share one instance.
 */
class KeyboardSession(
    private val context: Context,
    private val preferences: KeyboardPreferences,
) {

    enum class Mode { ALPHABET, SYMBOLS, NUMBERS }

    enum class Shift { OFF, ON, LOCKED }

    var mode: Mode = Mode.ALPHABET
        private set

    var shift: Shift = Shift.OFF
        private set

    var languages: List<LanguageLayout> = preferences.enabledLanguages()
        private set

    var languageIndex: Int = 0
        private set

    var actionLabel: String = ""
        private set

    var actionId: Int = EditorInfo.IME_ACTION_UNSPECIFIED
        private set

    var inputType: Int = InputType.TYPE_CLASS_TEXT
        private set

    private var cache = mutableMapOf<String, Keyboard>()

    val language: LanguageLayout
        get() = languages.getOrElse(languageIndex) {
            languages.firstOrNull() ?: fallbackLanguage
        }

    val locale: Locale get() = language.locale

    val isShifted: Boolean get() = shift != Shift.OFF

    /** Stable identity of the active layout, e.g. `fr` or `en_us_azerty`. */
    val languageKey: String get() = language.key

    /**
     * The resource name of the layout currently in use, computed without a
     * [Context]. Mirrors [keyboard] but stays free of Android types so the
     * derived [KeyboardState] can be inspected on the JVM.
     */
    val activeLayoutResource: String
        get() = when (mode) {
            Mode.ALPHABET -> language.alphabetResource
            Mode.SYMBOLS -> KeyboardLayouts.SYMBOLS_RESOURCE
            Mode.NUMBERS -> KeyboardLayouts.NUMBERS_RESOURCE
        }

    val palette get() = com.moonfin.tvkeyboard.theme.ThemeCatalog.palette(
        preferences.theme,
        preferences.style,
    )

    val keyboard: Keyboard
        get() = when (mode) {
            Mode.ALPHABET -> cached("alphabet:${language.key}") {
                KeyboardLayouts.alphabet(context, language)
            }
            Mode.SYMBOLS -> cached("symbols") { KeyboardLayouts.symbols(context) }
            Mode.NUMBERS -> cached("numbers") { KeyboardLayouts.numbers(context) }
        }

    private fun cached(key: String, factory: () -> Keyboard): Keyboard =
        cache.getOrPut(key, factory)

    fun beginInput(info: EditorInfo?, restarting: Boolean) {
        reloadLanguages()
        if (!restarting) {
            mode = initialMode(info)
            shift = Shift.OFF
            if (languages.isNotEmpty()) {
                languageIndex = preferences.initialLanguage(languages)
                    .let { preferred -> languages.indexOfFirst { it.key == preferred.key } }
                    .coerceAtLeast(0)
            }
        }
        actionId = editorActionId(info)
        actionLabel = editorActionLabel(actionId)
        inputType = info?.inputType ?: InputType.TYPE_CLASS_TEXT
        cache = mutableMapOf()
    }

    private fun initialMode(info: EditorInfo?): Mode {
        val inputType = info?.inputType ?: return Mode.ALPHABET
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE -> Mode.NUMBERS
            else -> Mode.ALPHABET
        }
    }

    fun reloadLanguages() {
        languages = preferences.enabledLanguages()
        languageIndex = languageIndex.coerceIn(0, (languages.size - 1).coerceAtLeast(0))
        cache = mutableMapOf()
    }

    fun toggleShift() {
        shift = when (shift) {
            Shift.OFF -> Shift.ON
            Shift.ON -> Shift.LOCKED
            Shift.LOCKED -> Shift.OFF
        }
    }

    fun lockShift() {
        // Long-press Caps Lock: toggle between LOCKED and unlocked (OFF)
        if (shift == Shift.LOCKED) {
            shift = Shift.OFF
        } else if (shift == Shift.ON) {
            shift = Shift.LOCKED
        } else {
            shift = Shift.LOCKED
        }
    }

    fun consumeShift() {
        // One-shot Shift: reset after typing a character
        if (shift == Shift.ON) {
            shift = Shift.OFF
        }
    }

    fun toggleMode() {
        mode = when (mode) {
            Mode.ALPHABET -> Mode.SYMBOLS
            Mode.SYMBOLS -> Mode.NUMBERS
            Mode.NUMBERS -> Mode.ALPHABET
        }
    }

    fun showAlphabet() {
        mode = Mode.ALPHABET
    }

    fun showNumbers() {
        mode = Mode.NUMBERS
    }

    fun cycleLanguage() {
        if (languages.size <= 1) return
        languageIndex = (languageIndex + 1) % languages.size
        cache = mutableMapOf()
    }

    fun selectLanguage(index: Int) {
        if (index in languages.indices) {
            languageIndex = index
            cache = mutableMapOf()
        }
    }

    /** Switch to a layout by its stable key, or no-op when it is not enabled. */
    fun selectLanguageByKey(key: String) {
        val index = languages.indexOfFirst { it.key == key }
        if (index >= 0) selectLanguage(index)
    }

    private val fallbackLanguage = LanguageLayout("English", "en_us", azerty = false)

    companion object {
        fun editorActionId(info: EditorInfo?): Int {
            if (info == null) return EditorInfo.IME_ACTION_NONE
            val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
            return if (action == EditorInfo.IME_ACTION_NONE) EditorInfo.IME_ACTION_DONE else action
        }

        fun editorActionLabel(action: Int): String = when (action) {
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_DONE -> "Done"
            EditorInfo.IME_ACTION_UNSPECIFIED, EditorInfo.IME_ACTION_NONE -> "Enter"
            else -> "Done"
        }
    }
}
