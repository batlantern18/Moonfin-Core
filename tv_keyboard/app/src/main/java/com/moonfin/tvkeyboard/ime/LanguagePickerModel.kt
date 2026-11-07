package com.moonfin.tvkeyboard.ime

/**
 * Pure D-pad navigation for the language picker overlay. Kept free of Android
 * types so the up/down/cyclic behaviour can be unit-tested on the JVM. The
 * picker is a single vertical list, so horizontal input is a no-op.
 */
enum class PickerDirection { UP, DOWN }

object LanguagePickerModel {

    /** Clamp a selection into range; an empty list has no selection (-1). */
    fun clamp(index: Int, count: Int): Int =
        if (count <= 0) -1 else index.coerceIn(0, count - 1)

    /**
     * Move the selection by one row. With [cyclic] on, the edges wrap around
     * the list; with it off, the selection stops at the ends.
     */
    fun move(index: Int, count: Int, direction: PickerDirection, cyclic: Boolean): Int {
        if (count <= 1) return clamp(index, count)
        val current = clamp(index, count)
        return when (direction) {
            PickerDirection.UP ->
                if (current == 0) (if (cyclic) count - 1 else 0) else current - 1
            PickerDirection.DOWN ->
                if (current == count - 1) (if (cyclic) 0 else count - 1) else current + 1
        }
    }
}
