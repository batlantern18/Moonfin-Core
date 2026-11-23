package com.moonfin.tvkeyboard.ime

import com.moonfin.tvkeyboard.theme.KeyboardStyle

/** Where the compact input window sits horizontally. */
enum class HorizontalAlignment {
    LEFT,
    CENTER,
    RIGHT,
    ;

    companion object {
        /** Unknown or absent names fall back to centre rather than failing. */
        fun fromName(name: String?): HorizontalAlignment = when (name?.trim()?.lowercase()) {
            "left", "start" -> LEFT
            "right", "end" -> RIGHT
            else -> CENTER
        }
    }
}

/**
 * The input window's width and horizontal placement. The keyboard is a compact
 * window, not a full-screen panel; a host can pull it to an edge with
 * `privateImeOptions="horizontalAlignment=left|center|right"`.
 */
data class WindowPlacement(
    val alignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    val widthFraction: Float = DEFAULT_WIDTH_FRACTION,
) {

    /** The same placement, sized for the chosen keyboard style. */
    fun withStyle(style: KeyboardStyle): WindowPlacement =
        copy(widthFraction = widthFractionFor(style))

    companion object {
        const val DEFAULT_WIDTH_FRACTION = 0.6f

        /** Docked is slightly narrower so the extra rows do not make it tall. */
        const val DOCKED_WIDTH_FRACTION = 0.52f
        const val OPTION = "horizontalAlignment"

        /**
         * Reads `horizontalAlignment` out of the comma-separated
         * `privateImeOptions` string. Unknown options and unknown values are
         * ignored, leaving the centred default.
         */
        fun parse(privateImeOptions: String?): WindowPlacement {
            val requested = privateImeOptions
                ?.split(',')
                ?.mapNotNull { option ->
                    val parts = option.split('=', limit = 2)
                    if (parts.size == 2 && parts[0].trim().equals(OPTION, ignoreCase = true)) {
                        parts[1].trim()
                    } else {
                        null
                    }
                }
                ?.lastOrNull()
            return WindowPlacement(alignment = HorizontalAlignment.fromName(requested))
        }

        fun widthPx(screenWidth: Int, fraction: Float = DEFAULT_WIDTH_FRACTION): Int =
            (screenWidth * fraction).toInt().coerceAtLeast(1)

        /**
         * Every style is a compact floating panel: the reference keyboard is a
         * small centred card, never a bar across the screen. Docked is the
         * tightest because it also carries the echo and cue rows.
         */
        fun widthFractionFor(style: KeyboardStyle): Float = when (style) {
            KeyboardStyle.DOCKED -> DOCKED_WIDTH_FRACTION
            KeyboardStyle.CLASSIC, KeyboardStyle.GLASS -> DEFAULT_WIDTH_FRACTION
        }
    }
}
