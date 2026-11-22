package com.moonfin.tvkeyboard.ime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives Fire TV's optional keyboard-state broadcasts. The tracer bullet only
 * logs them; later tickets use them to refresh the IME.
 */
class KeyboardStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "keyboard state broadcast: ${intent.action}")
    }

    private companion object {
        const val TAG = "TvKeyboardState"
    }
}
