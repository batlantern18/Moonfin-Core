package com.moonfin.tvkeyboard.theme

import android.graphics.Color
import kotlin.math.pow

/**
 * The four built-in visual themes Moonfin ships in `packages/design`.
 *
 * These identifiers and colour values are copied from the Dart theme specs so
 * the keyboard looks like the rest of the app instead of inventing a palette.
 */
enum class VisualTheme(
    val id: String,
    val displayName: String,
    val isGlass: Boolean,
    val isPixel: Boolean,
) {
    MOONFIN("moonfin", "Moonfin", isGlass = false, isPixel = false),
    NEON_PULSE("neon_pulse", "Neon Pulse", isGlass = false, isPixel = false),
    GLASS("glass", "Glass", isGlass = true, isPixel = false),
    EIGHTBIT_HERO("8bit_hero", "8-bit Hero", isGlass = false, isPixel = true),
    ;

    companion object {
        fun fromId(id: String?): VisualTheme =
            entries.firstOrNull { it.id == id } ?: MOONFIN
    }
}

/**
 * The three in-app keyboard treatments Moonfin exposes in Settings › General ›
 * Keyboard Style. `CLASSIC` uses the active theme's button tokens directly,
 * `GLASS` is the frosted near-white selection, `DOCKED` is the TV default with
 * an accent-filled selection.
 */
enum class KeyboardStyle(val id: String, val displayName: String) {
    CLASSIC("classic", "Classic"),
    GLASS("glass", "Glass"),
    DOCKED("docked", "Docked"),
    ;

    companion object {
        fun fromId(id: String?): KeyboardStyle =
            entries.firstOrNull { it.id == id } ?: DOCKED
    }
}

data class Border(val color: Int, val width: Float)

data class Glow(
    val color: Int,
    val blur: Float = 0f,
    val spread: Float = 0f,
    val dx: Float = 0f,
    val dy: Float = 0f,
)

/** Mirrors `ThemeColorTokens` + `ThemeBorderTokens` from `moonfin_design`. */
data class ThemeTokens(
    val theme: VisualTheme,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val scrim: Int,
    val accent: Int,
    val onAccent: Int,
    val buttonNormal: Int,
    val buttonFocused: Int,
    val buttonDisabled: Int,
    val buttonActive: Int,
    val onButtonNormal: Int,
    val onButtonFocused: Int,
    val onButtonDisabled: Int,
    val inputBackground: Int,
    val inputFocused: Int,
    val inputBorder: Int,
    val inputBorderFocused: Int,
    val cardBorder: Border,
    val chipBorder: Border,
    val focusBorder: Border,
    val cardRadius: Float,
    val chipRadius: Float,
    val chipBackground: Int,
    val focusGlow: List<Glow>,
) {
    val isGlass: Boolean get() = theme.isGlass
    val isPixel: Boolean get() = theme.isPixel
}

/** The resolved look of one keyboard style under one theme. */
data class KeyboardPalette(
    val scrim: Int,
    val panel: Int,
    val panelBorder: Border?,
    val panelRadius: Float,
    val idleKey: Int,
    val idleKeyBorder: Border?,
    val functionKey: Int,
    val selectedKey: Int,
    val selectedKeyBorder: Border?,
    val textPrimary: Int,
    val textMuted: Int,
    val textOnSelected: Int,
    val chipBackground: Int,
    val chipBorder: Border?,
    val chipRadius: Float,
    val focusGlow: List<Glow>,
    val accent: Int,
    val accentText: Int,
    val focusBorder: Border,
    val keyHeight: Float,
    val rowGap: Float,
    val keyRadius: Float,
    val selectedScale: Float,
    val glass: Boolean,
    val isPixel: Boolean,
)

object ThemeCatalog {

    private fun hex(value: Long): Int = value.toInt()

