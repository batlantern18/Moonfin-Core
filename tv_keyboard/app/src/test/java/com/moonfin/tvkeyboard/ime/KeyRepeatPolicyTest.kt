package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyRepeatPolicyTest {

    @Test
    fun `isRepeatKey returns true for delete and cursor keys`() {
        val policy = KeyRepeatPolicy.DEFAULT
        assertTrue(policy.isRepeatKey(KeyCodes.DELETE))
        assertTrue(policy.isRepeatKey(KeyCodes.LEFT))
        assertTrue(policy.isRepeatKey(KeyCodes.RIGHT))
    }

    @Test
    fun `isRepeatKey returns false for other keys`() {
        val policy = KeyRepeatPolicy.DEFAULT
        assertFalse(policy.isRepeatKey(KeyCodes.SHIFT))
        assertFalse(policy.isRepeatKey(KeyCodes.LANGUAGE))
        assertFalse(policy.isRepeatKey(KeyCodes.SPACE))
        assertFalse(policy.isRepeatKey(KeyCodes.MODE_CHANGE))
        assertFalse(policy.isRepeatKey(KeyCodes.VOICE))
        assertFalse(policy.isRepeatKey(KeyCodes.CLIPBOARD))
        assertFalse(policy.isRepeatKey(65)) // 'a'
    }

    @Test
    fun `policy timing constants match specifications`() {
        val policy = KeyRepeatPolicy.DEFAULT
        assertEquals(400L, policy.initialDelayMs)
        assertEquals(50L, policy.intervalMs)
    }
}
