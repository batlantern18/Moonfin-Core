package com.moonfin.tvkeyboard.ime

/**
 * Abstraction for time measurements. Keeps timing code JVM-testable without
 * relying on System.nanoTime or System.currentTimeMillis directly.
 */
interface Clock {
    /** Returns the current time in nanoseconds. */
    fun now(): Long
}

/** Production clock using the system monotonic clock. */
object SystemClock : Clock {
    override fun now(): Long = System.nanoTime()
}

/** Test clock that allows manual control over time. */
class FakeClock : Clock {
    private var currentTime = 0L

    override fun now(): Long = currentTime

    /** Advances the clock by the given nanoseconds. */
    fun advance(nanos: Long) {
        currentTime += nanos
    }

    /** Resets the clock to zero. */
    fun reset() {
        currentTime = 0L
    }
}

/**
 * Measures elapsed time using a [Clock]. Useful for profiling cold start
 * and key-press-to-render latency.
 */
class Stopwatch(private val clock: Clock) {
    private var startTime = 0L
    private var elapsed = 0L
    private var running = false

    /** Starts or resumes the stopwatch. */
    fun start() {
        if (!running) {
            startTime = clock.now()
            running = true
        }
    }

    /** Stops the stopwatch and accumulates the elapsed time. */
    fun stop() {
        if (running) {
            elapsed += clock.now() - startTime
            running = false
        }
    }

    /** Resets the stopwatch to zero. */
    fun reset() {
        elapsed = 0L
        running = false
    }

    /** Returns the elapsed time in milliseconds. */
    fun elapsedMs(): Long {
        if (running) {
            return (elapsed + clock.now() - startTime) / 1_000_000
        }
        return elapsed / 1_000_000
    }
}