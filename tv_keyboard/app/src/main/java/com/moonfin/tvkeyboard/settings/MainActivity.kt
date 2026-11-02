package com.moonfin.tvkeyboard.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.moonfin.tvkeyboard.R
import com.moonfin.tvkeyboard.ime.TvKeyboardService

/**
 * Minimal TV launcher for the tracer bullet: reports whether the IME is enabled
 * and selected, and opens the system input-method settings to activate it.
 */
class MainActivity : Activity() {

    private lateinit var statusView: TextView
    private lateinit var enableButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(color(R.color.moonfin_background)))
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        enableButton.post { enableButton.requestFocus() }
    }

    private fun buildContent(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(64), dp(56), dp(64), dp(56))
        }

        content.addView(
            TextView(this).apply {
                text = getString(R.string.settings_title)
                textSize = 34f
                setTextColor(color(R.color.moonfin_on_surface))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            },
        )
        content.addView(
            TextView(this).apply {
                text = getString(R.string.settings_subtitle)
                textSize = 18f
                setTextColor(Color.parseColor("#B3FFFFFF"))
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(28))
            },
        )

        statusView = TextView(this).apply {
            textSize = 20f
            setTextColor(color(R.color.moonfin_on_surface))
            setLineSpacing(dp(6).toFloat(), 1f)
            setPadding(dp(24), dp(20), dp(24), dp(20))
            background = GradientDrawable().apply {
                setColor(color(R.color.moonfin_surface))
                cornerRadius = dp(8).toFloat()
            }
        }
        content.addView(
            statusView,
            LinearLayout.LayoutParams(dp(560), ViewGroup.LayoutParams.WRAP_CONTENT),
        )

        content.addView(
            TextView(this).apply {
                text = getString(R.string.activation_hint)
                textSize = 16f
                setTextColor(Color.parseColor("#99FFFFFF"))
                setLineSpacing(dp(4).toFloat(), 1f)
            },
            LinearLayout.LayoutParams(dp(560), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(16)
                bottomMargin = dp(24)
            },
        )

        enableButton = actionButton(getString(R.string.action_enable_keyboard)) { openInputMethodSettings() }
        content.addView(
            enableButton,
            LinearLayout.LayoutParams(dp(560), dp(64)).apply { bottomMargin = dp(16) },
        )

        content.addView(
            actionButton(getString(R.string.action_choose_keyboard)) { chooseKeyboard() },
            LinearLayout.LayoutParams(dp(560), dp(64)).apply { bottomMargin = dp(16) },
        )

        content.addView(
            actionButton(getString(R.string.action_refresh)) { refreshStatus() },
            LinearLayout.LayoutParams(dp(560), dp(64)),
        )

        return ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(color(R.color.moonfin_background))
            addView(
                content,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }
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
            minHeight = dp(64)
            minimumHeight = dp(64)
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

    private fun refreshStatus() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
}