    fun tokens(theme: VisualTheme): ThemeTokens = when (theme) {
        VisualTheme.MOONFIN -> ThemeTokens(
            theme = theme,
            background = hex(0xFF101010),
            onBackground = hex(0xFFFFFFFF),
            surface = hex(0xFF1A1A1A),
            onSurface = hex(0xFFFFFFFF),
            surfaceVariant = hex(0xFF252525),
            scrim = hex(0xCC000000),
            accent = hex(0xFF00A4DC),
            onAccent = hex(0xFFFFFFFF),
            buttonNormal = hex(0xFF2A2A2A),
            buttonFocused = hex(0xFFFFFFFF),
            buttonDisabled = hex(0xFF1E1E1E),
            buttonActive = hex(0xFF3A3A3A),
            onButtonNormal = hex(0xFFFFFFFF),
            onButtonFocused = hex(0xFF00A4DC),
            onButtonDisabled = hex(0xFF666666),
            inputBackground = hex(0xFF2A2A2A),
            inputFocused = hex(0xFF3A3A3A),
            inputBorder = hex(0xFF404040),
            inputBorderFocused = hex(0xFF00A4DC),
            cardBorder = Border(hex(0x00000000), 0f),
            chipBorder = Border(hex(0x558EC8F0), 1f),
            focusBorder = Border(hex(0xFF00A4DC), 2f),
            cardRadius = 8f,
            chipRadius = 999f,
            chipBackground = hex(0x1F8EC8F0),
            focusGlow = emptyList(),
        )

        VisualTheme.NEON_PULSE -> ThemeTokens(
            theme = theme,
            background = hex(0xFF0B0420),
            onBackground = hex(0xFF00E5FF),
            surface = hex(0xCC1E0A3F),
            onSurface = hex(0xFF00E5FF),
            surfaceVariant = hex(0xCC1E0A3F),
            scrim = hex(0xCC0B0420),
            accent = hex(0xFFFF2E92),
            onAccent = hex(0xFFFFFFFF),
            buttonNormal = hex(0x00000000),
            buttonFocused = hex(0xFF00E5FF),
            buttonDisabled = hex(0x22FFFFFF),
            buttonActive = hex(0x33FF2E92),
            onButtonNormal = hex(0xFFFF2E92),
            onButtonFocused = hex(0xFFFFFFFF),
            onButtonDisabled = hex(0xAAFFFFFF),
            inputBackground = hex(0x331E0A3F),
            inputFocused = hex(0x441E0A3F),
            inputBorder = hex(0x66FF2E92),
            inputBorderFocused = hex(0xFFFF2E92),
            cardBorder = Border(hex(0x66FF2E92), 1f),
            chipBorder = Border(hex(0xCCFF2E92), 1.2f),
            focusBorder = Border(hex(0xFFFF2E92), 1.4f),
            cardRadius = 10f,
            chipRadius = 8f,
            chipBackground = hex(0x00000000),
            focusGlow = listOf(
                Glow(hex(0x99FF2E92), blur = 8f, spread = 0.5f),
                Glow(hex(0x6600E5FF), blur = 5f),
            ),
        )

        VisualTheme.GLASS -> ThemeTokens(
            theme = theme,
            background = hex(0xCC07090F),
            onBackground = hex(0xFFFFFFFF),
            surface = hex(0xD90E1117),
            onSurface = hex(0xFFFFFFFF),
            surfaceVariant = hex(0x29FFFFFF),
            scrim = hex(0x99060810),
            accent = hex(0xFF0A84FF),
            onAccent = hex(0xFFFFFFFF),
            buttonNormal = hex(0x1FFFFFFF),
            buttonFocused = hex(0xF2FFFFFF),
            buttonDisabled = hex(0x14FFFFFF),
            buttonActive = hex(0x3DFFFFFF),
            onButtonNormal = hex(0xFFFFFFFF),
            onButtonFocused = hex(0xFF0A0A12),
            onButtonDisabled = hex(0x66FFFFFF),
            inputBackground = hex(0x1FFFFFFF),
            inputFocused = hex(0x33FFFFFF),
            inputBorder = hex(0x33FFFFFF),
            inputBorderFocused = hex(0xFF0A84FF),
            cardBorder = Border(hex(0x29FFFFFF), 1f),
            chipBorder = Border(hex(0x3DFFFFFF), 1f),
            focusBorder = Border(hex(0xFFFFFFFF), 2f),
            cardRadius = 14f,
            chipRadius = 999f,
            chipBackground = hex(0x1FFFFFFF),
            focusGlow = listOf(
                Glow(hex(0x40FFFFFF), blur = 12f),
                Glow(hex(0x330A84FF), blur = 22f, spread = 1f),
            ),
        )

        VisualTheme.EIGHTBIT_HERO -> ThemeTokens(
            theme = theme,
            background = hex(0xFF1A1C2C),
            onBackground = hex(0xFFF4F4F4),
            surface = hex(0xFF333C57),
            onSurface = hex(0xFFF4F4F4),
            surfaceVariant = hex(0xFF566C86),
            scrim = hex(0xCC1A1C2C),
            accent = hex(0xFFEF7D57),
            onAccent = hex(0xFF1A1C2C),
            buttonNormal = hex(0xFF29366F),
            buttonFocused = hex(0xFFFFCD75),
            buttonDisabled = hex(0xFF333C57),
            buttonActive = hex(0xFF41A6F6),
            onButtonNormal = hex(0xFFF4F4F4),
            onButtonFocused = hex(0xFF1A1C2C),
            onButtonDisabled = hex(0xFF566C86),
            inputBackground = hex(0xFF333C57),
            inputFocused = hex(0xFF3B5DC9),
            inputBorder = hex(0xFF566C86),
            inputBorderFocused = hex(0xFFFFCD75),
            cardBorder = Border(hex(0xFF566C86), 2f),
            chipBorder = Border(hex(0xFFF4F4F4), 2f),
            focusBorder = Border(hex(0xFFFFCD75), 3f),
            cardRadius = 0f,
            chipRadius = 0f,
            chipBackground = hex(0xFF29366F),
            focusGlow = listOf(Glow(hex(0x99FFCD75), dx = 4f, dy = 4f)),
        )
    }

