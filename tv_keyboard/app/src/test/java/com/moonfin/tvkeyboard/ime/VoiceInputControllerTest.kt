package com.moonfin.tvkeyboard.ime

import android.content.Context
import android.os.Bundle
import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Dictation is driven through the recognition listener, so partial streams and
 * the final handoff can be exercised without a microphone or a device.
 */
@RunWith(RobolectricTestRunner::class)
class VoiceInputControllerTest {

    private lateinit var context: Context
    private lateinit var controller: VoiceInputController
    private val results = mutableListOf<String>()
    private val states = mutableListOf<Triple<VoiceState, String, String>>()
    private var handoffs = 0

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        results.clear()
        states.clear()
        handoffs = 0
        controller = VoiceInputController(
            context = context,
            onResult = { results += it },
            onError = { },
        ).also {
            it.onStateChanged = { state, error, partial -> states += Triple(state, error, partial) }
            it.onDictationComplete = { handoffs++ }
        }
    }

    private fun bundle(vararg texts: String): Bundle = Bundle().apply {
        putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, ArrayList(texts.toList()))
    }

    @Test
    fun `partial results stream into the voice state without committing text`() {
        val listener = controller.recognitionListener()

        listener.onReadyForSpeech(Bundle())
        listener.onPartialResults(bundle("hello wor"))
        listener.onPartialResults(bundle("hello world"))

        assertEquals(VoiceState.LISTENING, controller.state)
        assertEquals(listOf("", "hello wor", "hello world"), states.map { it.third })
        assertTrue(results.isEmpty())
        assertEquals(0, handoffs)
    }

    @Test
    fun `the latest partial transcript is what the top bar shows while listening`() {
        val listener = controller.recognitionListener()
        listener.onPartialResults(bundle("streaming text"))

        val last = states.last()
        val content = TopBarContent.of(
            fieldMode = FieldMode.TEXT,
            voiceState = last.first,
            voicePartial = last.third,
        )

        assertEquals(TopBarKind.VOICE, content.kind)
        assertEquals("streaming text", content.voiceDisplayText("Listening…", "Unavailable"))
    }

    @Test
    fun `the final result is delivered once and focus hands off after it`() {
        val listener = controller.recognitionListener()
        listener.onPartialResults(bundle("hello"))

        listener.onResults(bundle("hello world"))

        assertEquals(listOf("hello world"), results)
        assertEquals(1, handoffs)
        assertEquals(VoiceState.IDLE, controller.state)
        assertEquals("", states.last().third)
    }

    @Test
    fun `a blank final result never commits text or hands off focus`() {
        controller.recognitionListener().onResults(bundle("  "))

        assertTrue(results.isEmpty())
        assertEquals(0, handoffs)
        assertEquals(VoiceState.IDLE, controller.state)
    }

    @Test
    fun `password fields never create a recognizer or start recognition`() {
        var recognizersCreated = 0
        val guarded = VoiceInputController(
            context = context,
            onResult = { },
            onError = { },
            recognizerFactory = {
                recognizersCreated++
                error("Recognition must not start in a password field")
            },
        )

        guarded.start(FieldMode.PASSWORD)

        assertEquals(0, recognizersCreated)
        assertEquals(VoiceState.IDLE, guarded.state)
    }
}
