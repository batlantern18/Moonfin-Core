package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardGeometryTest {

    private fun rect(index: Int, x: Int, y: Int, width: Int = 100, height: Int = 50) =
        RawKeyRect(index = index, x = x, y = y, width = width, height = height)

    @Test
    fun `key rectangles scale uniformly to the view width`() {
        val keys = listOf(rect(0, 0, 0), rect(1, 100, 0), rect(2, 0, 50))

        val cells = KeyboardGeometry.scaled(keys, layoutWidth = 200, layoutHeight = 100, viewWidth = 400)

        assertEquals(NavKey(0, 0f, 0f, 200f, 100f), cells[0])
        assertEquals(NavKey(1, 200f, 0f, 400f, 100f), cells[1])
        assertEquals(NavKey(2, 0f, 100f, 200f, 200f), cells[2])
    }

    @Test
    fun `content height follows the same scale as the width`() {
        assertEquals(
            200,
            KeyboardGeometry.contentHeight(layoutWidth = 200, layoutHeight = 100, viewWidth = 400),
        )
    }

    @Test
    fun `an empty layout produces no cells`() {
        assertTrue(KeyboardGeometry.scaled(emptyList(), 200, 100, 400).isEmpty())
    }

    @Test
    fun `mirroring puts the first key on the right edge`() {
        val cells = KeyboardGeometry.scaled(
            listOf(rect(0, 0, 0), rect(1, 100, 0)),
            layoutWidth = 200,
            layoutHeight = 50,
            viewWidth = 400,
        )

        val mirrored = KeyboardGeometry.mirrored(cells, viewWidth = 400)

        assertEquals(NavKey(0, 200f, 0f, 400f, 100f), mirrored[0])
        assertEquals(NavKey(1, 0f, 0f, 200f, 100f), mirrored[1])
    }

    @Test
    fun `mirroring keeps each key's index and row`() {
        val cells = KeyboardGeometry.scaled(
            listOf(rect(0, 0, 0), rect(1, 100, 100)),
            layoutWidth = 200,
            layoutHeight = 150,
            viewWidth = 200,
        )

        val mirrored = KeyboardGeometry.mirrored(cells, viewWidth = 200)

        assertEquals(listOf(0, 1), mirrored.map { it.index })
        assertEquals(cells[1].top, mirrored[1].top, 0.001f)
        assertEquals(cells[1].bottom, mirrored[1].bottom, 0.001f)
    }

    @Test
    fun `a zero width view produces no cells instead of dividing by zero`() {
        assertTrue(KeyboardGeometry.scaled(listOf(rect(0, 0, 0)), 200, 100, 0).isEmpty())
    }
}
