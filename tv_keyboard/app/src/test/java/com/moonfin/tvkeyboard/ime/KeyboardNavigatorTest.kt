package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardNavigatorTest {

    /** A 3x3 grid of 100px keys used by the navigation tests. */
    private val grid: List<NavKey> = buildList {
        for (row in 0 until 3) {
            for (column in 0 until 3) {
                val index = row * 3 + column
                add(
                    NavKey(
                        index = index,
                        left = column * 100f,
                        top = row * 100f,
                        right = column * 100f + 100f,
                        bottom = row * 100f + 100f,
                    ),
                )
            }
        }
    }

    @Test
    fun `right moves focus to the next key in the same row`() {
        assertEquals(4, KeyboardNavigator.move(grid, 3, Direction.RIGHT, cyclic = false))
    }

    @Test
    fun `up moves focus to the same column in the row above`() {
        assertEquals(1, KeyboardNavigator.move(grid, 4, Direction.UP, cyclic = false))
    }

    @Test
    fun `down moves focus to the same column in the row below`() {
        assertEquals(7, KeyboardNavigator.move(grid, 4, Direction.DOWN, cyclic = false))
    }

    @Test
    fun `left from the first column stays put when not cyclic`() {
        assertEquals(3, KeyboardNavigator.move(grid, 3, Direction.LEFT, cyclic = false))
    }

    @Test
    fun `left from the first column wraps to the end of the row when cyclic`() {
        assertEquals(5, KeyboardNavigator.move(grid, 3, Direction.LEFT, cyclic = true))
    }

    @Test
    fun `right from the last column wraps to the start of the row when cyclic`() {
        assertEquals(3, KeyboardNavigator.move(grid, 5, Direction.RIGHT, cyclic = true))
    }

    @Test
    fun `up from the top row wraps to the bottom row when cyclic`() {
        assertEquals(7, KeyboardNavigator.move(grid, 1, Direction.UP, cyclic = true))
    }

    @Test
    fun `down from the bottom row wraps to the top row when cyclic`() {
        assertEquals(1, KeyboardNavigator.move(grid, 7, Direction.DOWN, cyclic = true))
    }

    @Test
    fun `hit test returns the key containing the point`() {
        assertEquals(4, KeyboardNavigator.hitTest(grid, 150f, 150f))
    }

    @Test
    fun `hit test falls back to the nearest key outside the keyboard`() {
        assertEquals(8, KeyboardNavigator.hitTest(grid, 1000f, 1000f))
    }

    @Test
    fun `move on an empty keyboard returns minus one`() {
        assertEquals(-1, KeyboardNavigator.move(emptyList(), 0, Direction.LEFT, cyclic = false))
    }
}
