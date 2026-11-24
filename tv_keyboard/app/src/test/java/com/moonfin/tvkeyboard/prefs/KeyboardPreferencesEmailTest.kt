package com.moonfin.tvkeyboard.prefs

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.any

/**
 * Tests for saved email address persistence in KeyboardPreferences.
 */
class KeyboardPreferencesEmailTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var preferences: KeyboardPreferences

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)

        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSharedPreferences(KeyboardPreferences.FILE, Context.MODE_PRIVATE))
            .thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.putStringSet(anyString(), any())).thenReturn(editor)

        preferences = KeyboardPreferences(context)
    }

    @Test
    fun `savedEmails defaults to empty set when not set`() {
        `when`(prefs.getStringSet(KeyboardPreferences.KEY_SAVED_EMAILS, null)).thenReturn(null)
        assertTrue(preferences.savedEmails.isEmpty())
    }

    @Test
    fun `savedEmails persists and restores`() {
        val emails = setOf("user@gmail.com", "test@yahoo.com")
        `when`(prefs.getStringSet(KeyboardPreferences.KEY_SAVED_EMAILS, null)).thenReturn(emails)
        assertEquals(emails, preferences.savedEmails)
    }

    @Test
    fun `addSavedEmail stores the email`() {
        val stored = mutableSetOf<String>()
        `when`(prefs.getStringSet(KeyboardPreferences.KEY_SAVED_EMAILS, null)).then { stored.toSet() }
        `when`(editor.putStringSet(anyString(), any())).then { invocation ->
            val set = invocation.getArgument<Set<String>>(1)
            stored.clear()
            stored.addAll(set)
            editor
        }

        preferences.addSavedEmail("new@gmail.com")

        assertEquals(setOf("new@gmail.com"), preferences.savedEmails)
    }

    @Test
    fun `addSavedEmail deduplicates existing emails and moves to front`() {
        val stored = mutableSetOf<String>()
        `when`(prefs.getStringSet(KeyboardPreferences.KEY_SAVED_EMAILS, null)).then { stored.toSet() }
        `when`(editor.putStringSet(anyString(), any())).then { invocation ->
            val set = invocation.getArgument<Set<String>>(1)
            stored.clear()
            stored.addAll(set)
            editor
        }

        preferences.addSavedEmail("new@gmail.com")
        preferences.addSavedEmail("existing@test.com")

        val list = preferences.savedEmails.toList()
        assertEquals(2, list.size)
        assertEquals("existing@test.com", list[0]) // Most recent should be first
        assertEquals("new@gmail.com", list[1])
    }

    @Test
    fun `addSavedEmail maintains order with most recent first`() {
        val stored = mutableSetOf<String>()
        `when`(prefs.getStringSet(KeyboardPreferences.KEY_SAVED_EMAILS, null)).then { stored.toSet() }
        `when`(editor.putStringSet(anyString(), any())).then { invocation ->
            val set = invocation.getArgument<Set<String>>(1)
            stored.clear()
            stored.addAll(set)
            editor
        }

        preferences.addSavedEmail("first@test.com")
        preferences.addSavedEmail("second@test.com")
        preferences.addSavedEmail("third@test.com")

        val list = preferences.savedEmails.toList()
        // Most recently added should be first
        assertEquals("third@test.com", list[0])
        assertEquals("second@test.com", list[1])
        assertEquals("first@test.com", list[2])
    }

    @Test
    fun `clearSavedEmails removes all saved emails`() {
        `when`(prefs.getStringSet(KeyboardPreferences.KEY_SAVED_EMAILS, null)).thenReturn(setOf("test@test.com"))
        `when`(editor.remove(anyString())).thenReturn(editor)

        preferences.clearSavedEmails()

        verify(editor).remove(KeyboardPreferences.KEY_SAVED_EMAILS)
        verify(editor).apply()
    }
}