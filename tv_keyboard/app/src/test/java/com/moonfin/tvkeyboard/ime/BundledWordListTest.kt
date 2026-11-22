package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledWordListTest {

    private val words = listOf("apple", "application", "apply", "apt", "banana", "band", "be", "bee")

    @Test
    fun `prefix shorter than 2 chars returns empty`() {
        assertTrue(BundledWordList.completionsFor("a", words).isEmpty())
        assertTrue(BundledWordList.completionsFor("", words).isEmpty())
    }

    @Test
    fun `prefix matches at start case-insensitively`() {
        val results = BundledWordList.completionsFor("ap", words)
        assertTrue(results.containsAll(listOf("apple", "application", "apply", "apt")))
        assertFalse(results.contains("banana"))
    }

    @Test
    fun `exact word not returned as completion`() {
        val results = BundledWordList.completionsFor("apple", words)
        assertFalse(results.contains("apple"))
    }

    @Test
    fun `uppercase prefix matches lowercase list`() {
        val results = BundledWordList.completionsFor("AP", words)
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `limit is respected`() {
        val results = BundledWordList.completionsFor("ap", words, limit = 2)
        assertEquals(2, results.size)
    }
}
