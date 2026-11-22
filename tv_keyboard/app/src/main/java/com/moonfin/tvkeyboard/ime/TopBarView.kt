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
 * The strip above the keys: one module with four contents — word suggestions,
 * email domains, the digit and symbol row for password fields, and the voice
 * plate. Which one is shown comes from [TopBarContent]; this view never decides
 * it. Navigable with the D-pad left/right; Down or Back returns to the keys.
 */
class TopBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onItemSelected(item: Suggestion, index: Int)
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

    var content: TopBarContent = TopBarContent.EMPTY
        set(value) {
            if (field == value) return
            field = value
            focusedIndex = if (value.items.isEmpty()) -1 else 0
            chipRects = emptyList()
            contentDescription = if (value.kind == TopBarKind.VOICE) voiceLabel(value) else null
            requestLayout()
            invalidate()
        }

    private val items: List<Suggestion> get() = content.items

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

        // Nothing to show collapses the strip, so the keys take the space back.
        val desiredHeight = if (content.visible) {
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
        if (content.kind == TopBarKind.VOICE || items.isEmpty()) {
            chipRects = emptyList()
            return
        }

        val rects = mutableListOf<RectF>()
        var currentX = paddingLeft.toFloat()
        val y = paddingTop.toFloat()

        textPaint.textSize = 14 * density

        for (item in items) {
            val textWidth = textPaint.measureText(item.text)
            val chipWidth = textWidth + chipPadding * 2
            rects.add(RectF(currentX, y, currentX + chipWidth, y + chipHeight))
            currentX += chipWidth + chipSpacing
        }

        chipRects = rects
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!content.visible) return
        if (content.kind == TopBarKind.VOICE) {
            drawVoicePlate(canvas)
            return
        }
        if (chipRects.isEmpty()) return

        for (i in items.indices) {
            if (i >= chipRects.size) break
            val rect = chipRects[i]
            val isFocused = (i == focusedIndex) && hasFocus()
            val isPressed = (i == pressedIndex)

            chipPaint.color = when {
                isPressed -> palette.accent
                isFocused -> palette.selectedKey
                else -> palette.chipBackground
            }
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

            textPaint.color = when {
                isPressed -> palette.accentText
                isFocused -> palette.textOnSelected
                else -> palette.textPrimary
            }
            textPaint.textSize = 14 * density

            val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(items[i].text, rect.centerX(), baseline, textPaint)
        }
    }

    /** The listening and error states take the whole strip, not a key's canvas. */
    private fun drawVoicePlate(canvas: Canvas) {
        val rect = RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            paddingTop + chipHeight,
        )
        val listening = content.voiceState == VoiceState.LISTENING
        chipPaint.color = if (listening) palette.accent else ERROR_PLATE
        canvas.drawRoundRect(rect, chipRadius, chipRadius, chipPaint)

        textPaint.color = if (listening) palette.accentText else 0xFFFFFFFF.toInt()
        textPaint.textSize = 14 * density
        val label = voiceLabel(content)
        val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }

    private fun voiceLabel(content: TopBarContent): String = when (content.voiceState) {
        VoiceState.LISTENING -> context.getString(R.string.voice_listening)
        VoiceState.ERROR ->
            "⚠ " + content.voiceError.ifEmpty { context.getString(R.string.voice_unavailable) }
        VoiceState.IDLE -> ""
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
                if (focusedIndex < items.lastIndex) {
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
                if (focusedIndex >= 0 && focusedIndex < items.size) {
                    listener?.onItemSelected(items[focusedIndex], focusedIndex)
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
                if (pressed >= 0 && released == pressed && pressed < items.size) {
                    listener?.onItemSelected(items[pressed], pressed)
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
        content = TopBarContent.EMPTY
        chipRects = emptyList()
        invalidate()
    }

    private fun hitTest(x: Float, y: Float): Int {
        for (i in chipRects.indices) {
            if (chipRects[i].contains(x, y)) return i
        }
        return -1
    }

    private companion object {
        const val ERROR_PLATE = 0xFFB00020.toInt()
    }
}
