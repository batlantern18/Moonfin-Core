package com.moonfin.tvkeyboard.settings
import com.moonfin.tvkeyboard.BuildConfig

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.moonfin.tvkeyboard.R
import com.moonfin.tvkeyboard.ime.LanguageLayout
import com.moonfin.tvkeyboard.ime.LanguageRegistry
import com.moonfin.tvkeyboard.ime.TvKeyboardService
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.VisualTheme

/**
 * TV-safe settings app for Moonfin TV Keyboard.
 * Fully navigable with D-pad, no touchscreen required.
 * Sections: Activation, Layout, Theme, Style, Options, About.
 */
class MainActivity : Activity() {

    // Constants for system settings intents
    companion object {
        const val IME_PACKAGE_NAME = "com.moonfin.tvkeyboard"
        const val IME_CLASS_NAME = "com.moonfin.tvkeyboard.ime.TvKeyboardService"
        const val ACTION_ENABLE_KEYBOARD = "android.settings.INPUT_METHOD_SETTINGS"
        const val ACTION_CHOOSE_KEYBOARD = "android.settings.INPUT_METHOD_SUBTYPE_SETTINGS"
        const val EXTRA_INPUT_METHOD_ID = "android.provider.extra.INPUT_METHOD_ID"
    }

    private lateinit var preferences: KeyboardPreferences
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var scrollView: ScrollView
    private lateinit var contentLayout: LinearLayout
    private var lastFocusedView: View? = null

