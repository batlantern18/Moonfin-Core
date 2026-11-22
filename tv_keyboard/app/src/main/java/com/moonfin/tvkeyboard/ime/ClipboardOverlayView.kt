package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import com.moonfin.tvkeyboard.theme.KeyboardPalette
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.ThemeCatalog
import com.moonfin.tvkeyboard.theme.VisualTheme

/**
 * D-pad navigable list of recent clipboard clips. Up/down moves selection;
 * center/Enter pastes; Back dismisses without pasting.
 */
class ClipboardOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) { field = value; invalidate() }

    var onPaste: ((ClipboardController.ClipItem) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    private var clips: List<ClipboardController.ClipItem> = emptyList()
    private var focusedIndex = 0
    private val density: Float get() = resources.displayMetrics.density

    private val rowHeight: Float get() = 56 * density
    private val padding: Float get() = 16 * density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    var isVisible: Boolean = false
        private set

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        visibility = View.GONE
    }

    fun show(items: List<ClipboardController.ClipItem>) {
        clips = items
        focusedIndex = 0
        isVisible = true
        visibility = View.VISIBLE
        requestFocus()
        requestLayout()
        invalidate()
    }

    fun hide() {
        isVisible = false
        visibility = View.GONE
        clips = emptyList()
        focusedIndex = 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val rows = clips.size.coerceAtLeast(1)
        val h = (rowHeight * rows + padding * 2).toInt()
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isVisible) return

        bgPaint.color = palette.panel
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (clips.isEmpty()) {
            textPaint.textSize = 18 * density
            textPaint.color = palette.textMuted
            val msg = "Clipboard empty — copy text in another app first"
            canvas.drawText(msg, padding, padding + rowHeight / 2, textPaint)
            return
        }

        val radius = 8 * density
        clips.forEachIndexed { i, clip ->
            val top = padding + i * rowHeight
            val bottom = top + rowHeight
            val rect = RectF(padding / 2, top + 4 * density, width - padding / 2, bottom - 4 * density)
            val focused = i == focusedIndex

            bgPaint.color = if (focused) palette.selectedKey else palette.idleKey
            canvas.drawRoundRect(rect, radius, radius, bgPaint)

            if (focused) {
                borderPaint.color = palette.focusBorder.color
                borderPaint.strokeWidth = palette.focusBorder.width * density
                canvas.drawRoundRect(rect, radius, radius, borderPaint)
            }

            textPaint.textSize = 16 * density
            textPaint.color = if (focused) palette.textOnSelected else palette.textPrimary
            val maxW = (width - padding * 2).coerceAtLeast(1f)
            var display = clip.text.replace('\n', ' ')
            while (display.length > 1 && textPaint.measureText(display) > maxW) {
                display = display.dropLast(4) + "…"
            }
            val textY = top + rowHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(display, padding, textY, textPaint)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (focusedIndex > 0) { focusedIndex--; invalidate() }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (focusedIndex < clips.size - 1) { focusedIndex++; invalidate() }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event.repeatCount == 0) {
                    clips.getOrNull(focusedIndex)?.let { item ->
                        hide()
                        onPaste?.invoke(item)
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                hide()
                onDismiss?.invoke()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
