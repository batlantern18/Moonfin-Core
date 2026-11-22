package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class FeedbackControllerTest {

    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    private lateinit var view: View

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        audioManager = mock(AudioManager::class.java)
        view = mock(View::class.java)
        `when`(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)
    }

    @Test
    fun `onKeyPress performs haptic and sound when both enabled`() {
        val controller = FeedbackController(context, hapticEnabled = true, soundEnabled = true)
        controller.onKeyPress(view)

        verify(view).performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        verify(audioManager).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
    }

    @Test
    fun `onKeyPress performs neither when both disabled`() {
        val controller = FeedbackController(context, hapticEnabled = false, soundEnabled = false)
        controller.onKeyPress(view)

        verify(view, never()).performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        verify(audioManager, never()).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
    }

    @Test
    fun `onLongPressOpen triggers long press haptic when enabled`() {
        val controller = FeedbackController(context, hapticEnabled = true, soundEnabled = false)
        controller.onLongPressOpen(view)

        verify(view).performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    @Test
    fun `onPopupSelection triggers tap and sound when enabled`() {
        val controller = FeedbackController(context, hapticEnabled = true, soundEnabled = true)
        controller.onPopupSelection(view)

        verify(view).performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        verify(audioManager).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
    }

    @Test
    fun `updatePreferences updates feedback state`() {
        val controller = FeedbackController(context, hapticEnabled = false, soundEnabled = false)
        controller.updatePreferences(haptic = true, sound = true)
        controller.onKeyPress(view)

        verify(view).performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        verify(audioManager).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
    }
}
