package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.inputmethodservice.Keyboard
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.moonfin.tvkeyboard.R
import com.moonfin.tvkeyboard.theme.KeyboardPalette
import com.moonfin.tvkeyboard.theme.KeyboardStyle
import com.moonfin.tvkeyboard.theme.ThemeCatalog
import com.moonfin.tvkeyboard.theme.VisualTheme
import java.util.Locale

/**
 * Draws the whole keyboard on one [Canvas] and keeps a single internal focus
 * index. Android's per-key focus is deliberately not used: a TV remote only
 * has one cursor, and the rendered highlight must match it exactly.
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onKey(key: Keyboard.Key)
    }

    var listener: Listener? = null

    var palette: KeyboardPalette = ThemeCatalog.palette(VisualTheme.MOONFIN, KeyboardStyle.DOCKED)
        set(value) {
            field = value
            invalidate()
        }

    var shifted: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var cyclic: Boolean = false

    var locale: Locale = Locale.getDefault()
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var keyboard: Keyboard? = null
        set(value) {
            if (field === value) return
            field = value
            rebuild()
            invalidate()
        }

    private val density: Float get() = resources.displayMetrics.density

    private var keys: List<Keyboard.Key> = emptyList()
    private var cells: List<NavKey> = emptyList()
    private var focusedIndex = 0
    private var pressedIndex = -1

    // Long-press detection for Shift (Caps Lock), the language picker, and
    // keys that carry a vendored popup layout.
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressKey: Keyboard.Key? = null

    /**
     * Key with a popup that is waiting for key-up. Popup keys type their base
     * character on release, so a hold can open the mini keyboard first.
     */
    private var pendingPopupKey: Keyboard.Key? = null

    private val LONG_PRESS_DELAY_MS = 500L

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    init {
        // The IME window sends D-pad and Back here, so the view must own focus.
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
    }

    private val contentWidth: Int
        get() = (width - paddingLeft - paddingRight).coerceAtLeast(0)

    fun focusedKeyIndex(): Int = focusedIndex

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val measuredWidth = if (widthMode == MeasureSpec.UNSPECIFIED) {
            resources.displayMetrics.widthPixels
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }
        val kb = keyboard
        val desiredHeight = if (kb == null) {
            (220 * density).toInt()
        } else {
            KeyboardGeometry.contentHeight(
                layoutWidth = kb.minWidth,
                layoutHeight = kb.height,
                viewWidth = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(1),
            ) + paddingTop + paddingBottom
        }
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    private fun rebuild() {
        val kb = keyboard
        if (kb == null || contentWidth <= 0) {
            keys = emptyList()
            cells = emptyList()
            return
        }
        keys = kb.keys
        cells = KeyboardGeometry.scaled(
            keys = keys.mapIndexed { index, key ->
                RawKeyRect(index, key.x, key.y, key.width, key.height)
            },
            layoutWidth = kb.minWidth,
            layoutHeight = kb.height,
            viewWidth = contentWidth,
        )
        clampFocus()
    }

    private fun clampFocus() {
        if (keys.isEmpty()) {
            focusedIndex = -1
            return
        }
        focusedIndex = focusedIndex.coerceIn(0, keys.lastIndex)
        contentDescription = describe(keys[focusedIndex])
    }

    private fun moveFocus(direction: Direction) {
        val next = KeyboardNavigator.move(cells, focusedIndex, direction, cyclic)
        if (next >= 0 && next != focusedIndex) {
            focusedIndex = next
            pressedIndex = -1
            contentDescription = keys.getOrNull(next)?.let(::describe)
            invalidate()
        }
    }

    private fun activate(index: Int) {
        val key = keys.getOrNull(index) ?: return
        listener?.onKey(key)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(palette.panel)
        if (cells.isEmpty()) return

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
            drawKey(canvas, key, rect, radius, focused = index == focusedIndex, pressed = index == pressedIndex)
        }
    }

    private fun drawKey(
        canvas: Canvas,
        key: Keyboard.Key,
        rect: RectF,
        radius: Float,
        focused: Boolean,
        pressed: Boolean,
    ) {
        val isFunction = key.codes.any { it < 0 }
        val background = when {
            pressed -> palette.accent
            focused -> palette.selectedKey
            isFunction -> palette.functionKey
            else -> palette.idleKey
        }
        keyPaint.style = Paint.Style.FILL
        keyPaint.color = background
        canvas.drawRoundRect(rect, radius, radius, keyPaint)

        if (focused) {
            keyPaint.style = Paint.Style.STROKE
            keyPaint.color = palette.focusBorder.color
            keyPaint.strokeWidth = palette.focusBorder.width * density
            canvas.drawRoundRect(rect, radius, radius, keyPaint)
        }

        val label = labelFor(key)
        if (label.isEmpty()) return

        val textColor = when {
            pressed -> palette.accentText
            focused -> palette.textOnSelected
            isFunction -> palette.textMuted
            else -> palette.textPrimary
        }
        textPaint.color = textColor
        fitText(label, rect.width() - 8 * density, rect.height() * 0.42f)
        val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }

    /** Shrinks the label until it fits the key instead of clipping it. */
    private fun fitText(label: String, maxWidth: Float, preferredSize: Float) {
        var size = preferredSize.coerceAtLeast(10 * density)
        val minimum = 9 * density
        textPaint.textSize = size
        while (size > minimum && textPaint.measureText(label) > maxWidth) {
            size -= density
            textPaint.textSize = size
        }
    }

    private fun labelFor(key: Keyboard.Key): String {
        val code = key.codes.firstOrNull() ?: return ""
        return when (code) {
            KeyCodes.DELETE -> "Del"
            KeyCodes.SPACE -> "Space"
            KeyCodes.MODE_CHANGE -> "?123"
            KeyCodes.LANGUAGE -> "Lang"
            KeyCodes.LEFT -> "<"
            KeyCodes.RIGHT -> ">"
            KeyCodes.VOICE -> "Mic"
            KeyCodes.CLIPBOARD -> "Clip"
            else -> {
                val label = key.label?.toString().orEmpty()
                TextCommitter.applyShift(label, shifted, locale).toString()
            }
        }
    }

    private fun describe(key: Keyboard.Key): String =
        labelFor(key).ifEmpty { key.codes.firstOrNull()?.toString().orEmpty() }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { moveFocus(Direction.UP); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { moveFocus(Direction.DOWN); return true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { moveFocus(Direction.LEFT); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { moveFocus(Direction.RIGHT); return true }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            -> {
                // Held remotes repeat the event; only the first press counts.
                if (event.repeatCount == 0) {
                    val focusedKey = keys.getOrNull(focusedIndex)
                    if (focusedKey != null) {
                        when {
                            focusedKey.codes.firstOrNull() in LONG_PRESS_CODES -> {
                                // Start the long-press timer (caps lock, language picker).
                                startLongPressTimer(focusedKey)
                                activate(focusedIndex)
                            }
                            AccentPopup.hasPopup(focusedKey) -> {
                                // Type on release unless the hold wins.
                                pendingPopupKey = focusedKey
                                startLongPressTimer(focusedKey)
                            }
                            else -> activate(focusedIndex)
                        }
                    }
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            -> {
                cancelLongPressTimer()
                // A popup key that was released before the hold threshold types
                // its base character exactly once.
                val pending = pendingPopupKey
                pendingPopupKey = null
                if (pending != null) {
                    val index = keys.indexOf(pending)
                    if (index >= 0) activate(index)
                }
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun startLongPressTimer(key: Keyboard.Key) {
        cancelLongPressTimer()
        longPressKey = key
        longPressRunnable = Runnable { onLongPress() }
        longPressHandler.postDelayed(longPressRunnable!!, LONG_PRESS_DELAY_MS)
    }

    private fun cancelLongPressTimer() {
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        longPressRunnable = null
        longPressKey = null
    }

    private fun onLongPress() {
        val key = longPressKey ?: keys.getOrNull(focusedIndex) ?: return
        longPressKey = null
        when (key.codes.firstOrNull()) {
            KeyCodes.SHIFT -> (listener as? LongPressListener)?.onLongPressShift()
            KeyCodes.LANGUAGE, KeyCodes.SPACE -> (listener as? LongPressListener)?.onLongPressLanguage()
            else -> if (AccentPopup.hasPopup(key)) {
                // The hold won: swallow the release so the base character is
                // never typed before the mini keyboard opens.
                pendingPopupKey = null
                (listener as? LongPressListener)?.onLongPressKey(key)
            }
        }
    }

    interface LongPressListener {
        fun onLongPressShift()
        fun onLongPressLanguage()

        /** A key with a vendored popup layout was held. */
        fun onLongPressKey(key: Keyboard.Key)
    }

    private companion object {
        /** Keys that open a long-press action (caps lock, language picker). */
        val LONG_PRESS_CODES = setOf(KeyCodes.SHIFT, KeyCodes.LANGUAGE, KeyCodes.SPACE)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val index = hitTest(event.x, event.y)
                if (index < 0) return false
                focusedIndex = index
                pressedIndex = index
                contentDescription = keys.getOrNull(index)?.let(::describe)
                val key = keys.getOrNull(index)
                if (key != null && AccentPopup.hasPopup(key)) {
                    // A tap types the base character on release; a hold opens
                    // the mini keyboard instead.
                    pendingPopupKey = key
                    startLongPressTimer(key)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressedIndex >= 0 && hitTest(event.x, event.y) != pressedIndex) {
                    pressedIndex = -1
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val released = hitTest(event.x, event.y)
                val pressed = pressedIndex
                pressedIndex = -1
                val pending = pendingPopupKey
                pendingPopupKey = null
                cancelLongPressTimer()
                invalidate()
                if (pressed >= 0 && released == pressed) {
                    val key = keys.getOrNull(pressed)
                    val popupHeld = key != null && AccentPopup.hasPopup(key) && pending == null
                    if (!popupHeld) {
                        performClick()
                        activate(pressed)
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                pendingPopupKey = null
                cancelLongPressTimer()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
            val index = hitTest(event.x, event.y)
            if (index >= 0 && index != focusedIndex) {
                focusedIndex = index
                contentDescription = keys.getOrNull(index)?.let(::describe)
                invalidate()
            }
        }
        return super.onHoverEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun hitTest(x: Float, y: Float): Int =
        KeyboardNavigator.hitTest(
            cells,
            x - paddingLeft,
            y - paddingTop,
        )
}
