package com.alessandrolattao.lanotifica.network

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthMonitorTest {

    @Test
    fun `ConnectionState enum has expected values`() {
        val states = HealthMonitor.ConnectionState.entries

        assertEquals(3, states.size)
        assertEquals(HealthMonitor.ConnectionState.DISCONNECTED, states[0])
        assertEquals(HealthMonitor.ConnectionState.CONNECTING, states[1])
        assertEquals(HealthMonitor.ConnectionState.CONNECTED, states[2])
    }

    @Test
    fun `ConnectionState DISCONNECTED has correct name`() {
        assertEquals("DISCONNECTED", HealthMonitor.ConnectionState.DISCONNECTED.name)
    }

    @Test
    fun `ConnectionState CONNECTING has correct name`() {
        assertEquals("CONNECTING", HealthMonitor.ConnectionState.CONNECTING.name)
    }

    @Test
    fun `ConnectionState CONNECTED has correct name`() {
        assertEquals("CONNECTED", HealthMonitor.ConnectionState.CONNECTED.name)
    }
}
