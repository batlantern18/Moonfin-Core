package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.moonfin.tvkeyboard.theme.KeyboardPalette
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.ThemeCatalog
import com.moonfin.tvkeyboard.theme.VisualTheme

/**
 * The line above the keys that repeats the field's text, with a caret at the
 * cursor. Docked style only; it collapses to zero height when there is nothing
 * to show, so the keys take the space back.
 */
class EchoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) {
            field = value
            invalidate()
        }

    var echo: EchoState = EchoState()
        set(value) {
            if (field == value) return
            field = value
            visibility = if (value.visible) VISIBLE else GONE
            requestLayout()
            invalidate()
        }

    private val density: Float get() = resources.displayMetrics.density

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val caretPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    init {
        visibility = GONE
        val horizontal = (28 * density).toInt()
        setPadding(horizontal, 0, horizontal, 0)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val measuredWidth = if (widthMode == MeasureSpec.UNSPECIFIED) {
            resources.displayMetrics.widthPixels
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }
        val desiredHeight = if (echo.visible) lineHeight() + paddingTop + paddingBottom else 0
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!echo.visible) return

        val size = 24 * density
        textPaint.textSize = size
        textPaint.color = palette.textPrimary

        val left = paddingLeft.toFloat()
        val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        val available = (width - paddingLeft - paddingRight - 6 * density).coerceAtLeast(1f)

        var clipped = echo.displayText
        while (clipped.isNotEmpty() && textPaint.measureText(clipped) > available) {
            clipped = clipped.dropLast(1)
        }
        canvas.drawText(clipped, left, baseline, textPaint)

        val prefix = echo.displayText.substring(0, echo.displayCaret.coerceIn(0, echo.displayText.length))
        val caretX = (left + textPaint.measureText(prefix)).coerceAtMost(left + available)

        caretPaint.color = palette.accent
        canvas.drawRect(
            caretX,
            baseline + textPaint.ascent(),
            caretX + (2 * density).coerceAtLeast(2f),
            baseline + textPaint.descent(),
            caretPaint,
        )
    }

    private fun lineHeight(): Int = (34 * density).toInt()
}
