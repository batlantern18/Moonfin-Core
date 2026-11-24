package com.moonfin.tvkeyboard.settings

import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.moonfin.tvkeyboard.ime.LanguageRegistry
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.Locale

/**
 * The settings screen must offer every reference variant as a real checkbox
 * row, and a device configured for a regional locale must select that exact
 * variant instead of the English default.
 */
@RunWith(RobolectricTestRunner::class)
class RegionalLanguageSettingsTest {

    private fun activity(): MainActivity =
        Robolectric.buildActivity(MainActivity::class.java).setup().get()

    private fun languageRow(activity: MainActivity, label: String): View {
        val roots = mutableListOf<View>()
        fun collect(view: View) {
            if (view is TextView && view.text?.toString() == label) roots += view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) collect(view.getChildAt(i))
            }
        }
        activity.findViewById<View>(android.R.id.content)?.let(::collect)
        val text = roots.firstOrNull() ?: throw AssertionError("No settings row for '$label'")
        return text.parent as View
    }

    private fun checkBoxIn(row: View): CheckBox {
        fun find(view: View): CheckBox? {
            if (view is CheckBox) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
            }
            return null
        }
        return find(row) ?: throw AssertionError("Settings row has no checkbox")
    }

    @Test
    fun `settings offers every registered reference variant`() {
        val current = activity()
        val prefs = KeyboardPreferences(current)
        prefs.reset()
        val activity = activity()
        val labels = LanguageRegistry.all(activity).map { it.displayName }

        labels.forEach { label ->
            val row = languageRow(activity, label)
            assertTrue("'$label' is not focusable", row.isFocusable)
            assertNotNull("'$label' has no checkbox", checkBoxIn(row))
        }
    }

    @Test
    fun `enabling a regional variant persists it as the selected keyboard`() {
        val activity = activity()
        val prefs = KeyboardPreferences(activity)
        prefs.reset()
        prefs.setEnabledLanguageKeys(listOf("en_us"))

        val reopened = activity()
        val row = languageRow(reopened, "English (UK)")
        checkBoxIn(row).isChecked = true

        val stored = KeyboardPreferences(reopened)
        assertTrue("en_us was lost", "en_us" in stored.enabledLanguageKeys)
        assertTrue("en_gb was not enabled", "en_gb" in stored.enabledLanguageKeys)
        assertTrue(
            "en_gb is not an enabled layout",
            stored.enabledLanguages().any { it.key == "en_gb" },
        )
    }

    @Test
    fun `system locale selects its exact registered variant`() {
        val original = Locale.getDefault()
        try {
            val activity = activity()
            val prefs = KeyboardPreferences(activity)
            prefs.reset()
            prefs.useSystemLanguages = true
            Locale.setDefault(Locale("en", "AU"))
            assertEquals("en_au", prefs.initialLanguage().key)
            Locale.setDefault(Locale("fr", "CA"))
            assertEquals("fr_ca", prefs.initialLanguage().key)
        } finally {
            Locale.setDefault(original)
        }
    }
}
