package com.moonfin.tvkeyboard.ime

/** One key rectangle straight from [android.inputmethodservice.Keyboard], in layout pixels. */
data class RawKeyRect(
    val index: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

/**
 * Converts a parsed keyboard's pixel geometry into view coordinates. Kept free
 * of Android types so the scaling can be unit-tested on the JVM.
 */
object KeyboardGeometry {

    /** Uniformly scales every key so the layout spans [viewWidth]. */
    fun scaled(
        keys: List<RawKeyRect>,
        layoutWidth: Int,
        layoutHeight: Int,
        viewWidth: Int,
    ): List<NavKey> {
        if (keys.isEmpty() || layoutWidth <= 0 || viewWidth <= 0) return emptyList()
        val scale = viewWidth.toFloat() / layoutWidth
        return keys.map { key ->
            NavKey(
                index = key.index,
                left = key.x * scale,
                top = key.y * scale,
                right = (key.x + key.width) * scale,
                bottom = (key.y + key.height) * scale,
            )
        }
    }

    /** Height the view needs so key rows keep the layout's aspect ratio. */
    fun contentHeight(layoutWidth: Int, layoutHeight: Int, viewWidth: Int): Int {
        if (layoutWidth <= 0 || layoutHeight <= 0 || viewWidth <= 0) return 0
        return (layoutHeight.toFloat() / layoutWidth * viewWidth).toInt()
    }
}
