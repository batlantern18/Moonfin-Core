package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceStateTest {

    @Test
    fun `voiceState has expected enum constants`() {
        val states = VoiceState.entries
        assertEquals(3, states.size)
        assertTrue(states.contains(VoiceState.IDLE))
        assertTrue(states.contains(VoiceState.LISTENING))
        assertTrue(states.contains(VoiceState.ERROR))
    }

    @Test
    fun `valueOf resolves valid enum names`() {
        assertEquals(VoiceState.IDLE, VoiceState.valueOf("IDLE"))
        assertEquals(VoiceState.LISTENING, VoiceState.valueOf("LISTENING"))
        assertEquals(VoiceState.ERROR, VoiceState.valueOf("ERROR"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `valueOf throws on invalid enum name`() {
        VoiceState.valueOf("UNKNOWN")
    }
}
