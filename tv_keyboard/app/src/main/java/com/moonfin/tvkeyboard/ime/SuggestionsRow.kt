package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import com.moonfin.tvkeyboard.R
import com.moonfin.tvkeyboard.theme.KeyboardPalette
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.ThemeCatalog
import com.moonfin.tvkeyboard.theme.VisualTheme

/**
 * Horizontal chip row for suggestions. Navigable with D-pad left/right.
 * Center/Enter selects the focused suggestion. Down returns focus to keyboard.
 */
class SuggestionsRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onSuggestionSelected(suggestion: Suggestion, index: Int)
        fun onFocusLeft()
        fun onFocusRight()
        fun onFocusDown()
    }

    var listener: Listener? = null

    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) {
            field = value
            invalidate()
        }

    var suggestions: List<Suggestion> = emptyList()
        set(value) {
            field = value
            focusedIndex = if (value.isEmpty()) -1 else 0
            invalidate()
        }

    var suggestionsVisible: Boolean = true
        set(value) {
            field = value
            if (!value) focusedIndex = -1
            invalidate()
        }

    private val density: Float get() = resources.displayMetrics.density

    private var focusedIndex = -1
    private var pressedIndex = -1

    private val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private val chipHeight: Float get() = (44 * density).coerceAtLeast(1f)
    private val chipPadding: Float get() = (12 * density).coerceAtLeast(1f)
    private val chipSpacing: Float get() = (8 * density).coerceAtLeast(1f)
    private val chipRadius: Float get() = (8 * density).coerceAtLeast(1f)

    private var chipRects: List<RectF> = emptyList()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
    }

    fun focusedIndex(): Int = focusedIndex

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val measuredWidth = if (widthMode == MeasureSpec.UNSPECIFIED) {
            resources.displayMetrics.widthPixels
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }

        val desiredHeight = if (suggestionsVisible && suggestions.isNotEmpty()) {
            chipHeight.toInt() + paddingTop + paddingBottom
        } else {
            0
        }
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildChips()
    }

    private fun rebuildChips() {
        if (suggestions.isEmpty() || !suggestionsVisible) {
            chipRects = emptyList()
            return
        }

        val rects = mutableListOf<RectF>()
        var currentX = paddingLeft.toFloat()
        val y = paddingTop.toFloat()

        textPaint.textSize = 14 * density

        for (suggestion in suggestions) {
            val textWidth = textPaint.measureText(suggestion.text)
            val chipWidth = textWidth + chipPadding * 2
            val rect = RectF(currentX, y, currentX + chipWidth, y + chipHeight)
            rects.add(rect)
            currentX += chipWidth + chipSpacing
        }

        chipRects = rects
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!suggestionsVisible || suggestions.isEmpty() || chipRects.isEmpty()) return

        for (i in suggestions.indices) {
            if (i >= chipRects.size) break
            val rect = chipRects[i]
            val isFocused = (i == focusedIndex) && hasFocus()
            val isPressed = (i == pressedIndex)

            val background = when {
                isPressed -> palette.accent
                isFocused -> palette.selectedKey
                else -> palette.chipBackground
            }

            chipPaint.color = background
            canvas.drawRoundRect(rect, chipRadius, chipRadius, chipPaint)

            if (isFocused) {
                borderPaint.color = palette.focusBorder.color
                borderPaint.strokeWidth = palette.focusBorder.width * density
                canvas.drawRoundRect(rect, chipRadius, chipRadius, borderPaint)
            } else if (palette.chipBorder != null) {
                borderPaint.color = palette.chipBorder!!.color
                borderPaint.strokeWidth = palette.chipBorder!!.width * density
                canvas.drawRoundRect(rect, chipRadius, chipRadius, borderPaint)
            }

            val textColor = when {
                isPressed -> palette.accentText
                isFocused -> palette.textOnSelected
                else -> palette.textPrimary
            }
            textPaint.color = textColor
            textPaint.textSize = 14 * density

            val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(suggestions[i].text, rect.centerX(), baseline, textPaint)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusedIndex > 0) {
                    focusedIndex--
                    pressedIndex = -1
                    invalidate()
                } else {
                    listener?.onFocusLeft()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusedIndex < suggestions.lastIndex) {
                    focusedIndex++
                    pressedIndex = -1
                    invalidate()
                } else {
                    listener?.onFocusRight()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (focusedIndex >= 0 && focusedIndex < suggestions.size) {
                    val suggestion = suggestions[focusedIndex]
                    listener?.onSuggestionSelected(suggestion, focusedIndex)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_BACK -> {
                listener?.onFocusDown()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                val index = hitTest(event.x, event.y)
                if (index >= 0) {
                    focusedIndex = index
                    pressedIndex = index
                    invalidate()
                    return true
                }
                return false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                if (pressedIndex >= 0 && hitTest(event.x, event.y) != pressedIndex) {
                    pressedIndex = -1
                    invalidate()
                }
                return true
            }
            android.view.MotionEvent.ACTION_UP -> {
                val released = hitTest(event.x, event.y)
                val pressed = pressedIndex
                pressedIndex = -1
                invalidate()
                if (pressed >= 0 && released == pressed && pressed < suggestions.size) {
                    listener?.onSuggestionSelected(suggestions[pressed], pressed)
                }
                return true
            }
            android.view.MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clear() {
        suggestions = emptyList()
        focusedIndex = -1
        chipRects = emptyList()
        invalidate()
    }

    private fun hitTest(x: Float, y: Float): Int {
        for (i in chipRects.indices) {
            if (chipRects[i].contains(x, y)) {
                return i
            }
        }
        return -1
    }
}
