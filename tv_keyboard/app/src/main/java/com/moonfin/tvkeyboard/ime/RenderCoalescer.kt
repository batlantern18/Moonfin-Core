package com.moonfin.tvkeyboard.ime

/**
 * Collapses a burst of render requests into one render.
 *
 * Every writer asks for a render — an edit, a shift change, a language change,
 * a voice state change — and the first request schedules the work while the
 * rest fold into it. The scheduler is injected so the collapse can be tested
 * on the JVM without a message loop.
 */
class RenderCoalescer(
    private val scheduler: ((() -> Unit) -> Unit),
    private val render: () -> Unit,
) {
    private var pending = false

    fun request() {
        if (pending) return
        pending = true
        scheduler {
            pending = false
            render()
        }
    }
}
