package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Manages voice input via Android's SpeechRecognizer.
 *
 * One listener drives the whole session: partial results stream to the state
 * callback for the top bar, and only the final result reaches the text editor.
 * The recognizer sits behind [recognizerFactory] so the listener and the field
 * guard can be exercised without a microphone or a device.
 */
class VoiceInputController(
    private val context: Context,
    private val onResult: (text: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val recognizerFactory: (Context) -> SpeechRecognizer = {
        SpeechRecognizer.createSpeechRecognizer(it)
    },
) {
    private var recognizer: SpeechRecognizer? = null

    var state: VoiceState = VoiceState.IDLE
        private set

    /** Errors and the live transcript travel together with the state. */
    var onStateChanged: ((VoiceState, String, String) -> Unit)? = null

    /** A non-empty final result was delivered and focus can move to the action. */
    var onDictationComplete: (() -> Unit)? = null

    private val listener: RecognitionListener = createListener()

    /** The listener installed on the recognizer, exposed as a test seam. */
    fun recognitionListener(): RecognitionListener = listener

    fun start(fieldMode: FieldMode = FieldMode.TEXT) {
        // A disallowed field must not even create a recognizer.
        if (!fieldMode.allowsVoice || state == VoiceState.LISTENING) return

        if (!hasMicrophonePermission()) {
            fail("Microphone permission not granted. Enable in Settings > Apps > Moonfin Keyboard > Permissions.")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            fail("Speech recognition not available on this device.")
            return
        }

        val next = try {
            recognizerFactory(context)
        } catch (_: Exception) {
            fail("Failed to start voice input.")
            return
        }
        recognizer = next
        next.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            next.startListening(intent)
        } catch (_: Exception) {
            disposeRecognizer()
            fail("Failed to start voice input.")
        }
    }

    fun stop() {
        recognizer?.stopListening()
        disposeRecognizer()
        if (state != VoiceState.IDLE) {
            state = VoiceState.IDLE
            emit()
        }
    }

    private fun fail(message: String) {
        state = VoiceState.ERROR
        emit(error = message)
        this@VoiceInputController.onError(message)
    }

    private fun emit(error: String = "", partial: String = "") {
        onStateChanged?.invoke(state, error, partial)
    }

    private fun disposeRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun hasMicrophonePermission(): Boolean {
        val permission = android.Manifest.permission.RECORD_AUDIO
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle) {
            state = VoiceState.LISTENING
            emit()
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
            disposeRecognizer()
            state = VoiceState.ERROR
            emit(error = message)
            this@VoiceInputController.onError(message)
        }

        override fun onResults(results: Bundle) {
            val text = firstText(results).trim()
            disposeRecognizer()
            state = VoiceState.IDLE
            emit()
            if (text.isNotEmpty()) {
                this@VoiceInputController.onResult(text)
                onDictationComplete?.invoke()
            }
        }

        override fun onPartialResults(partialResults: Bundle) {
            val partial = firstText(partialResults)
            if (partial.isEmpty()) return
            state = VoiceState.LISTENING
            emit(partial = partial)
        }

        override fun onEvent(eventType: Int, params: Bundle) {}
    }

    private fun firstText(bundle: Bundle): String =
        bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
}
