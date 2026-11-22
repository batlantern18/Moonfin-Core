package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeZonePolicyTest {

    @Test
    fun `KeyboardNavigator returns same index when at top edge`() {
        val keys = listOf(
            NavKey(0, 0f, 0f, 50f, 44f),
            NavKey(1, 50f, 0f, 100f, 44f),
            NavKey(2, 100f, 0f, 150f, 44f),
        )
        val result = KeyboardNavigator.move(keys, 0, Direction.UP, cyclic = false)
        assertEquals(0, result)
    }

    @Test
    fun `KeyboardNavigator returns same index when at left edge of row`() {
        val keys = listOf(
            NavKey(0, 0f, 0f, 50f, 44f),
            NavKey(1, 50f, 0f, 100f, 44f),
        )
        val result = KeyboardNavigator.move(keys, 0, Direction.LEFT, cyclic = false)
        assertEquals(0, result)
    }

    @Test
    fun `KeyboardNavigator returns same index when at right edge of row`() {
        val keys = listOf(
            NavKey(0, 0f, 0f, 50f, 44f),
            NavKey(1, 50f, 0f, 100f, 44f),
        )
        val result = KeyboardNavigator.move(keys, 1, Direction.RIGHT, cyclic = false)
        assertEquals(1, result)
    }

    @Test
    fun `move returns different index when not at edge`() {
        val keys = listOf(
            NavKey(0, 0f, 0f, 50f, 44f),
            NavKey(1, 50f, 0f, 100f, 44f),
        )
        val result = KeyboardNavigator.move(keys, 0, Direction.RIGHT, cyclic = false)
        assertEquals(1, result)
    }
}
