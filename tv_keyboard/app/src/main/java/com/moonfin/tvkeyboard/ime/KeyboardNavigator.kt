package com.moonfin.tvkeyboard.ime

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class Direction { UP, DOWN, LEFT, RIGHT }

/** A key's rectangle in view pixels, used by the pure navigation model. */
data class NavKey(
    val index: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Geometric D-pad navigation over a keyboard's key rectangles. Kept free of
 * Android types so it can be unit-tested on the JVM.
 */
object KeyboardNavigator {

    private const val EDGE_TOLERANCE = 1f

    fun move(keys: List<NavKey>, current: Int, direction: Direction, cyclic: Boolean): Int {
        if (keys.isEmpty()) return -1
        val currentIndex = if (current in keys.indices) current else 0
        val cur = keys[currentIndex]

        val candidates = keys.filter { it.index != currentIndex && isInDirection(cur, it, direction) }
        if (candidates.isEmpty()) {
            return if (cyclic) wrap(keys, cur, direction) else currentIndex
        }

        // Prefer keys that share the perpendicular band (same row for
        // left/right, same column for up/down) before falling back to diagonals.
        val overlapping = candidates.filter { overlapsPerpendicular(cur, it, direction) }
        val pool = overlapping.ifEmpty { candidates }
        return pool.minByOrNull { score(cur, it, direction) }!!.index
    }

    private fun isInDirection(from: NavKey, to: NavKey, direction: Direction): Boolean = when (direction) {
        Direction.LEFT -> to.right <= from.left + EDGE_TOLERANCE
        Direction.RIGHT -> to.left >= from.right - EDGE_TOLERANCE
        Direction.UP -> to.bottom <= from.top + EDGE_TOLERANCE
        Direction.DOWN -> to.top >= from.bottom - EDGE_TOLERANCE
    }

    private fun overlapsPerpendicular(a: NavKey, b: NavKey, direction: Direction): Boolean = when (direction) {
        Direction.LEFT, Direction.RIGHT -> b.bottom > a.top && b.top < a.bottom
        Direction.UP, Direction.DOWN -> b.right > a.left && b.left < a.right
    }

    private fun score(from: NavKey, to: NavKey, direction: Direction): Float {
        val primary = when (direction) {
            Direction.LEFT -> from.left - to.right
            Direction.RIGHT -> to.left - from.right
            Direction.UP -> from.top - to.bottom
            Direction.DOWN -> to.top - from.bottom
        }
        val perpendicular = when (direction) {
            Direction.LEFT, Direction.RIGHT -> abs(to.centerY - from.centerY)
            Direction.UP, Direction.DOWN -> abs(to.centerX - from.centerX)
        }
        return primary + perpendicular * 0.35f
    }

    /**
     * Cyclic wrap: left/right stays in the same row and jumps to the far edge;
     * up/down jumps to the opposite edge row and keeps the nearest column.
     */
    private fun wrap(keys: List<NavKey>, cur: NavKey, direction: Direction): Int {
        val onRow = keys.filter { it.index != cur.index && verticalOverlap(it, cur) }
        val onColumn = keys.filter { it.index != cur.index && horizontalOverlap(it, cur) }

        return when (direction) {
            Direction.LEFT ->
                (onRow.ifEmpty { keys.filter { it.index != cur.index } }).maxByOrNull { it.right }!!.index
            Direction.RIGHT ->
                (onRow.ifEmpty { keys.filter { it.index != cur.index } }).minByOrNull { it.left }!!.index
            Direction.UP -> {
                val pool = onColumn.ifEmpty { keys.filter { it.index != cur.index } }
                val bottomMost = pool.maxOf { it.bottom }
                pool.filter { abs(it.bottom - bottomMost) <= EDGE_TOLERANCE }
                    .minByOrNull { abs(it.centerX - cur.centerX) }!!.index
            }
            Direction.DOWN -> {
                val pool = onColumn.ifEmpty { keys.filter { it.index != cur.index } }
                val topMost = pool.minOf { it.top }
                pool.filter { abs(it.top - topMost) <= EDGE_TOLERANCE }
                    .minByOrNull { abs(it.centerX - cur.centerX) }!!.index
            }
        }
    }

    private fun verticalOverlap(a: NavKey, b: NavKey): Boolean = a.bottom > b.top && a.top < b.bottom
    private fun horizontalOverlap(a: NavKey, b: NavKey): Boolean = a.right > b.left && a.left < b.right

    /** Index of the key containing a point, or the nearest key centre. */
    fun hitTest(keys: List<NavKey>, x: Float, y: Float): Int {
        if (keys.isEmpty()) return -1
        keys.firstOrNull { x >= it.left && x <= it.right && y >= it.top && y <= it.bottom }
            ?.let { return it.index }
        var best = 0
        var bestDistance = Float.MAX_VALUE
        for (key in keys) {
            val dx = max(0f, max(key.left - x, x - key.right))
            val dy = max(0f, max(key.top - y, y - key.bottom))
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                best = key.index
            }
        }
        return best
    }

    /** Index of the key closest to a vertical row line, used for row jumps. */
    fun nearestInRow(keys: List<NavKey>, x: Float, y: Float): Int {
        if (keys.isEmpty()) return -1
        val tolerance = min(16f, (keys.maxOf { it.bottom } - keys.minOf { it.top }) / 2f)
        val row = keys.filter { abs(it.centerY - y) <= tolerance }
        val pool = row.ifEmpty { keys }
        return pool.minByOrNull { abs(it.centerX - x) }!!.index
    }
}
