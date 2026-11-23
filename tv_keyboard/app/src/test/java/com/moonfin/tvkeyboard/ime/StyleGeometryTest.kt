package com.moonfin.tvkeyboard.ime

import com.moonfin.tvkeyboard.theme.KeyboardStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Docked fills the width; Classic and Glass keep the compact floating window. */
class StyleGeometryTest {

    @Test
    fun `docked stays a compact card`() {
        val fraction = WindowPlacement.widthFractionFor(KeyboardStyle.DOCKED)

        assertTrue("docked must not span the screen", fraction < 0.6f)
        assertEquals(WindowPlacement.DOCKED_WIDTH_FRACTION, fraction, 0.001f)
    }

    @Test
    fun `classic and glass keep the compact fraction`() {
        assertEquals(
            WindowPlacement.DEFAULT_WIDTH_FRACTION,
            WindowPlacement.widthFractionFor(KeyboardStyle.CLASSIC),
            0.001f,
        )
        assertEquals(
            WindowPlacement.DEFAULT_WIDTH_FRACTION,
            WindowPlacement.widthFractionFor(KeyboardStyle.GLASS),
            0.001f,
        )
    }

    @Test
    fun `placement inherits the style width and keeps the host alignment`() {
        val parsed = WindowPlacement.parse("horizontalAlignment=right").withStyle(KeyboardStyle.DOCKED)

        assertEquals(HorizontalAlignment.RIGHT, parsed.alignment)
        assertEquals(WindowPlacement.DOCKED_WIDTH_FRACTION, parsed.widthFraction, 0.001f)
    }

    @Test
    fun `docked width is a fraction of the screen`() {
        val width = WindowPlacement.widthPx(1920, WindowPlacement.widthFractionFor(KeyboardStyle.DOCKED))

        assertTrue("expected a narrow panel, got $width", width < 1100)
    }
}
