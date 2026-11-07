package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.inputmethodservice.Keyboard
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import com.moonfin.tvkeyboard.theme.KeyboardPalette
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.ThemeCatalog
import java.util.Locale

/**
 * Mini keyboard of accent choices shown above the focused key when a letter is
 * long-pressed. Draws the popup layout on one [Canvas] with a single internal
 * focus index, mirroring [KeyboardView]. Navigation is the same geometric
 * [KeyboardNavigator], so D-pad movement, wrap, and edges behave identically to
 * the main board.
 */
class AccentPopupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var isVisible: Boolean = false
        private set

    var focusedIndex: Int = -1
        private set

    /** Invoked with the chosen key's code and (shift-applied) label text. */
    var onSelected: ((String) -> Unit)? = null

    /** Invoked when the user dismisses the popup without selecting (Back / past the bottom). */
    var onCancel: (() -> Unit)? = null

    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) {
            field = value
            invalidate()
        }

    /** Wrap focus at the popup edges, mirroring the keyboard's cyclic option. */
    var cyclic: Boolean = false

    private var keyboard: Keyboard? = null
    private var keys: List<Keyboard.Key> = emptyList()
    private var cells: List<NavKey> = emptyList()

    private val density: Float get() = resources.displayMetrics.density

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    init {
        visibility = View.GONE
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private val contentWidth: Int
        get() = (width - paddingLeft - paddingRight).coerceAtLeast(0)

    /** Open the popup for [resId], rendering and focusing its first key. */
    fun show(resId: Int, shifted: Boolean, locale: Locale) {
        this.shifted = shifted
        this.locale = locale
        keyboard = Keyboard(context, resId)
        val kb = keyboard!!
        keys = kb.keys
        cells = KeyboardGeometry.scaled(
            keys = keys.mapIndexed { index, key ->
                RawKeyRect(index, key.x, key.y, key.width, key.height)
            },
            layoutWidth = kb.minWidth,
            layoutHeight = kb.height,
            viewWidth = contentWidth.coerceAtLeast(1),
        )
        focusedIndex = cells.firstOrNull()?.index ?: 0
        isVisible = true
        visibility = View.VISIBLE
        requestFocus()
        invalidate()
    }

    fun hide() {
        isVisible = false
        visibility = View.GONE
        keyboard = null
        keys = emptyList()
        cells = emptyList()
        focusedIndex = -1
    }

    private fun move(direction: Direction): Boolean {
        val next = KeyboardNavigator.move(cells, focusedIndex, direction, cyclic)
        if (next < 0 || next == focusedIndex) return false
        focusedIndex = next
        invalidate()
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val kb = keyboard
        val desired = if (kb == null) {
            (120 * density).toInt()
        } else {
            KeyboardGeometry.contentHeight(
                layoutWidth = kb.minWidth,
                layoutHeight = kb.height,
                viewWidth = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(1),
            ) + paddingTop + paddingBottom
        }
        setMeasuredDimension(measuredWidth, resolveSize(desired, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val kb = keyboard ?: return
        cells = KeyboardGeometry.scaled(
            keys = keys.mapIndexed { index, key ->
                RawKeyRect(index, key.x, key.y, key.width, key.height)
            },
            layoutWidth = kb.minWidth,
            layoutHeight = kb.height,
            viewWidth = contentWidth.coerceAtLeast(1),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isVisible || cells.isEmpty()) return

        canvas.drawColor(palette.scrim)
        val kb = keyboard ?: return
        val scale = contentWidth.toFloat() / kb.minWidth.coerceAtLeast(1)
        val insetX = resources.getDimension(R.dimen.keyboard_horizontal_gap) * scale / 2f
        val insetY = resources.getDimension(R.dimen.keyboard_vertical_gap) * scale / 2f
        val radius = (palette.keyRadius * density).coerceAtLeast(0f)

        cells.forEachIndexed { index, cell ->
            val key = keys.getOrNull(index) ?: return@forEachIndexed
            val rect = RectF(
                paddingLeft + cell.left + insetX,
                paddingTop + cell.top + insetY,
                paddingLeft + cell.right - insetX,
                paddingTop + cell.bottom - insetY,
            )
            val focused = index == focusedIndex
            keyPaint.style = Paint.Style.FILL
            keyPaint.color = if (focused) palette.selectedKey else palette.idleKey
            canvas.drawRoundRect(rect, radius, radius, keyPaint)

            if (focused) {
                keyPaint.style = Paint.Style.STROKE
                keyPaint.color = palette.focusBorder.color
                keyPaint.strokeWidth = palette.focusBorder.width * density
                canvas.drawRoundRect(rect, radius, radius, keyPaint)
            }

            val label = key.label?.toString().orEmpty()
            if (label.isNotEmpty()) {
                textPaint.color = if (focused) palette.textOnSelected else palette.textPrimary
                textPaint.textSize = (rect.height() * 0.5f).coerceAtLeast(10f * density)
                val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(label, rect.centerX(), baseline, textPaint)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> return move(Direction.UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Past the bottom row closes the popup without inserting.
                if (!move(Direction.DOWN)) {
                    onCancel?.invoke()
                    hide()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> return move(Direction.LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> return move(Direction.RIGHT)
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            -> {
                if (event.repeatCount == 0) selectFocused()
                return true
            }
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE,
            -> { onCancel?.invoke(); hide(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun selectFocused() {
        val key = keys.getOrNull(focusedIndex) ?: return
        onSelected?.invoke(AccentPopup.textFor(key, shifted, locale))
        hide()
    }

    private var shifted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private var locale: Locale = Locale.getDefault()

    override fun onKeyUp
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            -> return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
