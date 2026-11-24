package com.moonfin.tvkeyboard.settings

import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.moonfin.tvkeyboard.R
import com.moonfin.tvkeyboard.prefs.KeyboardPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * The two privacy controls must be real settings rows: visible, D-pad
 * reachable, and backed by persisted preferences. The privacy policy shown in
 * the same app must keep describing what the keyboard actually does.
 */
@RunWith(RobolectricTestRunner::class)
class PrivacyControlsTest {

    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        KeyboardPreferences(Robolectric.buildActivity(MainActivity::class.java).setup().get()).reset()
        activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
    }

    private fun collectTextViews(view: View, out: MutableList<TextView>) {
        if (view is TextView) out += view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) collectTextViews(view.getChildAt(i), out)
        }
    }

    private fun findRow(title: String): View? {
        val root = activity.findViewById<View>(android.R.id.content) ?: return null
        val labels = mutableListOf<TextView>()
        collectTextViews(root, labels)
        val label = labels.firstOrNull { it.text?.toString() == title } ?: return null
        return (label.parent as? View)?.parent as? View
    }

    private fun rowFor(titleRes: Int): View {
        val row = findRow(activity.getString(titleRes))
        assertNotNull("Settings row '${activity.getString(titleRes)}' is missing", row)
        return row!!
    }

    private fun checkBoxIn(row: View): CheckBox {
        fun find(view: View): CheckBox? {
            if (view is CheckBox) return view
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
            }
            return null
        }
        return find(row)!!
    }

    @Test
    fun `both privacy controls are visible and D-pad reachable`() {
        val shareRow = rowFor(R.string.option_share_usage_statistics)
        val languagesRow = rowFor(R.string.option_use_system_languages)

        assertEquals(View.VISIBLE, shareRow.visibility)
        assertEquals(View.VISIBLE, languagesRow.visibility)
        assertTrue(shareRow.isFocusable)
        assertTrue(shareRow.isFocusableInTouchMode)
        assertTrue(languagesRow.isFocusable)
        assertTrue(languagesRow.isFocusableInTouchMode)
    }

    @Test
    fun `share usage statistics starts off and persists when toggled`() {
        val prefs = KeyboardPreferences(activity)
        assertFalse(prefs.shareUsageStatistics)
        assertFalse(checkBoxIn(rowFor(R.string.option_share_usage_statistics)).isChecked)

        checkBoxIn(rowFor(R.string.option_share_usage_statistics)).isChecked = true
        assertTrue(prefs.shareUsageStatistics)

        val reopened = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertTrue(checkBoxIn(rowForOn(reopened, R.string.option_share_usage_statistics)).isChecked)
    }

    private fun rowForOn(target: MainActivity, titleRes: Int): View {
        val labels = mutableListOf<TextView>()
        val root = target.findViewById<View>(android.R.id.content)
        collectTextViews(root, labels)
        val label = labels.first { it.text?.toString() == target.getString(titleRes) }
        return (label.parent as? View)?.parent as? View ?: error("Row not found for $titleRes")
    }

    @Test
    fun `use system languages starts on and persists when toggled`() {
        val prefs = KeyboardPreferences(activity)
        assertTrue(prefs.useSystemLanguages)
        assertTrue(checkBoxIn(rowFor(R.string.option_use_system_languages)).isChecked)

        checkBoxIn(rowFor(R.string.option_use_system_languages)).isChecked = false
        assertFalse(prefs.useSystemLanguages)
    }

    @Test
    fun `privacy text still describes the no-telemetry behaviour`() {
        val text = activity.getString(R.string.legal_privacy_text)
        assertTrue(text.contains("does not request network access"))
        assertTrue(text.contains("does not collect usage data"))
        assertTrue(text.contains("Share usage statistics"))
    }

    @Test
    fun `the app declares no INTERNET permission`() {
        val permissions = activity.packageManager
            .getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
        assertFalse(permissions?.contains("android.permission.INTERNET") ?: false)
    }
}
