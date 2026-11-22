package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure D-pad navigation for the language picker overlay. */
class LanguagePickerModelTest {

    @Test
    fun `clamp keeps an index inside the list`() {
        assertEquals(0, LanguagePickerModel.clamp(-3, 5))
        assertEquals(4, LanguagePickerModel.clamp(99, 5))
        assertEquals(2, LanguagePickerModel.clamp(2, 5))
    }

    @Test
    fun `an empty list has no selection`() {
        assertEquals(-1, LanguagePickerModel.clamp(0, 0))
        assertEquals(-1, LanguagePickerModel.move(0, 0, PickerDirection.DOWN, cyclic = false))
    }

    @Test
    fun `down moves to the next language and stops at the end`() {
        assertEquals(1, LanguagePickerModel.move(0, 3, PickerDirection.DOWN, cyclic = false))
        assertEquals(2, LanguagePickerModel.move(2, 3, PickerDirection.DOWN, cyclic = false))
    }

    @Test
    fun `up moves to the previous language and stops at the start`() {
        assertEquals(0, LanguagePickerModel.move(1, 3, PickerDirection.UP, cyclic = false))
        assertEquals(0, LanguagePickerModel.move(0, 3, PickerDirection.UP, cyclic = false))
    }

    @Test
    fun `cyclic down from the last wraps to the first`() {
        assertEquals(0, LanguagePickerModel.move(2, 3, PickerDirection.DOWN, cyclic = true))
    }

    @Test
    fun `cyclic up from the first wraps to the last`() {
        assertEquals(2, LanguagePickerModel.move(0, 3, PickerDirection.UP, cyclic = true))
    }

    @Test
    fun `a single language never moves`() {
        assertEquals(0, LanguagePickerModel.move(0, 1, PickerDirection.DOWN, cyclic = true))
        assertEquals(0, LanguagePickerModel.move(0, 1, PickerDirection.UP, cyclic = true))
    }
}