    fun palette(theme: VisualTheme, style: KeyboardStyle): KeyboardPalette {
        val tokens = tokens(theme)
        val accent = tokens.accent
        val accentText = lerpColor(accent, Color.WHITE, 0.55f)

        return when (style) {
            KeyboardStyle.GLASS -> KeyboardPalette(
                scrim = hex(0x6B06080C),
                panel = hex(0xE01C1E24),
                panelBorder = Border(hex(0x17FFFFFF), 1f),
                panelRadius = 14f,
                idleKey = hex(0x13FFFFFF),
                idleKeyBorder = null,
                functionKey = hex(0x09FFFFFF),
                selectedKey = hex(0xFFF5F7FA),
                selectedKeyBorder = null,
                textPrimary = hex(0xEBFFFFFF),
                textMuted = hex(0x9EFFFFFF),
                textOnSelected = hex(0xFF0B0D12),
                chipBackground = hex(0x0FFFFFFF),
                chipBorder = null,
                chipRadius = 10f,
                focusGlow = listOf(
                    Glow(hex(0x8C000000), blur = 26f, dy = 10f),
                    Glow(hex(0x29FFFFFF), blur = 28f),
                ),
                accent = accent,
                accentText = accentText,
                focusBorder = Border(hex(0xFFFFFFFF), 2f),
                keyHeight = 44f,
                rowGap = 8f,
                keyRadius = 12f,
                selectedScale = 1.12f,
                glass = true,
                isPixel = false,
            )

            KeyboardStyle.DOCKED -> KeyboardPalette(
                scrim = hex(0x8C040508),
                panel = hex(0xF70B0C10),
                panelBorder = Border(hex(0x12FFFFFF), 1f),
                panelRadius = 14f,
                idleKey = hex(0x0FFFFFFF),
                idleKeyBorder = null,
                functionKey = hex(0x0DFFFFFF),
                selectedKey = accent,
                selectedKeyBorder = null,
                textPrimary = hex(0xD1FFFFFF),
                textMuted = hex(0x73FFFFFF),
                textOnSelected = if (luminance(accent) > 0.19) hex(0xFF04131A) else Color.WHITE,
                chipBackground = Color.TRANSPARENT,
                chipBorder = null,
                chipRadius = 10f,
                focusGlow = listOf(
                    Glow(withAlpha(accent, 0.18f), spread = 5f),
                    Glow(withAlpha(accent, 0.28f), blur = 30f, dy = 12f),
                ),
                accent = accent,
                accentText = accentText,
                focusBorder = Border(accent, tokens.focusBorder.width),
                keyHeight = 40f,
                rowGap = 4f,
                keyRadius = 12f,
                selectedScale = 1.12f,
                glass = false,
                isPixel = false,
            )

            KeyboardStyle.CLASSIC -> {
                val idleKey = multiplyAlpha(tokens.surfaceVariant, 0.92f)
                KeyboardPalette(
                    scrim = Color.TRANSPARENT,
                    panel = tokens.surface,
                    panelBorder = tokens.cardBorder.takeIf { it.width > 0f },
                    panelRadius = tokens.cardRadius,
                    idleKey = idleKey,
                    idleKeyBorder = tokens.chipBorder.takeIf { it.width > 0f },
                    functionKey = idleKey,
                    selectedKey = tokens.buttonFocused,
                    selectedKeyBorder = tokens.focusBorder,
                    textPrimary = tokens.onSurface,
                    textMuted = withAlpha(tokens.onSurface, 0.64f),
                    textOnSelected = tokens.onButtonFocused,
                    chipBackground = tokens.chipBackground,
                    chipBorder = tokens.chipBorder.takeIf { it.width > 0f },
                    chipRadius = tokens.chipRadius,
                    focusGlow = tokens.focusGlow,
                    accent = accent,
                    accentText = accentText,
                    focusBorder = tokens.focusBorder,
                    keyHeight = 40f,
                    rowGap = 4f,
                    keyRadius = if (tokens.isPixel) 0f else minOf(tokens.chipRadius, 14f),
                    selectedScale = if (tokens.isPixel) 1f else 1.06f,
                    glass = tokens.isGlass,
                    isPixel = tokens.isPixel,
                )
            }
        }
    }

    fun lerpColor(from: Int, to: Int, fraction: Float): Int {
        val t = fraction.coerceIn(0f, 1f)
        fun channel(a: Int, b: Int) = (a + (b - a) * t).toInt().coerceIn(0, 255)
        return Color.argb(
            channel(Color.alpha(from), Color.alpha(to)),
            channel(Color.red(from), Color.red(to)),
            channel(Color.green(from), Color.green(to)),
            channel(Color.blue(from), Color.blue(to)),
        )
    }

    fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    fun multiplyAlpha(color: Int, factor: Float): Int =
        Color.argb(
            (Color.alpha(color) * factor).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    /** WCAG relative luminance, used to pick readable text on the accent fill. */
    fun luminance(color: Int): Double {
        fun channel(value: Int): Double {
            val c = value / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(Color.red(color)) +
            0.7152 * channel(Color.green(color)) +
            0.0722 * channel(Color.blue(color))
    }
}
