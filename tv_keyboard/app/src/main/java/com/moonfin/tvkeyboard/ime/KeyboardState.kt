package com.moonfin.tvkeyboard.ime

import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import com.moonfin.tvkeyboard.theme.KeyboardPalette
import java.util.Locale

/**
 * A single, immutable snapshot of everything the keyboard UI needs at one
 * instant. The service builds one [KeyboardState] per render and hands it to the
 * views instead of many loose fields, so `TvKeyboardService`, `KeyboardSession`
 * and the views no longer read each other's private state directly.
 *
 * [session] is the per-input-connection typing state; [ui] is the appearance and
 * input-surface state derived from the persisted preferences. Both are data
 * classes, so two renders are comparable with `==` and the mapping can be tested
 * on the JVM without a device.
 */
data class KeyboardState(
    val session: SessionState,
    val ui: UIState,
) {
    // --- Derived view state, read straight from the aggregate. ---

    /** Theme the keys, action button, suggestions and popups are drawn with. */
    val palette: KeyboardPalette get() = ui.palette

    /** Editor-action label, e.g. "Search", "Send", "Enter". Empty hides the button. */
    val actionLabel: String get() = session.actionLabel

    val isShifted: Boolean get() = session.isShifted
    val isRtl: Boolean get() = session.isRtl
    val cyclic: Boolean get() = ui.cyclic
    val locale: Locale get() = ui.locale
    val suggestions: List<Suggestion> get() = ui.suggestions
    val suggestionsVisible: Boolean get() = ui.suggestionsEnabled && ui.suggestions.isNotEmpty()

    /** Per-input-connection state: what is being typed and how. */
    data class SessionState(
        val mode: KeyboardSession.Mode,
        val shift: KeyboardSession.Shift,
        val languageIndex: Int,
        val languageKey: String,
        val isRtl: Boolean,
        val isShifted: Boolean,
        val layoutResource: String,
        val actionLabel: String,
        val actionId: Int,
        val inputType: Int,
    )

    /** Appearance and input-surface state derived from the persisted preferences. */
    data class UIState(
        val palette: KeyboardPalette,
        val suggestions: List<Suggestion>,
        val cyclic: Boolean,
        val locale: Locale,
        val suggestionsEnabled: Boolean,
        val enlarge: Boolean,
        val keepOnScreen: Boolean,
    )

    companion object {
        /**
         * Collapse a live [KeyboardSession] and its [KeyboardPreferences] into one
         * snapshot. [suggestions] are the completions the engine produced for the
         * current field; pass an empty list when suggestions are disabled.
         */
        fun from(
            session: KeyboardSession,
            preferences: KeyboardPreferences,
            suggestions: List<Suggestion> = emptyList(),
        ): KeyboardState =
            KeyboardState(
                session = SessionState(
                    mode = session.mode,
                    shift = session.shift,
                    languageIndex = session.languageIndex,
                    languageKey = session.languageKey,
                    isRtl = session.language.isRtl,
                    isShifted = session.isShifted,
                    layoutResource = session.activeLayoutResource,
                    actionLabel = session.actionLabel,
                    actionId = session.actionId,
                    inputType = session.inputType,
                ),
                ui = UIState(
                    palette = session.palette,
                    suggestions = suggestions,
                    cyclic = preferences.cyclicNavigation,
                    locale = session.locale,
                    suggestionsEnabled = preferences.suggestionsEnabled,
                    enlarge = preferences.enlarge,
                    keepOnScreen = preferences.keepOnScreen,
                ),
            )
    }
}
