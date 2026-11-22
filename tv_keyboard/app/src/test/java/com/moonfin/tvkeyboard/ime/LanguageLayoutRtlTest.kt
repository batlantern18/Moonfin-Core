package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RTL detection drives the keyboard and picker layout direction. */
class LanguageLayoutRtlTest {

    private fun layout(code: String) = LanguageLayout("name", code, azerty = false)

    @Test
    fun `arabic hebrew and persian are right to left`() {
        assertTrue(layout("ar").isRtl)
        assertTrue(layout("he").isRtl)
        assertTrue(layout("fa").isRtl)
    }

    @Test
    fun `latin languages are left to right`() {
        assertFalse(layout("en_us").isRtl)
        assertFalse(layout("fr").isRtl)
        assertFalse(layout("ko_kr").isRtl)
    }

    @Test
    fun `rtl detection ignores the board variant suffix`() {
        // `ar` appears as a bare primary code; a hypothetical variant still resolves.
        assertTrue(layout("ar_01").isRtl)
        assertFalse(layout("fr_fr").isRtl)
    }
}
