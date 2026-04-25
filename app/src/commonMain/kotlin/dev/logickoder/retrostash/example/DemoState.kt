package dev.logickoder.retrostash.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import dev.logickoder.retrostash.core.RetrostashStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DemoState(private val scope: CoroutineScope) {

    private val ktorStore: RetrostashStore = InMemoryRetrostashStore()
    private val okhttpStore: RetrostashStore = InMemoryRetrostashStore()

    val events = mutableStateListOf<DemoEvent>()
    var lastResult by mutableStateOf<DemoResult?>(null)
        private set
    var transport by mutableStateOf(Transport.Ktor)
    var postId by mutableStateOf(1)
    var busy by mutableStateOf(false)
        private set

    private val ktorEngine: DemoEngine =
        KtorDemoEngine(store = ktorStore, onLog = ::logFromEngine)

    private val okhttpEngine: DemoEngine? =
        if (isOkHttpSupported) {
            createOkHttpDemoEngine(store = okhttpStore, onLog = ::logFromEngine)
        } else {
            null
        }

    private fun logFromEngine(message: String) {
        appendEvent(message)
    }

    private fun engine(): DemoEngine = when (transport) {
        Transport.Ktor -> ktorEngine
        Transport.OkHttp -> okhttpEngine ?: ktorEngine
    }

    fun availableTransports(): List<Transport> = buildList {
        add(Transport.Ktor)
        if (isOkHttpSupported && okhttpEngine != null) add(Transport.OkHttp)
    }

    fun runQuery() {
        if (busy) return
        scope.launch {
            busy = true
            try {
                appendEvent("→ ${transport.label}: GET /posts/$postId")
                val result = engine().runQuery(postId)
                lastResult = result
                appendEvent(
                    "← ${transport.label}: ${result.statusCode} (${result.source}, ${result.sizeBytes}B, ${result.durationMs}ms)"
                )
            } catch (e: Throwable) {
                appendEvent("× ${transport.label} query failed: ${e.message}", isError = true)
            } finally {
                busy = false
            }
        }
    }

    fun runMutation() {
        if (busy) return
        scope.launch {
            busy = true
            try {
                appendEvent("→ ${transport.label}: POST /posts/$postId (mutate)")
                val result = engine().runMutation(postId)
                lastResult = result
                appendEvent(
                    "← ${transport.label}: ${result.statusCode} (${result.sizeBytes}B, ${result.durationMs}ms) — invalidated posts/{id}"
                )
            } catch (e: Throwable) {
                appendEvent("× ${transport.label} mutation failed: ${e.message}", isError = true)
            } finally {
                busy = false
            }
        }
    }

    fun clearCache() {
        if (busy) return
        scope.launch {
            busy = true
            try {
                engine().clearCache()
                appendEvent("⌫ ${transport.label}: cache cleared")
            } finally {
                busy = false
            }
        }
    }

    fun clearLog() {
        events.clear()
        lastResult = null
    }

    private fun appendEvent(message: String, isError: Boolean = false) {
        events.add(0, DemoEvent(timestampMs = nowMs(), message = message, isError = isError))
        if (events.size > 100) {
            while (events.size > 100) events.removeAt(events.lastIndex)
        }
    }

    fun shutdown() {
        ktorEngine.close()
        okhttpEngine?.close()
    }
}
