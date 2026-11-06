package com.moonfin.tvkeyboard.prefs

import android.content.Context
import android.content.SharedPreferences
import com.moonfin.tvkeyboard.ime.LanguageLayout
import com.moonfin.tvkeyboard.ime.LanguageRegistry
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.VisualTheme
import java.util.Locale

/**
 * Every persisted keyboard setting. Defaults mirror Moonfin: the Docked
 * keyboard style, the Moonfin theme, suggestions on, and only English enabled.
 */
class KeyboardPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var theme: VisualTheme
        get() = VisualTheme.fromId(prefs.getString(KEY_THEME, null))
        set(value) = prefs.edit().putString(KEY_THEME, value.id).apply()

    var style: KeyboardStyle
        get() = KeyboardStyle.fromId(prefs.getString(KEY_STYLE, null))
        set(value) = prefs.edit().putString(KEY_STYLE, value.id).apply()

    var keepOnScreen: Boolean
        get() = prefs.getBoolean(KEY_KEEP_ON_SCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_ON_SCREEN, value).apply()

    var enlarge: Boolean
        get() = prefs.getBoolean(KEY_ENLARGE, false)
        set(value) = prefs.edit().putBoolean(KEY_ENLARGE, value).apply()

    var suggestionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUGGESTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SUGGESTIONS, value).apply()

    var cyclicNavigation: Boolean
        get() = prefs.getBoolean(KEY_CYCLIC, false)
        set(value) = prefs.edit().putBoolean(KEY_CYCLIC, value).apply()

    var autoSpace: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPACE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPACE, value).apply()

    var hideForPhysicalKeyboard: Boolean
        get() = prefs.getBoolean(KEY_HIDE_PHYSICAL, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_PHYSICAL, value).apply()

    var voiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE, value).apply()

    var autodetectLanguage: Boolean
        get() = prefs.getBoolean(KEY_AUTODETECT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTODETECT, value).apply()

    /**
     * The last language the user picked inside the keyboard, so the selection
     * survives a reboot. Null until the user has chosen one; `initialLanguage`
     * honours it before any first-run auto-detection.
     */
    var selectedLanguageKey: String?
        get() = prefs.getString(KEY_SELECTED_LANGUAGE, null)
        set(value) = prefs.edit().putString(KEY_SELECTED_LANGUAGE, value).apply()

    /** Stable keys such as `en_us` or `fr_azerty`. Empty means "use the default". */
    var enabledLanguageKeys: Set<String>
        get() = prefs.getStringSet(KEY_LANGUAGES, null)?.toSet() ?: setOf(DEFAULT_LANGUAGE)
        set(value) {
            val clean = value.ifEmpty { setOf(DEFAULT_LANGUAGE) }
            prefs.edit().putStringSet(KEY_LANGUAGES, clean).apply()
        }

    fun enabledLanguages(): List<LanguageLayout> {
        val all = LanguageRegistry.all(appContext)
        val enabled = enabledLanguageKeys
        val selected = all.filter { it.key in enabled }
        return selected.ifEmpty { all.filter { it.key == DEFAULT_LANGUAGE }.ifEmpty { all.take(1) } }
    }

    fun allLanguages(): List<LanguageLayout> = LanguageRegistry.all(appContext)

    /**
     * First-run layout choice: when auto-detect is on, prefer the layout whose
     * language matches the system locale, then English, then the first entry.
     */
    fun initialLanguage(all: List<LanguageLayout> = allLanguages()): LanguageLayout {
        val fallback = all.firstOrNull { it.key == DEFAULT_LANGUAGE } ?: all.first()
        // A persisted choice wins: the user already picked it.
        selectedLanguageKey?.let { key -> all.firstOrNull { it.key == key }?.let { return it } }
        if (!autodetectLanguage) return fallback
        val systemLanguage = Locale.getDefault().language
        return all.firstOrNull { it.code.substringBefore('_') == systemLanguage } ?: fallback
    }

    fun isEnabled(language: LanguageLayout): Boolean =
        language.key in enabledLanguageKeys

    /** Turning the last language off is not allowed; it is a no-op. */
    fun setLanguageEnabled(language: LanguageLayout, enabled: Boolean) {
        val current = enabledLanguageKeys
        val next = if (enabled) current + language.key else current - language.key
        if (next.isNotEmpty()) enabledLanguageKeys = next
    }

    fun reset() = prefs.edit().clear().apply()

    companion object {
        const val FILE = "moonfin_tv_keyboard"
        const val KEY_THEME = "theme"
        const val KEY_STYLE = "keyboardStyle"
        const val KEY_LANGUAGES = "enabledLanguages"
        const val KEY_KEEP_ON_SCREEN = "keepOnScreen"
        const val KEY_ENLARGE = "enlargeKeyboard"
        const val KEY_SUGGESTIONS = "suggestionsEnabled"
        const val KEY_CYCLIC = "cyclicNavigation"
        const val KEY_AUTO_SPACE = "autoSpace"
        const val KEY_HIDE_PHYSICAL = "hidePhysicalKeyboard"
        const val KEY_VOICE = "voiceEnabled"
        const val KEY_AUTODETECT = "autodetectLanguage"
        const val KEY_SELECTED_LANGUAGE = "selectedLanguageKey"
        const val DEFAULT_LANGUAGE = "en_us"
    }
}
