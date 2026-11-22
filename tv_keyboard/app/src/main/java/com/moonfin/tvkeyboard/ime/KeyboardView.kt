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

    interface EdgeListener {
        fun onEdgeLeft()
        fun onEdgeRight()
        fun onEdgeUp()
        fun onEdgeDown()
    }

    var listener: Listener? = null
    var edgeListener: EdgeListener? = null
    var feedbackController: FeedbackController? = null

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

    var spaceBarLanguageName: String = ""
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var enlarge: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
        }

    var voiceState: VoiceState = VoiceState.IDLE
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var voiceError: String = ""
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** False in fields where dictation is not allowed; the voice key goes inert. */
    var voiceAvailable: Boolean = true
        set(value) {
            if (field == value) return
            field = value
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

    // Held-key repeat for Delete and cursor keys (Issue #32)
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null
    private var repeatingKeyIndex: Int = -1

    /**
     * Key waiting for key-up. Popup keys and Space type on release, so a hold
     * can open the mini keyboard or the input-method picker first.
     */
    private var pendingReleaseKey: Keyboard.Key? = null

    /** True once the current press was consumed by a long press. */
    private var longPressFired = false

    // Held direction repeat: keeps focus moving while the D-pad stays down.
    private var directionRepeat: Direction? = null
    private var directionRepeatRunnable: Runnable? = null

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
            val baseContentHeight = KeyboardGeometry.contentHeight(
                layoutWidth = kb.minWidth,
                layoutHeight = kb.height,
                viewWidth = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(1),
            )
            val scaledHeight = if (enlarge) (baseContentHeight * 1.25f).toInt() else baseContentHeight
            scaledHeight + paddingTop + paddingBottom
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

    /** Moves focus one key; false when the direction has nowhere to go. */
    private fun moveFocus(direction: Direction): Boolean {
        val next = KeyboardNavigator.move(cells, focusedIndex, direction, cyclic)
        if (next >= 0 && next != focusedIndex) {
            focusedIndex = next
            pressedIndex = -1
            keys.getOrNull(next)?.let { announce(describe(it)) }
            invalidate()
            return true
        }
        if (next == focusedIndex) {
            // At the edge: notify EdgeListener
            when (direction) {
                Direction.UP -> edgeListener?.onEdgeUp()
                Direction.DOWN -> edgeListener?.onEdgeDown()
                Direction.LEFT -> edgeListener?.onEdgeLeft()
                Direction.RIGHT -> edgeListener?.onEdgeRight()
            }
        }
        return false
    }

    /** Publishes the focused key to TalkBack, as state and as a spoken event. */
    private fun announce(text: String) {
        contentDescription = text
        if (isShown) announceForAccessibility(text)
    }

    private fun activate(index: Int) {
        val key = keys.getOrNull(index) ?: return
        if (!voiceAvailable && key.codes.firstOrNull() == KeyCodes.VOICE) return
        feedbackController?.onKeyPress(this)
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

        // Inline voice status banner
        if (voiceState != VoiceState.IDLE) {
            val bannerHeight = 28 * density
            val bannerRect = RectF(
                paddingLeft.toFloat() + 8 * density,
                paddingTop.toFloat() + 4 * density,
                (width - paddingRight).toFloat() - 8 * density,
                paddingTop.toFloat() + 4 * density + bannerHeight
            )
            keyPaint.style = Paint.Style.FILL
            keyPaint.color = if (voiceState == VoiceState.LISTENING) palette.accent else 0xFFB00020.toInt()
            canvas.drawRoundRect(bannerRect, 6 * density, 6 * density, keyPaint)

            textPaint.color = if (voiceState == VoiceState.LISTENING) palette.accentText else 0xFFFFFFFF.toInt()
            textPaint.textSize = 14 * density
            val bannerText = if (voiceState == VoiceState.LISTENING) {
                "🎤 Listening…"
            } else {
                "⚠ " + voiceError.ifEmpty { "Voice input error" }
            }
            val textBaseline = bannerRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(bannerText, bannerRect.centerX(), textBaseline, textPaint)
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
        val isDisabled = !voiceAvailable && key.codes.firstOrNull() == KeyCodes.VOICE
        val background = when {
            isDisabled -> palette.functionKey
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
            isDisabled -> palette.textMuted
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
            KeyCodes.SPACE -> if (spaceBarLanguageName.isNotEmpty()) spaceBarLanguageName else "Space"
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
        KeyAnnouncements.describe(labelFor(key), key.codes.firstOrNull() ?: 0, shifted)

    /** Whether holding this key does anything beyond typing it. */
    private fun holdsSomething(key: Keyboard.Key): Boolean {
        val code = key.codes.firstOrNull() ?: return false
        return code in LONG_PRESS_CODES || AccentPopup.hasPopup(key)
    }

    override fun onDetachedFromWindow() {
        cancelDirectionRepeat()
        cancelRepeatTimer()
        cancelLongPressTimer()
        super.onDetachedFromWindow()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> return holdDirection(Direction.UP, event)
            KeyEvent.KEYCODE_DPAD_DOWN -> return holdDirection(Direction.DOWN, event)
            KeyEvent.KEYCODE_DPAD_LEFT -> return holdDirection(Direction.LEFT, event)
            KeyEvent.KEYCODE_DPAD_RIGHT -> return holdDirection(Direction.RIGHT, event)
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            -> {
                if (event.repeatCount == 0) {
                    cancelDirectionRepeat()
                    longPressFired = false
                    val focusedKey = keys.getOrNull(focusedIndex)
                    if (focusedKey != null) {
                        val primaryCode = focusedKey.codes.firstOrNull() ?: 0
                        when {
                            KeyRepeatPolicy.DEFAULT.isRepeatKey(primaryCode) -> {
                                // First press acts immediately
                                activate(focusedIndex)
                                startRepeatTimer(focusedIndex)
                            }
                            primaryCode == KeyCodes.SPACE -> {
                                // Space types on release, so holding it can open
                                // the input-method picker instead.
                                pendingReleaseKey = focusedKey
                                startLongPressTimer(focusedKey)
                            }
                            primaryCode in LONG_PRESS_CODES -> {
                                startLongPressTimer(focusedKey)
                                activate(focusedIndex)
                            }
                            AccentPopup.hasPopup(focusedKey) -> {
                                pendingReleaseKey = focusedKey
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

    /**
     * Moves on the first press, then keeps moving while the direction is held.
     * Framework repeats are ignored: the timer owns the pace, so a held key
     * never double-steps.
     */
    private fun holdDirection(direction: Direction, event: KeyEvent): Boolean {
        if (event.repeatCount == 0) {
            longPressFired = false
            moveFocus(direction)
            startDirectionRepeat(direction)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            -> {
                cancelDirectionRepeat()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            -> {
                cancelRepeatTimer()
                cancelLongPressTimer()
                val pending = pendingReleaseKey
                pendingReleaseKey = null
                val fired = longPressFired
                longPressFired = false
                if (!fired && pending != null) {
                    val index = keys.indexOf(pending)
                    if (index >= 0) activate(index)
                }
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun startRepeatTimer(keyIndex: Int) {
        cancelRepeatTimer()
        repeatingKeyIndex = keyIndex
        val policy = KeyRepeatPolicy.DEFAULT
        repeatRunnable = object : Runnable {
            override fun run() {
                if (repeatingKeyIndex >= 0) {
                    activate(repeatingKeyIndex)
                    repeatHandler.postDelayed(this, policy.intervalMs)
                }
            }
        }
        repeatHandler.postDelayed(repeatRunnable!!, policy.initialDelayMs)
    }

    private fun cancelRepeatTimer() {
        repeatRunnable?.let { repeatHandler.removeCallbacks(it) }
        repeatRunnable = null
        repeatingKeyIndex = -1
    }

    private fun startDirectionRepeat(direction: Direction) {
        cancelDirectionRepeat()
        directionRepeat = direction
        val runnable = object : Runnable {
            override fun run() {
                val current = directionRepeat ?: return
                val moved = moveFocus(current)
                if (!moved && !cyclic) {
                    // Edge of a row without wrap: no point in ticking on.
                    cancelDirectionRepeat()
                    return
                }
                repeatHandler.postDelayed(this, KeyRepeatPolicy.DEFAULT.intervalMs)
            }
        }
        directionRepeatRunnable = runnable
        repeatHandler.postDelayed(runnable, KeyRepeatPolicy.DEFAULT.initialDelayMs)
    }

    private fun cancelDirectionRepeat() {
        directionRepeatRunnable?.let { repeatHandler.removeCallbacks(it) }
        directionRepeatRunnable = null
        directionRepeat = null
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
        longPressFired = true
        pendingReleaseKey = null
        feedbackController?.onLongPressOpen(this)
        val longPressListener = listener as? LongPressListener
        when (LongPressTarget.of(key.codes.firstOrNull())) {
            LongPressTarget.SHIFT_LOCK -> longPressListener?.onLongPressShift()
            LongPressTarget.INPUT_METHOD_PICKER -> longPressListener?.onLongPressSpace()
            LongPressTarget.LANGUAGE_SETTINGS -> longPressListener?.onLongPressLanguage()
            LongPressTarget.KEY_POPUP -> if (AccentPopup.hasPopup(key)) {
                longPressListener?.onLongPressKey(key)
            }
        }
    }

    interface LongPressListener {
        fun onLongPressShift()

        /** Space held: open the system input-method picker. */
        fun onLongPressSpace()

        /** Language key held: open the system language settings. */
        fun onLongPressLanguage()

        fun onLongPressKey(key: Keyboard.Key)
    }

    private companion object {
        val LONG_PRESS_CODES = setOf(KeyCodes.SHIFT, KeyCodes.LANGUAGE, KeyCodes.SPACE)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val index = hitTest(event.x, event.y)
                if (index < 0) return false
                focusedIndex = index
                pressedIndex = index
                longPressFired = false
                keys.getOrNull(index)?.let { announce(describe(it)) }
                val key = keys.getOrNull(index)
                if (key != null && holdsSomething(key)) {
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
                cancelLongPressTimer()
                cancelRepeatTimer()
                invalidate()
                val fired = longPressFired
                longPressFired = false
                if (!fired && pressed >= 0 && released == pressed) {
                    performClick()
                    activate(pressed)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedIndex = -1
                pendingReleaseKey = null
                longPressFired = false
                cancelLongPressTimer()
                cancelRepeatTimer()
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
                keys.getOrNull(index)?.let { announce(describe(it)) }
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
