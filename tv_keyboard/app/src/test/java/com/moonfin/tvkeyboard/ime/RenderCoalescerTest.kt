package com.moonfin.tvkeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RenderCoalescerTest {

    private val scheduled = mutableListOf<() -> Unit>()
    private var renders = 0
    private lateinit var coalescer: RenderCoalescer

    @Before
    fun setUp() {
        scheduled.clear()
        renders = 0
        coalescer = RenderCoalescer(
            scheduler = { block -> scheduled.add(block) },
            render = { renders++ },
        )
    }

    @Test
    fun `a burst of requests schedules one render`() {
        coalescer.request()
        coalescer.request()
        coalescer.request()

        assertEquals(1, scheduled.size)
    }

    @Test
    fun `the scheduled render runs once`() {
        coalescer.request()
        coalescer.request()

        scheduled.forEach { it() }

        assertEquals(1, renders)
    }

    @Test
    fun `a request after the render runs schedules another`() {
        coalescer.request()
        scheduled.removeAt(0).invoke()

        coalescer.request()
        scheduled.forEach { it() }

        assertEquals(2, renders)
    }

    @Test
    fun `a request from inside the render schedules the next one`() {
        var once = true
        coalescer = RenderCoalescer(
            scheduler = { block -> scheduled.add(block) },
            render = {
                renders++
                if (once) {
                    once = false
                    coalescer.request()
                }
            },
        )

        coalescer.request()
        while (scheduled.isNotEmpty()) scheduled.removeAt(0).invoke()

        assertEquals(2, renders)
    }
}
