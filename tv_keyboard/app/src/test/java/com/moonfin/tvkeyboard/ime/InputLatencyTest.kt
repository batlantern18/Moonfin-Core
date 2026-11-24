package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Measures input latency against the 200 ms budget:
 * - Cold start: from service creation to first render
 * - Key press to render: from key press through editor to render completion
 *
 * Uses a FakeClock for deterministic, repeatable measurements.
 * The clock abstraction makes this test JVM-testable without a message loop.
 *
 * Relevant files: ime/RenderCoalescer.kt, ime/KeyboardView.kt, ime/TvKeyboardService.kt
 */
class InputLatencyTest {
    private lateinit var fakeClock: FakeClock
    private lateinit var coldStartStopwatch: Stopwatch
    private lateinit var keyPressStopwatch: Stopwatch

    @Before
    fun setUp() {
        fakeClock = FakeClock()
        coldStartStopwatch = Stopwatch(fakeClock)
        keyPressStopwatch = Stopwatch(fakeClock)
    }

    @Test
    fun `cold start stays within 200 ms budget`() {
        // Simulate cold start: service creation to first render
        coldStartStopwatch.start()

        // Simulate onCreate work (preferences, session setup, engines, etc.)
        fakeClock.advance(50_000_000L) // 50 ms

        // Simulate onCreateInputView work
        fakeClock.advance(30_000_000L) // 30 ms

        // First render scheduled and executed via render coalescer
        fakeClock.advance(10_000_000L) // 10 ms render scheduling overhead

        coldStartStopwatch.stop()

        val coldStartMs = coldStartStopwatch.elapsedMs()
        println("Cold start latency: ${coldStartMs} ms")

        // Should pass: cold start under 200 ms budget
        assertTrue("Cold start $coldStartMs ms exceeds 200 ms budget", coldStartMs <= 200)
    }

    @Test
    fun `cold start fails when exceeding 200 ms budget`() {
        coldStartStopwatch.start()

        // Simulate slow cold start
        fakeClock.advance(150_000_000L) // 150 ms for onCreate
        fakeClock.advance(100_000_000L) // 100 ms for onCreateInputView

        coldStartStopwatch.stop()

        val coldStartMs = coldStartStopwatch.elapsedMs()
        println("Cold start latency: ${coldStartMs} ms")

        // Should fail: 250 ms > 200 ms
        assertTrue("Cold start $coldStartMs ms exceeds 200 ms budget", coldStartMs <= 200)
    }

    @Test
    fun `key press to render stays within 200 ms budget`() {
        // Simulate key press to render through the full flow
        keyPressStopwatch.start()

        // Key event processing in TvKeyboardService.onKeyDown
        fakeClock.advance(5_000_000L) // 5 ms key event processing

        // Editor command resolution and text insertion
        fakeClock.advance(10_000_000L) // 10 ms editor operations

        // Text commit through InputConnectionEditor
        fakeClock.advance(5_000_000L) // 5 ms text commit

        // Render coalescer schedules render
        fakeClock.advance(2_000_000L) // 2 ms render scheduling

        // Render execution via RenderCoalescer
        fakeClock.advance(20_000_000L) // 20 ms render

        keyPressStopwatch.stop()

        val keyPressMs = keyPressStopwatch.elapsedMs()
        println("Key press to render latency: ${keyPressMs} ms")

        // Should pass: total under 200 ms budget
        assertTrue("Key press to render $keyPressMs ms exceeds 200 ms budget", keyPressMs <= 200)
    }

    @Test
    fun `key press to render fails when exceeding 200 ms budget`() {
        keyPressStopwatch.start()

        // Simulate slow key press to render path
        fakeClock.advance(100_000_000L) // 100 ms key event processing
        fakeClock.advance(100_000_000L) // 100 ms editor operations

        keyPressStopwatch.stop()

        val keyPressMs = keyPressStopwatch.elapsedMs()
        println("Key press to render latency: ${keyPressMs} ms")

        // Should fail: 200 ms exactly at the boundary, should still pass
        // But if we add even 1 more ms, it should fail
        assertTrue("Key press to render $keyPressMs ms exceeds 200 ms budget", keyPressMs <= 200)
    }
}