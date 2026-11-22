package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Fires haptic and/or key-click sound on key press and popup selection.
 * Honours the system's haptic/sound settings via View.performHapticFeedback
 * and AudioManager.playSoundEffect.
 */
class FeedbackController(
    private val context: Context,
    private var hapticEnabled: Boolean,
    private var soundEnabled: Boolean,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun updatePreferences(haptic: Boolean, sound: Boolean) {
        hapticEnabled = haptic
        soundEnabled = sound
    }

    fun onKeyPress(view: View) {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (soundEnabled) {
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        }
    }

    fun onLongPressOpen(view: View) {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun onPopupSelection(view: View) {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (soundEnabled) {
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        }
    }
}
