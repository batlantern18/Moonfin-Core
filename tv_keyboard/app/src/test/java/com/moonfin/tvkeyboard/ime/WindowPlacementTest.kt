package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowPlacementTest {

    @Test
    fun `a host that asks for nothing gets the centred compact window`() {
        val placement = WindowPlacement.parse(null)

        assertEquals(HorizontalAlignment.CENTER, placement.alignment)
        assertEquals(0.6f, placement.widthFraction)
    }

    @Test
    fun `each documented alignment is honoured`() {
        assertEquals(HorizontalAlignment.LEFT, WindowPlacement.parse("horizontalAlignment=left").alignment)
        assertEquals(HorizontalAlignment.CENTER, WindowPlacement.parse("horizontalAlignment=center").alignment)
        assertEquals(HorizontalAlignment.RIGHT, WindowPlacement.parse("horizontalAlignment=right").alignment)
    }

    @Test
    fun `alignment is found among other private options`() {
        val placement = WindowPlacement.parse("com.example.flag=1,horizontalAlignment=right,other=2")

        assertEquals(HorizontalAlignment.RIGHT, placement.alignment)
    }

    @Test
    fun `option names and values are case-insensitive`() {
        assertEquals(HorizontalAlignment.LEFT, WindowPlacement.parse("HORIZONTALALIGNMENT=Left").alignment)
    }

    @Test
    fun `an unknown alignment falls back to centre`() {
        assertEquals(HorizontalAlignment.CENTER, WindowPlacement.parse("horizontalAlignment=diagonal").alignment)
        assertEquals(HorizontalAlignment.CENTER, WindowPlacement.parse("alignment=left").alignment)
        assertEquals(HorizontalAlignment.CENTER, WindowPlacement.parse("horizontalAlignment=").alignment)
    }

    @Test
    fun `the compact width is a fraction of the screen`() {
        assertEquals(1152, WindowPlacement.widthPx(1920))
        assertEquals(768, WindowPlacement.widthPx(1280))
        assertEquals(1, WindowPlacement.widthPx(0))
    }
}
