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

        /** A character was chosen in the accent popup. */
        fun onAccentSelected(text: String)
    }

    var listener: Listener? = null

    /** Invoked with the enabled-language index the user picked in the overlay. */
    var onLanguageSelected: ((Int) -> Unit)? = null

    /** Invoked when the user dismisses the language picker without selecting. */
    var onLanguagePickerCancelled: (() -> Unit)? = null

    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) {
            field = value
            suggestionsRow.palette = value
            keyboardView.palette = value
            accentPopup.palette = value
            clipboardOverlay.palette = value
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
            accentPopup.cyclic = value
        }

    /** Mirror the surface when the active layout reads right-to-left. */
    var rtl: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            layoutDirection = if (value) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR
            languagePicker.rtl = value
            accentPopup.rtl = value
            invalidate()
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

    /** Shift and locale captured when the popup opened; they decide its text. */
    private var accentShifted = false
    private var accentLocale = java.util.Locale.getDefault()

    val suggestionsRow = SuggestionsRow(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(16, 8, 16, 8)
    }

    val keyboardView = KeyboardView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = android.view.Gravity.BOTTOM }
    }

    var actionButton: ActionButton? = null
        private set

    val accentPopup = AccentPopupView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        visibility = View.GONE
    }

    val languagePicker = LanguagePickerView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        visibility = View.GONE
    }

    val clipboardOverlay = ClipboardOverlayView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        visibility = View.GONE
    }

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
                if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
                    currentZone = FocusZone.ACTION
                    actionButton!!.requestFocus()
                }
            }

            override fun onFocusDown() {
                requestKeyboardFocus()
            }
        }

        keyboardView.listener = object : KeyboardView.Listener, KeyboardView.LongPressListener {
            override fun onKey(key: android.inputmethodservice.Keyboard.Key) {
                listener?.onKey(key)
            }

            override fun onLongPressShift() {
                (listener as? KeyboardView.LongPressListener)?.onLongPressShift()
            }

            override fun onLongPressSpace() {
                (listener as? KeyboardView.LongPressListener)?.onLongPressSpace()
            }

            override fun onLongPressLanguage() {
                (listener as? KeyboardView.LongPressListener)?.onLongPressLanguage()
            }

            override fun onLongPressKey(key: android.inputmethodservice.Keyboard.Key) {
                (listener as? KeyboardView.LongPressListener)?.onLongPressKey(key)
            }
        }

        keyboardView.edgeListener = object : KeyboardView.EdgeListener {
            override fun onEdgeUp() {
                if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
                    requestSuggestionsFocus()
                }
            }

            override fun onEdgeDown() {
                if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
                    currentZone = FocusZone.ACTION
                    actionButton!!.requestFocus()
                }
            }

            override fun onEdgeRight() {
                if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
                    currentZone = FocusZone.ACTION
                    actionButton!!.requestFocus()
                }
            }

            override fun onEdgeLeft() {}
        }

        addView(suggestionsRow)
        addView(keyboardView)
        addView(accentPopup)
        addView(languagePicker)
        addView(clipboardOverlay)

        clipboardOverlay.onDismiss = {
            clipboardOverlay.hide()
            requestKeyboardFocus()
        }

        accentPopup.onSelected = { key ->
            val text = AccentPopup.textFor(key, accentShifted, accentLocale)
            accentPopup.hide()
            requestLayout()
            requestKeyboardFocus()
            if (text != null) {
                listener?.onAccentSelected(text)
            } else {
                listener?.onKey(key)
            }
        }
        accentPopup.onCancel = {
            accentPopup.hide()
            requestLayout()
            requestKeyboardFocus()
        }

        languagePicker.onLanguageSelected = { index ->
            onLanguageSelected?.invoke(index)
        }
        languagePicker.onCancel = {
            onLanguagePickerCancelled?.invoke()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val measuredWidth = if (widthMode == MeasureSpec.UNSPECIFIED) {
            resources.displayMetrics.widthPixels
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }

        val actionButtonHeight = resources.getDimensionPixelSize(R.dimen.action_button_height)

        suggestionsRow.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val suggestionsHeight = if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
            suggestionsRow.measuredHeight
        } else {
            0
        }

        val keyboardWidthSpec = MeasureSpec.makeMeasureSpec(measuredWidth - paddingLeft - paddingRight, MeasureSpec.EXACTLY)
        val keyboardHeightSpec = MeasureSpec.makeMeasureSpec(
            MeasureSpec.getSize(heightMeasureSpec) - suggestionsHeight - actionButtonHeight - paddingTop - paddingBottom,
            MeasureSpec.AT_MOST
        )
        keyboardView.measure(keyboardWidthSpec, keyboardHeightSpec)

        if (accentPopup.isVisible) {
            accentPopup.measure(
                MeasureSpec.makeMeasureSpec(
                    measuredWidth - paddingLeft - paddingRight,
                    MeasureSpec.EXACTLY,
                ),
                MeasureSpec.makeMeasureSpec(
                    MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom,
                    MeasureSpec.AT_MOST,
                ),
            )
        }

        if (clipboardOverlay.isVisible) {
            clipboardOverlay.measure(
                MeasureSpec.makeMeasureSpec(measuredWidth - paddingLeft - paddingRight, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom, MeasureSpec.AT_MOST)
            )
        }

        if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
            actionButton!!.measure(
                MeasureSpec.makeMeasureSpec(measuredWidth - paddingLeft - paddingRight, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(actionButtonHeight, MeasureSpec.EXACTLY)
            )
        }

        val actionHeight = if (actionButton != null && actionButton!!.visibility == View.VISIBLE) actionButton!!.measuredHeight else 0
        val totalHeight = suggestionsHeight + keyboardView.measuredHeight + actionHeight + paddingTop + paddingBottom

        setMeasuredDimension(measuredWidth, resolveSize(totalHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val contentWidth = right - left - paddingLeft - paddingRight
        var currentTop = paddingTop

        if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
            suggestionsRow.layout(
                paddingLeft, currentTop,
                paddingLeft + contentWidth, currentTop + suggestionsRow.measuredHeight
            )
            currentTop += suggestionsRow.measuredHeight
        } else {
            suggestionsRow.layout(paddingLeft, currentTop, paddingLeft + contentWidth, currentTop)
        }

        if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
            val actionHeight = actionButton!!.measuredHeight
            val actionTop = bottom - top - paddingBottom - actionHeight
            actionButton!!.layout(
                paddingLeft, actionTop,
                paddingLeft + contentWidth, actionTop + actionHeight
            )
        }

        val keyboardTop = currentTop
        val keyboardBottom = if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
            actionButton!!.top
        } else {
            bottom - top - paddingBottom
        }
        keyboardView.layout(
            paddingLeft, keyboardTop,
            paddingLeft + contentWidth, keyboardBottom
        )

        if (accentPopup.isVisible) {
            val popupHeight = accentPopup.measuredHeight
            val popupTop = (keyboardView.top - popupHeight).coerceAtLeast(paddingTop)
            accentPopup.layout(
                paddingLeft, popupTop,
                paddingLeft + contentWidth, popupTop + popupHeight
            )
        } else {
            accentPopup.layout(paddingLeft, currentTop, paddingLeft, currentTop)
        }

        if (languagePicker.isVisible) {
            languagePicker.layout(paddingLeft, paddingTop, paddingLeft + contentWidth, bottom - top - paddingBottom)
        } else {
            languagePicker.layout(paddingLeft, paddingTop, paddingLeft, paddingTop)
        }

        if (clipboardOverlay.isVisible) {
            clipboardOverlay.layout(paddingLeft, paddingTop, paddingLeft + contentWidth, bottom - top - paddingBottom)
        } else {
            clipboardOverlay.layout(paddingLeft, paddingTop, paddingLeft, paddingTop)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(palette.panel)
    }

    fun showLanguagePicker(languages: List<LanguageLayout>, currentIndex: Int, cyclic: Boolean, rtl: Boolean) {
        languagePicker.palette = palette
        this.rtl = rtl
        languagePicker.show(languages, currentIndex, cyclic, rtl)
        requestLayout()
    }

    fun dismissLanguagePicker() {
        if (languagePicker.isVisible) {
            languagePicker.hide()
            requestKeyboardFocus()
        }
    }

    val isLanguagePickerVisible: Boolean get() = languagePicker.isVisible

    fun showClipboardOverlay(clips: List<ClipboardController.ClipItem>) {
        clipboardOverlay.palette = palette
        clipboardOverlay.show(clips)
        requestLayout()
    }

    fun dismissClipboardOverlay() {
        if (clipboardOverlay.isVisible) {
            clipboardOverlay.hide()
            requestKeyboardFocus()
        }
    }

    val isClipboardOverlayVisible: Boolean get() = clipboardOverlay.isVisible

    fun showAccentPopup(resId: Int, shifted: Boolean, locale: java.util.Locale) {
        accentShifted = shifted
        accentLocale = locale
        accentPopup.palette = palette
        accentPopup.cyclic = cyclicNavigation
        accentPopup.rtl = rtl
        accentPopup.describeKey = { key ->
            AccentPopup.textFor(key, accentShifted, accentLocale) ?: key.label?.toString().orEmpty()
        }
        accentPopup.show(resId)
        requestLayout()
    }

    fun dismissAccentPopup() {
        if (accentPopup.isVisible) {
            accentPopup.hide()
            requestLayout()
            requestKeyboardFocus()
        }
    }

    val isAccentPopupVisible: Boolean get() = accentPopup.isVisible

    fun apply(state: KeyboardState, keyboard: android.inputmethodservice.Keyboard?) {
        palette = state.palette
        cyclicNavigation = state.cyclic
        locale = state.locale
        shifted = state.isShifted
        rtl = state.isRtl
        suggestionsEnabled = state.ui.suggestionsEnabled
        this.keyboard = keyboard
        keyboardView.enlarge = state.ui.enlarge
        keyboardView.voiceState = state.ui.voiceState
        keyboardView.voiceError = state.ui.voiceError
        keyboardView.voiceAvailable = state.voiceAvailable
        keyboardView.spaceBarLanguageName = state.locale.displayLanguage
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
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_BACK -> {
                                requestKeyboardFocus()
                                return@setOnKeyListener true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                listener?.onActionButton()
                                return@setOnKeyListener true
                            }
                        }
                    }
                    false
                }
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
        if (languagePicker.isVisible) {
            return languagePicker.onKeyDown(keyCode, event)
        }
        if (clipboardOverlay.isVisible) {
            return clipboardOverlay.onKeyDown(keyCode, event)
        }
        if (accentPopup.isVisible) {
            return accentPopup.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                when (currentZone) {
                    FocusZone.KEYBOARD -> {
                        if (suggestionsEnabled && suggestionsRow.suggestions.isNotEmpty()) {
                            requestSuggestionsFocus()
                            return true
                        }
                    }
                    FocusZone.ACTION -> {
                        requestKeyboardFocus()
                        return true
                    }
                    FocusZone.SUGGESTIONS -> {}
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                when (currentZone) {
                    FocusZone.SUGGESTIONS -> {
                        requestKeyboardFocus()
                        return true
                    }
                    FocusZone.KEYBOARD -> {
                        if (actionButton != null && actionButton!!.visibility == View.VISIBLE) {
                            currentZone = FocusZone.ACTION
                            actionButton!!.requestFocus()
                            return true
                        }
                    }
                    FocusZone.ACTION -> {}
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                when (currentZone) {
                    FocusZone.SUGGESTIONS -> {}
                    FocusZone.KEYBOARD -> {
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
        isFocusableInTouchMode = true
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