    // Section views for focus restoration
    private var activationSection: View? = null
    private var layoutSection: View? = null
    private var themeSection: View? = null
    private var styleSection: View? = null
    private var optionsSection: View? = null
    private var aboutSection: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = KeyboardPreferences(this)
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        window.setBackgroundDrawable(ColorDrawable(color(R.color.moonfin_background)))
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(color(R.color.moonfin_background))
        }
        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(48), dp(40), dp(48), dp(40))
        }
        scrollView.addView(contentLayout, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        setContentView(scrollView)

        buildAllSections()
    }

    override fun onResume() {
        super.onResume()
        refreshActivationStatus()
        // Restore focus to last focused view or first focusable
        (lastFocusedView ?: findFirstFocusable(contentLayout))?.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        lastFocusedView = currentFocus
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Handle Back key to navigate up or exit
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // If a dialog or sub-screen is open, close it
            // For now, just finish the activity
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun buildAllSections() {
        contentLayout.addView(buildHeader())
        contentLayout.addView(buildActivationSection())
        contentLayout.addView(buildLayoutSection())
        contentLayout.addView(buildThemeSection())
        contentLayout.addView(buildStyleSection())
        contentLayout.addView(buildOptionsSection())
        contentLayout.addView(buildAboutSection())
    }

    private fun buildHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(32))

            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.settings_title)
                textSize = 36f
                setTextColor(color(R.color.moonfin_on_surface))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            })

            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.settings_subtitle)
                textSize = 18f
                setTextColor(Color.parseColor("#B3FFFFFF"))
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            })
        }
    }

    private fun buildActivationSection(): LinearLayout {
        val section = buildSectionContainer(R.string.section_activation)
        activationSection = section

        // Status card
        val statusCard = buildCard()
        val statusText = TextView(this).apply {
            id = View.generateViewId()
            textSize = 18f
            setTextColor(color(R.color.moonfin_on_surface))
            setLineSpacing(dp(6).toFloat(), 1f)
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }
        statusCard.addView(statusText)
        section.addView(statusCard, lpMatchWidth(dp(16)))

        // Buttons row
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(16), 0, 0)
        }
        buttonRow.addView(actionButton(getString(R.string.action_enable_keyboard)) { openInputMethodSettings() }, lpButton())
        buttonRow.addView(actionButton(getString(R.string.action_choose_keyboard)) { chooseKeyboard() }, lpButton())
        buttonRow.addView(actionButton(getString(R.string.action_refresh)) { refreshActivationStatus() }, lpButton())
        section.addView(buttonRow, lpMatchWidth(dp(16)))

        refreshActivationStatus()
        return section
    }

    private fun refreshActivationStatus() {
        val statusCard = (activationSection as LinearLayout).getChildAt(0) as LinearLayout
        val statusView = statusCard.getChildAt(0) as TextView

        val component = imeComponent()
        val enabled = inputMethodManager.enabledInputMethodList.any {
            it.packageName == packageName && it.serviceName == component.className
        }
        val defaultId = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val selected = defaultId == component.flattenToShortString() || defaultId == component.flattenToString()

        val enabledLine = getString(if (enabled) R.string.activation_enabled else R.string.activation_disabled)
        val selectedLine = getString(if (selected) R.string.activation_selected else R.string.activation_not_selected)
        statusView.text = "$enabledLine\n$selectedLine"
    }

    private fun buildLayoutSection(): LinearLayout {
        val section = buildSectionContainer(R.string.section_layout)
        layoutSection = section

        val hint = TextView(this).apply {
            text = getString(R.string.layout_hint)
            textSize = 16f
            setTextColor(Color.parseColor("#99FFFFFF"))
            setLineSpacing(dp(4).toFloat(), 1f)
            setPadding(0, 0, 0, dp(16))
        }
        section.addView(hint, lpMatchWidth(0))

        val languages = LanguageRegistry.all(this)
        val enabledKeys = preferences.enabledLanguageKeys

        languages.forEach { lang ->
            val row = buildLanguageRow(lang, lang.key in enabledKeys)
            section.addView(row, lpMatchWidth(dp(8)))
        }

        return section
    }

    private fun buildLanguageRow(lang: LanguageLayout, enabled: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isFocusableInTouchMode = true
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = buildFocusableBackground()
        }

        val checkbox = CheckBox(this).apply {
            isChecked = enabled
            isFocusable = false
            // Use a custom drawable for TV focus
            buttonTintList = ColorStateList.valueOf(color(R.color.moonfin_accent))
        }
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            preferences.setLanguageEnabled(lang, isChecked)
        }

        val label = TextView(this).apply {
            text = lang.displayName
            textSize = 20f
            setTextColor(color(R.color.moonfin_on_surface))
            setPadding(dp(16), 0, 0, 0)
        }

        row.addView(checkbox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        return row
    }

    private fun buildThemeSection(): LinearLayout {
        val section = buildSectionContainer(R.string.section_theme)
        themeSection = section

        val hint = TextView(this).apply {
            text = getString(R.string.theme_hint)
            textSize = 16f
            setTextColor(Color.parseColor("#99FFFFFF"))
            setLineSpacing(dp(4).toFloat(), 1f)
            setPadding(0, 0, 0, dp(16))
        }
        section.addView(hint, lpMatchWidth(0))

        val radioGroup = RadioGroup(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }

        val themes = listOf(
            VisualTheme.MOONFIN to R.string.theme_moonfin,
            VisualTheme.NEON_PULSE to R.string.theme_neon_pulse,
            VisualTheme.GLASS to R.string.theme_glass,
            VisualTheme.EIGHTBIT_HERO to R.string.theme_eightbit,
        )

        themes.forEach { (theme, stringRes) ->
            val radio = RadioButton(this).apply {
                text = getString(stringRes)
                textSize = 20f
                setTextColor(color(R.color.moonfin_on_surface))
                isChecked = preferences.theme == theme
                buttonTintList = ColorStateList.valueOf(color(R.color.moonfin_accent))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { preferences.theme = theme }
            }
            radioGroup.addView(radio, lpMatchWidth(dp(8)))
        }

        section.addView(radioGroup, lpMatchWidth(0))
        return section
    }

    private fun buildStyleSection(): LinearLayout {
        val section = buildSectionContainer(R.string.section_style)
        styleSection = section

        val hint = TextView(this).apply {
            text = getString(R.string.style_hint)
            textSize = 16f
            setTextColor(Color.parseColor("#99FFFFFF"))
            setLineSpacing(dp(4).toFloat(), 1f)
            setPadding(0, 0, 0, dp(16))
        }
        section.addView(hint, lpMatchWidth(0))

        val radioGroup = RadioGroup(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }

        val styles = listOf(
            KeyboardStyle.CLASSIC to R.string.style_classic,
            KeyboardStyle.GLASS to R.string.style_glass,
            KeyboardStyle.DOCKED to R.string.style_docked,
        )

        styles.forEach { (style, stringRes) ->
            val radio = RadioButton(this).apply {
                text = getString(stringRes)
                textSize = 20f
                setTextColor(color(R.color.moonfin_on_surface))
                isChecked = preferences.style == style
                buttonTintList = ColorStateList.valueOf(color(R.color.moonfin_accent))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { preferences.style = style }
            }
            radioGroup.addView(radio, lpMatchWidth(dp(8)))
        }

        section.addView(radioGroup, lpMatchWidth(0))
        return section
    }

    private fun buildOptionsSection(): LinearLayout {
        val section = buildSectionContainer(R.string.section_options)
        optionsSection = section

        val options = listOf(
            OptionItem(R.string.option_keep_on_screen, R.string.option_keep_on_screen_desc,
                { preferences.keepOnScreen }, { preferences.keepOnScreen = it }),
            OptionItem(R.string.option_enlarge, R.string.option_enlarge_desc,
                { preferences.enlarge }, { preferences.enlarge = it }),
            OptionItem(R.string.option_suggestions, R.string.option_suggestions_desc,
                { preferences.suggestionsEnabled }, { preferences.suggestionsEnabled = it }),
            OptionItem(R.string.option_cyclic, R.string.option_cyclic_desc,
                { preferences.cyclicNavigation }, { preferences.cyclicNavigation = it }),
            OptionItem(R.string.option_auto_space, R.string.option_auto_space_desc,
                { preferences.autoSpace }, { preferences.autoSpace = it }),
            OptionItem(R.string.option_hide_physical, R.string.option_hide_physical_desc,
                { preferences.hideForPhysicalKeyboard }, { preferences.hideForPhysicalKeyboard = it }),
            OptionItem(R.string.option_voice, R.string.option_voice_desc,
                { preferences.voiceEnabled }, { preferences.voiceEnabled = it }),
        )

        options.forEach { option ->
            val row = buildOptionRow(option.titleRes, option.descRes, option.getter, option.setter)
            section.addView(row, lpMatchWidth(dp(4)))
        }

        return section
    }

    private fun buildOptionRow(
        titleRes: Int, descRes: Int,
        getter: () -> Boolean, setter: (Boolean) -> Unit
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isFocusableInTouchMode = true
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = buildFocusableBackground()
        }

        val labelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        labelLayout.addView(TextView(this).apply {
            text = getString(titleRes)
            textSize = 20f
            setTextColor(color(R.color.moonfin_on_surface))
        })
        labelLayout.addView(TextView(this).apply {
            text = getString(descRes)
            textSize = 14f
            setTextColor(Color.parseColor("#99FFFFFF"))
            setPadding(0, dp(2), 0, 0)
        })

        val checkbox = CheckBox(this).apply {
            isChecked = getter()
            isFocusable = false
            buttonTintList = ColorStateList.valueOf(color(R.color.moonfin_accent))
        }
        checkbox.setOnCheckedChangeListener { _, isChecked -> setter(isChecked) }

        row.addView(labelLayout)
        row.addView(checkbox)

        return row
    }

    private data class OptionItem(
        val titleRes: Int,
        val descRes: Int,
        val getter: () -> Boolean,
        val setter: (Boolean) -> Unit
    )

    private fun buildAboutSection(): LinearLayout {
        val section = buildSectionContainer(R.string.section_about)
        aboutSection = section

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "Unknown"
        }
        val buildType = if (BuildConfig.DEBUG) "Debug" else "Release"

        val items = listOf(
            getString(R.string.about_version) to "$versionName ($buildType)",
            getString(R.string.about_status) to getImeStatusString(),
            getString(R.string.about_license) to getString(R.string.about_license_text),
            getString(R.string.about_project) to "https://github.com/batlantern18/Moonfin-Core",
        )

        items.forEach { (label, value) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            row.addView(TextView(this).apply {
                text = label
                textSize = 16f
                setTextColor(Color.parseColor("#99FFFFFF"))
            })
            row.addView(TextView(this).apply {
                text = value
                textSize = 18f
                setTextColor(color(R.color.moonfin_on_surface))
                setPadding(0, dp(2), 0, 0)
            })
            section.addView(row, lpMatchWidth(dp(8)))
        }

        return section
    }

    private fun getImeStatusString(): String {
        val component = imeComponent()
        val enabled = inputMethodManager.enabledInputMethodList.any {
            it.packageName == packageName && it.serviceName == component.className
        }
        val defaultId = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val selected = defaultId == component.flattenToShortString() || defaultId == component.flattenToString()
        return "${if (enabled) "Enabled" else "Disabled"}, ${if (selected) "Selected" else "Not selected"}"
    }

    // Helper functions for building UI

    private fun buildSectionContainer(titleRes: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, dp(8))

            addView(TextView(this@MainActivity).apply {
                text = getString(titleRes)
                textSize = 24f
                setTextColor(color(R.color.moonfin_accent))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, dp(12))
            })
        }
    }

    private fun buildCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.moonfin_surface))
            val bg = GradientDrawable().apply {
                setColor(color(R.color.moonfin_surface))
                cornerRadius = dp(12).toFloat()
            }
            background = bg
        }
    }

    private fun buildFocusableBackground(): GradientDrawable {
        val normal = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = dp(8).toFloat()
        }
        val focused = GradientDrawable().apply {
            setColor(color(R.color.moonfin_button_normal))
            cornerRadius = dp(8).toFloat()
            setStroke(dp(2), color(R.color.moonfin_accent))
        }
        val stateList = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
        return focused // Simplified for now
    }

    private fun actionButton(label: String, onClick: () -> Unit): Button {
        val focusedBackground = color(R.color.moonfin_button_focused)
        val normalBackground = color(R.color.moonfin_button_normal)
        val focusedText = color(R.color.moonfin_on_button_focused)
        val normalText = color(R.color.moonfin_on_surface)
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 18f
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            minHeight = dp(56)
            minimumHeight = dp(56)
            setPadding(dp(24), 0, dp(24), 0)
            backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(focusedBackground, normalBackground),
            )
            setTextColor(
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                    intArrayOf(focusedText, normalText),
                ),
            )
            setOnClickListener { onClick() }
        }
    }

    private fun lpMatchWidth(bottomMargin: Int = 0): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = dp(bottomMargin)
        return params
    }

    

    private fun lpButton(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0, dp(56), 1f
        ).apply { setMargins(dp(8), 0, dp(8), 0) }
    }

    private fun openInputMethodSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            toast(getString(R.string.activation_hint))
        }
    }

    private fun chooseKeyboard() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
            .putExtra(Settings.EXTRA_INPUT_METHOD_ID, imeComponent().flattenToShortString())
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            inputMethodManager.showInputMethodPicker()
        }
    }

    private fun imeComponent(): ComponentName = ComponentName(this, TvKeyboardService::class.java)

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun color(id: Int): Int {
        @Suppress("DEPRECATION")
        return resources.getColor(id)
    }

    private fun findFirstFocusable(root: ViewGroup): View? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is ViewGroup) {
                val found = findFirstFocusable(child)
                if (found != null) return found
            } else if (child.isFocusable || child.isFocusableInTouchMode) {
                return child
            }
        }
        return null
    }
}