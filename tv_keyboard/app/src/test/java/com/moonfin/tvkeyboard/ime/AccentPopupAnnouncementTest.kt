package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.inputmethodservice.Keyboard
import android.view.View
import com.moonfin.tvkeyboard.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Opening an accent popup tells TalkBack which character a press would insert. */
@RunWith(RobolectricTestRunner::class)
class AccentPopupAnnouncementTest {

    @Test
    fun `opening the popup describes the character it would insert`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val popup = AccentPopupView(context)
        popup.describeKey = { key -> "insert " + key.label }
        popup.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.AT_MOST),
        )
        popup.layout(0, 0, 1080, 400)

        popup.show(R.xml.shift_1)

        val firstLabel = Keyboard(context, R.xml.shift_1).keys.first().label
        assertEquals("insert $firstLabel", popup.contentDescription)
    }
}
