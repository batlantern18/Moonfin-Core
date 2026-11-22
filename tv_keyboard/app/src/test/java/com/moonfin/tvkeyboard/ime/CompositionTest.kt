package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionTest {

    @Test
    fun `appending builds the word in order`() {
        val composition = Composition()

        composition.append("h")
        composition.append("i")

        assertEquals("hi", composition.value)
        assertEquals(2, composition.length)
        assertFalse(composition.isEmpty)
    }

    @Test
    fun `deleting removes one character at a time`() {
        val composition = Composition()
        composition.append("hi")

        assertFalse(composition.deleteLast())
        assertEquals("h", composition.value)
        assertTrue(composition.deleteLast())
        assertTrue(composition.isEmpty)
    }

    @Test
    fun `deleting an empty composition stays empty`() {
        val composition = Composition()

        assertTrue(composition.deleteLast())
        assertEquals("", composition.value)
    }

    @Test
    fun `a composition that the field still holds is kept`() {
        val composition = Composition()
        composition.append("wo")

        assertTrue(composition.adopt("two wo"))
        assertEquals("wo", composition.value)
    }

    @Test
    fun `a composition the field no longer holds is dropped`() {
        val composition = Composition()
        composition.append("wo")

        assertFalse(composition.adopt("different text"))
        assertTrue(composition.isEmpty)
    }

    @Test
    fun `a field that cannot report its text does not drop the composition`() {
        val composition = Composition()
        composition.append("wo")

        assertTrue(composition.adopt(null))
        assertEquals("wo", composition.value)
    }

    @Test
    fun `clearing empties the composition`() {
        val composition = Composition()
        composition.append("word")

        composition.clear()

        assertTrue(composition.isEmpty)
    }
}
