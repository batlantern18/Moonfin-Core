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
import com.moonfin.tvkeyboard.R

/**
 * D-pad navigable language picker overlay for the keyboard.
 * Shows enabled languages as a vertically scrollable list.
 * Supports keyboard navigation and touch selection.
 */
class LanguagePickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var isVisible: Boolean = false
        private set

    var selectedIndex: Int = -1
        private set

    var onLanguageSelected: ((Int) -> Unit)? = null
        private set

    var onCancel: (() -> Unit)? = null
        private set

    private var languages: List<LanguageLayout> = emptyList()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 0, 0, 0)
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 18 * resources.displayMetrics.density
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private val buttonHeight: Float = 48f * resources.displayMetrics.density

    init {
        visibility = View.GONE
        isFocusable = true
        isFocusableInTouchMode = true
    }

    /**
     * Show the language picker with the given languages and current selection.
     */
    fun show(languages: List<LanguageLayout>, currentIndex: Int) {
        this.languages = languages
        isVisible = true
        visibility = View.VISIBLE
        selectedIndex = currentIndex.coerceIn(0, (languages.size - 1).coerceAtLeast(0))
        invalidate()
        requestFocus()
    }

    /**
     * Hide the language picker.
     */
    fun hide() {
        isVisible = false
        visibility = View.GONE
        selectedIndex = -1
        languages = emptyList()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isVisible || languages.isEmpty()) return

        // Draw background overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw each language
        val totalHeight = buttonHeight * languages.size
        val startY = maxOf(0f, (height - totalHeight) / 2)
        
        languages.forEachIndexed { index, lang ->
            val top = startY + index * buttonHeight
            val bottom = top + buttonHeight
            
            // Draw button background
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (index == selectedIndex) Color.YELLOW else Color.TRANSPARENT
            }
            canvas.drawRect(0f, top, width.toFloat(), bottom, bgPaint)
            
            // Draw text
            val baseline = top + buttonHeight / 2 - (textPaint.ascent() + textPaint.descent()) / 2
            canvas.drawText(lang.displayName, width / 2f, baseline, textPaint)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (selectedIndex > 0) {
                    selectedIndex--
                    invalidate()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (selectedIndex < languages.size - 1) {
                    selectedIndex++
                    invalidate()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                onLanguageSelected?.invoke(selectedIndex)
                hide()
                return true
            }
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE -> {
                onCancel?.invoke()
                hide()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * Get the language at the currently selected index.
     */
    fun getSelectedLanguage(): LanguageLayout? = 
        if (selectedIndex in languages.indices) languages[selectedIndex] else null
}