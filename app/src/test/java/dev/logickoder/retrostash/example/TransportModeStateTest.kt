package dev.logickoder.retrostash.example

import org.junit.Assert.assertEquals
import org.junit.Test

class TransportModeStateTest {

    @Test
    fun toggle_switches_between_okhttp_and_ktor() {
        val state = TransportModeState()

        assertEquals(TransportMode.OKHTTP, state.mode)
        assertEquals(TransportMode.KTOR, state.toggle())
        assertEquals(TransportMode.KTOR, state.mode)
        assertEquals(TransportMode.OKHTTP, state.toggle())
        assertEquals(TransportMode.OKHTTP, state.mode)
    }
}
