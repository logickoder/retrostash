package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrostashOkHttpBridgeTest {

    @Test
    fun install_adds_handle_and_policy_interceptors() {
        val bridge = RetrostashOkHttpBridge(store = InMemoryRetrostashStore())
        val builder = OkHttpClient.Builder()

        bridge.install(builder)
        val client = builder.build()

        val found = RetrostashOkHttpBridge.from(client)
        assertNotNull(found)
        assertEquals(bridge, found)
        assertTrue(client.interceptors.any { it is RetrostashOkHttpHandleInterceptor })
        assertTrue(client.interceptors.any { it is RetrostashOkHttpInterceptor })
    }

    @Test
    fun invalidateQueryKey_returns_false_for_blank_key() {
        val bridge = RetrostashOkHttpBridge(store = InMemoryRetrostashStore())
        val result = bridge.invalidateQueryKey("  ")
        assertEquals(false, result)
    }
}
