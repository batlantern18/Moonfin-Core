package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import com.moonfin.tvkeyboard.theme.KeyboardPalette
import com.moonfin.tvkeyboard.theme.ThemeCatalog
import com.moonfin.tvkeyboard.theme.VisualTheme
import com.moonfin.tvkeyboard.theme.KeyboardStyle

/**
 * D-pad navigable language picker overlay. Shows the enabled languages as a
 * vertical list, highlights the active one, and inserts the chosen index on
 * center / Back to cancel. Navigation is delegated to [LanguagePickerModel] so
 * the edge/cyclic rules are unit-tested off-device.
 */
class LanguagePickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var isVisible: Boolean = false
        private set

    var selectedIndex: Int = -1
        private set

    /** Called with the enabled-language index the user picked. */
    var onLanguageSelected: ((Int) -> Unit)? = null

    /** Called when the user cancels with Back / Escape. */
    var onCancel: (() -> Unit)? = null

    private var languages: List<LanguageLayout> = emptyList()

    /** Wrap up/down at the list ends, mirroring the keyboard's cyclic option. */
    var cyclic: Boolean = false

    /** Theme the overlay is drawn with, so it matches the active keyboard. */
    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) {
            field = value
            invalidate()
        }

    /** Mirror the overlay when the active layout reads right-to-left. */
    var rtl: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            layoutDirection = if (value) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR
            invalidate()
        }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    private val rowHeight: Float get() = 48f * resources.displayMetrics.density

    init {
        visibility = View.GONE
        isFocusable = true
        isFocusableInTouchMode = true
        layoutDirection = LAYOUT_DIRECTION_LTR
    }

    /** Show the picker with the given languages and the currently active index. */
    fun show(languages: List<LanguageLayout>, currentIndex: Int, cyclic: Boolean = this.cyclic, rtl: Boolean = false) {
        this.languages = languages
        this.cyclic = cyclic
        this.rtl = rtl
        selectedIndex = LanguagePickerModel.clamp(currentIndex, languages.size)
        isVisible = true
        visibility = View.VISIBLE
        requestFocus()
        invalidate()
    }

    fun hide() {
        isVisible = false
        visibility = View.GONE
        selectedIndex = -1
        languages = emptyList()
    }

    /** Apply the picker navigation model and redraw. */
    fun move(direction: PickerDirection) {
        if (languages.isEmpty()) return
        selectedIndex = LanguagePickerModel.move(selectedIndex, languages.size, direction, cyclic)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isVisible || languages.isEmpty()) return

        // Dimmed panel behind the list.
        val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.panel }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), panel)

        val totalHeight = rowHeight * languages.size
        val startY = maxOf(0f, (height - totalHeight) / 2)
        val leftPad = 16f * resources.displayMetrics.density

        languages.forEachIndexed { index, language ->
            val top = startY + index * rowHeight
            val bottom = top + rowHeight
            val selected = index == selectedIndex
            val rect = RectF(0f, top, width.toFloat(), bottom)

            val fill = Paint(Paint.ANTI_ALIAS_FLAG)
            fill.color = if (selected) palette.selectedKey else Color.TRANSPARENT
            canvas.drawRect(rect, fill)

            if (selected) {
                val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = palette.focusBorder.color
                    strokeWidth = palette.focusBorder.width * resources.displayMetrics.density
                }
                canvas.drawRect(rect, stroke)
            }

            textPaint.color = if (selected) palette.textOnSelected else palette.textPrimary
            textPaint.textSize = (18f * resources.displayMetrics.density).coerceAtLeast(12f)
            val baseline = top + (bottom - top - (textPaint.ascent() + textPaint.descent())) / 2
            val textX = if (rtl) width - leftPad - textPaint.measureText(language.displayName) else leftPad
            canvas.drawText(language.displayName, textX, baseline, textPaint)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { move(PickerDirection.UP); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { move(PickerDirection.DOWN); return true }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            -> {
                if (selectedIndex in languages.indices) onLanguageSelected?.invoke(selectedIndex)
                hide()
                return true
            }
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE,
            -> { onCancel?.invoke(); hide(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            -> return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /** The language at the currently selected index, or null when nothing is selected. */
    fun getSelectedLanguage(): LanguageLayout? =
        languages.getOrNull(selectedIndex)
}
