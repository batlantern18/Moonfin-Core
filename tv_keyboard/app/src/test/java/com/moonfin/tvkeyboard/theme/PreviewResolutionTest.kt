package com.moonfin.tvkeyboard.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewResolutionTest {

    @Test
    fun `palette resolves for all theme and style combinations`() {
        for (theme in VisualTheme.entries) {
            for (style in KeyboardStyle.entries) {
                val palette = ThemeCatalog.palette(theme, style)
                assertNotEquals(0, palette.idleKey)
                assertNotEquals(0, palette.selectedKey)
            }
        }
    }

    @Test
    fun `theme fromId returns MOONFIN for unknown id`() {
        assertEquals(VisualTheme.MOONFIN, VisualTheme.fromId(null))
        assertEquals(VisualTheme.MOONFIN, VisualTheme.fromId("unknown_theme"))
    }

    @Test
    fun `style fromId returns DOCKED for unknown id`() {
        assertEquals(KeyboardStyle.DOCKED, KeyboardStyle.fromId(null))
        assertEquals(KeyboardStyle.DOCKED, KeyboardStyle.fromId("unknown"))
    }
}
