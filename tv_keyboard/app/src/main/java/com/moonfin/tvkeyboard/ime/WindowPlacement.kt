package com.moonfin.tvkeyboard.ime

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

    companion object {
        const val DEFAULT_WIDTH_FRACTION = 0.6f
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
    }
}
