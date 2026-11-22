package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Manages voice input via Android's SpeechRecognizer.
 * Inserts recognized text at the cursor position with auto-space handling.
 */
class VoiceInputController(
    private val context: Context,
    private val onResult: (text: String) -> Unit,
    private val onError: (message: String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    var state: VoiceState = VoiceState.IDLE
        private set

    var onStateChanged: ((VoiceState, String) -> Unit)? = null

    fun start() {
        if (!hasMicrophonePermission()) {
            val err = "Microphone permission not granted. Enable in Settings > Apps > Moonfin Keyboard > Permissions."
            state = VoiceState.ERROR
            onStateChanged?.invoke(state, err)
            onError(err)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val err = "Speech recognition not available on this device."
            state = VoiceState.ERROR
            onStateChanged?.invoke(state, err)
            onError(err)
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(createListener())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            val err = "Failed to start voice input."
            state = VoiceState.ERROR
            onStateChanged?.invoke(state, err)
            onError(err)
        }
    }

    fun stop() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        if (state != VoiceState.IDLE) {
            state = VoiceState.IDLE
            onStateChanged?.invoke(state, "")
        }
    }

    private fun hasMicrophonePermission(): Boolean {
        val permission = android.Manifest.permission.RECORD_AUDIO
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle) {
                state = VoiceState.LISTENING
                onStateChanged?.invoke(state, "")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error. Check microphone."
                    SpeechRecognizer.ERROR_CLIENT -> "Client error. Try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error. Connect to the internet."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Check connection."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy. Try again."
                    SpeechRecognizer.ERROR_SERVER -> "Server error. Try again later."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                    else -> "Voice input error."
                }
                state = VoiceState.ERROR
                onStateChanged?.invoke(state, message)
                onError(message)
            }

            override fun onResults(results: android.os.Bundle) {
                state = VoiceState.IDLE
                onStateChanged?.invoke(state, "")
                val words = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = words?.firstOrNull()
                if (text != null && text.isNotBlank()) {
                    onResult(text)
                }
            }

            override fun onPartialResults(partialResults: android.os.Bundle) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle) {}
        }
    }
}
