package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.moonfin.tvkeyboard.R
import com.moonfin.tvkeyboard.theme.KeyboardPalette
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.ThemeCatalog
import com.moonfin.tvkeyboard.theme.VisualTheme

/**
 * Root view composing the suggestions row, editor-action button, and keyboard view.
 * Owns focus zones: SUGGESTIONS, ACTION, KEYBOARD. D-pad navigation moves between zones.
 */
class KeyboardRootView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    interface Listener {
        fun onKey(key: android.inputmethodservice.Keyboard.Key)
        fun onActionButton()
        fun onSuggestionSelected(suggestion: Suggestion, index: Int)
    }

    var listener: Listener? = null

    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) {
            field = value
            suggestionsRow.palette = value
            keyboardView.palette = value
            actionButton?.let { it.palette = value }
            invalidate()
        }

    var suggestionsEnabled: Boolean = true
        set(value) {
            field = value
            suggestionsRow.suggestionsVisible = value
            if (!value) {
                suggestionsRow.clear()
            }
            requestLayout()
        }

    var cyclicNavigation: Boolean = false
        set(value) {
            field = value
            keyboardView.cyclic = value
        }

    var locale: java.util.Locale = java.util.Locale.getDefault()
        set(value) {
            field = value
            keyboardView.locale = value
        }

    var shifted: Boolean = false
        set(value) {
            field = value
            keyboardView.shifted = value
        }

    var keyboard: android.inputmethodservice.Keyboard? = null
        set(value) {
            field = value
            keyboardView.keyboard = value
        }

    private enum class FocusZone { SUGGESTIONS, ACTION, KEYBOARD }

    private var currentZone = FocusZone.KEYBOARD

    private val suggestionsRow = SuggestionsRow(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(16, 8, 16, 8)
    }

    private val keyboardView = KeyboardView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = android.view.Gravity.BOTTOM }
    }

    private var actionButton: ActionButton? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        suggestionsRow.listener = object : SuggestionsRow.Listener {
            override fun onSuggestionSelected(suggestion: Suggestion, index: Int) {
                listener?.onSuggestionSelected(suggestion, index)
            }

            override fun onFocusLeft() {
                // Left from first suggestion - stay in suggestions
            }

            override fun onFocusRight() {
                // Right from last suggestion - move to action button
                if (actionButton != null) {
                    currentZone = FocusZone.ACTION
                    actionButton!!.requestFocus()
                }
            }
        }

        keyboardView.listener = object : KeyboardView.Listener {
            override fun onKey(key: android.inputmethodservice.Keyboard.Key) {
                listener?.onKey(key)
            }
        }

        addView(suggestionsRow)
        addView(keyboardView)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val measuredWidth = if (widthMode == MeasureSpec.UNSPECIFIED) {
            resources.displayMetrics.widthPixels
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }

        val actionButtonHeight = resources.getDimensionPixelSize(R.dimen.action_button_height)

        // Measure suggestions row first
        suggestionsRow.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val suggestionsHeight = if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
            suggestionsRow.measuredHeight
        } else {
            0
        }

        // Measure keyboard view
        val keyboardWidthSpec = MeasureSpec.makeMeasureSpec(measuredWidth - paddingLeft - paddingRight, MeasureSpec.EXACTLY)
        val keyboardHeightSpec = MeasureSpec.makeMeasureSpec(
            MeasureSpec.getSize(heightMeasureSpec) - suggestionsHeight - actionButtonHeight - paddingTop - paddingBottom,
            MeasureSpec.AT_MOST
        )
        keyboardView.measure(keyboardWidthSpec, keyboardHeightSpec)

        // Measure action button
        if (actionButton != null) {
            actionButton!!.measure(
                MeasureSpec.makeMeasureSpec(measuredWidth - paddingLeft - paddingRight, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(actionButtonHeight, MeasureSpec.EXACTLY)
            )
        }

        val totalHeight = suggestionsHeight + keyboardView.measuredHeight +
            (actionButton?.measuredHeight ?: 0) + paddingTop + paddingBottom

        setMeasuredDimension(measuredWidth, resolveSize(totalHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val contentWidth = right - left - paddingLeft - paddingRight
        var currentTop = paddingTop

        // Layout suggestions row
        if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
            suggestionsRow.layout(
                paddingLeft, currentTop,
                paddingLeft + contentWidth, currentTop + suggestionsRow.measuredHeight
            )
            currentTop += suggestionsRow.measuredHeight
        } else {
            suggestionsRow.layout(paddingLeft, currentTop, paddingLeft + contentWidth, currentTop)
        }

        // Layout action button at bottom
        if (actionButton != null) {
            val actionHeight = actionButton!!.measuredHeight
            val actionTop = bottom - top - paddingBottom - actionHeight
            actionButton!!.layout(
                paddingLeft, actionTop,
                paddingLeft + contentWidth, actionTop + actionHeight
            )
            currentTop = actionTop
        }

        // Layout keyboard view in remaining space
        val keyboardTop = currentTop
        val keyboardBottom = actionButton?.top ?: bottom - top - paddingBottom
        keyboardView.layout(
            paddingLeft, keyboardTop,
            paddingLeft + contentWidth, keyboardBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(palette.panel)
    }

    /**
     * Apply one [KeyboardState] to the whole surface. This is the single seam
     * the service uses to render, so the view no longer reads the session or
     * preferences directly. [keyboard] is the parsed layout for the state's
     * [KeyboardState.SessionState.layoutResource]; it is an Android resource
     * concern rather than typed state, so it is passed alongside the snapshot.
     */
    fun apply(state: KeyboardState, keyboard: android.inputmethodservice.Keyboard?) {
        palette = state.palette
        cyclicNavigation = state.cyclic
        locale = state.locale
        shifted = state.isShifted
        suggestionsEnabled = state.ui.suggestionsEnabled
        this.keyboard = keyboard
        setSuggestions(state.suggestions)
        if (state.actionLabel.isNotEmpty()) {
            showActionButton(state.actionLabel)
        } else {
            hideActionButton()
        }
    }

    fun setSuggestions(suggestions: List<Suggestion>) {
        suggestionsRow.suggestions = suggestions
        requestLayout()
    }

    fun clearSuggestions() {
        suggestionsRow.clear()
        requestLayout()
    }

    fun showActionButton(label: String) {
        if (actionButton == null) {
            actionButton = ActionButton(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { gravity = android.view.Gravity.BOTTOM }
                setOnClickListener { listener?.onActionButton() }
                addView(this)
            }
        }
        actionButton!!.label = label
        actionButton!!.palette = palette
        actionButton!!.visibility = View.VISIBLE
        requestLayout()
    }

    fun hideActionButton() {
        actionButton?.visibility = View.GONE
        requestLayout()
    }

    fun requestKeyboardFocus() {
        currentZone = FocusZone.KEYBOARD
        keyboardView.requestFocus()
    }

    fun requestSuggestionsFocus() {
        if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
            currentZone = FocusZone.SUGGESTIONS
            suggestionsRow.requestFocus()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                when (currentZone) {
                    FocusZone.KEYBOARD -> {
                        // Up from keyboard top row goes to suggestions
                        if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
                            requestSuggestionsFocus()
                            return true
                        }
                    }
                    FocusZone.ACTION -> {
                        // Up from action goes to keyboard
                        requestKeyboardFocus()
                        return true
                    }
                    FocusZone.SUGGESTIONS -> {}
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                when (currentZone) {
                    FocusZone.SUGGESTIONS -> {
                        // Down from suggestions goes to keyboard
                        requestKeyboardFocus()
                        return true
                    }
                    FocusZone.KEYBOARD -> {
                        // Down from keyboard bottom goes to action
                        if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
                            currentZone = FocusZone.ACTION
                            actionButton!!.requestFocus()
                            return true
                        }
                    }
                    FocusZone.ACTION -> {}
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                when (currentZone) {
                    FocusZone.ACTION -> {
                        // Left from action goes to suggestions
                        if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
                            requestSuggestionsFocus()
                            suggestionsRow.setFocusedToLast()
                            return true
                        }
                    }
                    FocusZone.KEYBOARD, FocusZone.SUGGESTIONS -> {}
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                when (currentZone) {
                    FocusZone.SUGGESTIONS -> {
                        // Right from last suggestion handled by SuggestionsRow
                    }
                    FocusZone.KEYBOARD -> {
                        // Right from keyboard right edge goes to action
                        if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
                            currentZone = FocusZone.ACTION
                            actionButton!!.requestFocus()
                            return true
                        }
                    }
                    FocusZone.ACTION -> {}
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                // Dismiss overlays, then hide IME
                if (currentZone != FocusZone.KEYBOARD) {
                    requestKeyboardFocus()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFocus) {
            // Ensure we stay in the correct zone
            when (currentZone) {
                FocusZone.SUGGESTIONS -> suggestionsRow.requestFocus()
                FocusZone.ACTION -> actionButton?.requestFocus()
                FocusZone.KEYBOARD -> keyboardView.requestFocus()
            }
        }
    }
}

/**
 * Editor action button (Go, Search, Send, Next, Done, Enter).
 */
class ActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) {
            field = value
            invalidate()
        }

    var label: String = ""
        set(value) {
            field = value
            invalidate()
        }

    private val density: Float get() = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val radius: Float get() = (8 * density).coerceAtLeast(1f)

    init {
        isFocusable = true
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = resources.getDimensionPixelSize(R.dimen.action_button_height)
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(height, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        backgroundPaint.color = palette.accent
        canvas.drawRoundRect(rect, radius, radius, backgroundPaint)

        if (isFocused) {
            borderPaint.color = palette.focusBorder.color
            borderPaint.strokeWidth = palette.focusBorder.width * density
            canvas.drawRoundRect(rect, radius, radius, borderPaint)
        }

        if (label.isNotEmpty()) {
            textPaint.color = palette.accentText
            textPaint.textSize = (16 * density).coerceAtLeast(12f)
            val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, rect.centerX(), baseline, textPaint)
        }
    }
}