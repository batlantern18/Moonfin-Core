package com.moonfin.tvkeyboard.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ClipboardControllerTest {

    private lateinit var context: Context
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var controller: ClipboardController
    private var pastedText: String? = null
    private var dismissed: Boolean = false

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        clipboardManager = mock(ClipboardManager::class.java)
        `when`(context.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(clipboardManager)

        pastedText = null
        dismissed = false
        controller = ClipboardController(
            context = context,
            onPaste = { pastedText = it },
            onDismiss = { dismissed = true },
        )
    }

    @Test
    fun `hasText returns false when primaryClip is null`() {
        `when`(clipboardManager.primaryClip).thenReturn(null)
        assertFalse(controller.hasText())
    }

    @Test
    fun `hasText returns false when primaryClip has zero items`() {
        val clipData = mock(ClipData::class.java)
        `when`(clipData.itemCount).thenReturn(0)
        `when`(clipboardManager.primaryClip).thenReturn(clipData)
        assertFalse(controller.hasText())
    }

    @Test
    fun `hasText returns true when primaryClip has text`() {
        val clipData = mock(ClipData::class.java)
        val item = mock(ClipData.Item::class.java)
        `when`(item.text).thenReturn("Copied content")
        `when`(clipData.itemCount).thenReturn(1)
        `when`(clipData.getItemAt(0)).thenReturn(item)
        `when`(clipboardManager.primaryClip).thenReturn(clipData)

        assertTrue(controller.hasText())
    }

    @Test
    fun `snapshot adds primary clip to history and deduplicates`() {
        val clipData = mock(ClipData::class.java)
        val item = mock(ClipData.Item::class.java)
        `when`(item.text).thenReturn("first clip")
        `when`(clipData.itemCount).thenReturn(1)
        `when`(clipData.getItemAt(0)).thenReturn(item)
        `when`(clipboardManager.primaryClip).thenReturn(clipData)

        val clips1 = controller.snapshot()
        assertEquals(1, clips1.size)
        assertEquals("first clip", clips1[0].text)

        val clips2 = controller.snapshot()
        assertEquals(1, clips2.size)

        val item2 = mock(ClipData.Item::class.java)
        `when`(item2.text).thenReturn("second clip")
        `when`(clipData.getItemAt(0)).thenReturn(item2)

        val clips3 = controller.snapshot()
        assertEquals(2, clips3.size)
        assertEquals("second clip", clips3[0].text)
        assertEquals("first clip", clips3[1].text)
    }

    @Test
    fun `snapshot caps history at MAX_CLIPS`() {
        val clipData = mock(ClipData::class.java)
        `when`(clipData.itemCount).thenReturn(1)
        `when`(clipboardManager.primaryClip).thenReturn(clipData)

        for (i in 1..15) {
            val item = mock(ClipData.Item::class.java)
            `when`(item.text).thenReturn("clip $i")
            `when`(clipData.getItemAt(0)).thenReturn(item)
            controller.snapshot()
        }

        val clips = controller.snapshot()
        assertEquals(ClipboardController.MAX_CLIPS, clips.size)
        assertEquals("clip 15", clips.first().text)
    }

    @Test
    fun `clear removes all history`() {
        val clipData = mock(ClipData::class.java)
        val item = mock(ClipData.Item::class.java)
        `when`(item.text).thenReturn("some text")
        `when`(clipData.itemCount).thenReturn(1)
        `when`(clipData.getItemAt(0)).thenReturn(item)
        `when`(clipboardManager.primaryClip).thenReturn(clipData)

        controller.snapshot()
        controller.clear()

        `when`(clipboardManager.primaryClip).thenReturn(null)
        assertTrue(controller.snapshot().isEmpty())
    }

    @Test
    fun `ClipItem equality and text accessor`() {
        val item1 = ClipboardController.ClipItem("abc")
        val item2 = ClipboardController.ClipItem("abc")
        assertEquals(item1, item2)
        assertEquals("abc", item1.text)
    }
}
