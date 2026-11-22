package com.moonfin.tvkeyboard.ime

import android.content.Context
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for Shift/caps/symbols/numbers behavior in KeyboardSession.
 * These tests verify the public interface (seams) without reaching into private helpers.
 */
@RunWith(RobolectricTestRunner::class)
class KeyboardSessionShiftTest {

    private lateinit var context: Context
    private lateinit var session: KeyboardSession

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        session = KeyboardSession(context, KeyboardPreferences(context))
    }

    @Test
    fun testShiftCyclesOffOnLockedOff() {
        assertEquals(KeyboardSession.Shift.OFF, session.shift)
        
        session.toggleShift()
        assertEquals(KeyboardSession.Shift.ON, session.shift)
        
        session.toggleShift()
        assertEquals(KeyboardSession.Shift.LOCKED, session.shift)
        
        session.toggleShift()
        assertEquals(KeyboardSession.Shift.OFF, session.shift)
    }

    @Test
    fun testLockShiftTogglesLockedUnlocked() {
        assertEquals(KeyboardSession.Shift.OFF, session.shift)
        
        session.lockShift()
        assertEquals(KeyboardSession.Shift.LOCKED, session.shift)
        
        session.lockShift()
        assertEquals(KeyboardSession.Shift.OFF, session.shift)
        
        // If we start from ON, it should go to LOCKED
        session.toggleShift() // OFF -> ON
        session.lockShift()   // ON -> LOCKED
        assertEquals(KeyboardSession.Shift.LOCKED, session.shift)
        
        session.lockShift()   // LOCKED -> OFF
        assertEquals(KeyboardSession.Shift.OFF, session.shift)
    }

    @Test
    fun testConsumeShiftResetsOneShot() {
        assertEquals(KeyboardSession.Shift.OFF, session.shift)
        
        session.toggleShift() // OFF -> ON
        assertEquals(KeyboardSession.Shift.ON, session.shift)
        
        session.consumeShift() // ON -> OFF (one-shot consumed)
        assertEquals(KeyboardSession.Shift.OFF, session.shift)
        
        // Locked shift should not be consumed
        session.lockShift() // OFF -> LOCKED
        assertEquals(KeyboardSession.Shift.LOCKED, session.shift)
        
        session.consumeShift() // LOCKED stays LOCKED
        assertEquals(KeyboardSession.Shift.LOCKED, session.shift)
    }

    @Test
    fun testToggleModeCyclesAlphabetSymbolsNumbers() {
        assertEquals(KeyboardSession.Mode.ALPHABET, session.mode)
        
        session.toggleMode() // ALPHABET -> SYMBOLS
        assertEquals(KeyboardSession.Mode.SYMBOLS, session.mode)
        
        session.toggleMode() // SYMBOLS -> NUMBERS
        assertEquals(KeyboardSession.Mode.NUMBERS, session.mode)
        
        session.toggleMode() // NUMBERS -> ALPHABET
        assertEquals(KeyboardSession.Mode.ALPHABET, session.mode)
    }

    @Test
    fun testShowNumbersSwitchesToNumbers() {
        assertEquals(KeyboardSession.Mode.ALPHABET, session.mode)
        
        session.showNumbers()
        assertEquals(KeyboardSession.Mode.NUMBERS, session.mode)
    }

    @Test
    fun testShowAlphabetSwitchesToAlphabet() {
        session.showNumbers()
        assertEquals(KeyboardSession.Mode.NUMBERS, session.mode)
        
        session.showAlphabet()
        assertEquals(KeyboardSession.Mode.ALPHABET, session.mode)
        
        session.toggleMode() // ALPHABET -> SYMBOLS
        assertEquals(KeyboardSession.Mode.SYMBOLS, session.mode)
        
        session.showAlphabet()
        assertEquals(KeyboardSession.Mode.ALPHABET, session.mode)
    }
}
