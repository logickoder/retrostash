package dev.logickoder.retrostash.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.logickoder.retrostash.core.InMemoryRetrostashStore
import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.example.domain.DemoEvent
import dev.logickoder.retrostash.example.domain.KtorDemoEngine
import dev.logickoder.retrostash.example.domain.Transport
import dev.logickoder.retrostash.example.model.DemoResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DemoState(private val scope: CoroutineScope) {
    private val ktorStore: RetrostashStore = InMemoryRetrostashStore()
    private val okhttpStore: RetrostashStore = InMemoryRetrostashStore()
    private val retrofitStore: RetrostashStore = InMemoryRetrostashStore()

    val events = mutableStateListOf<DemoEvent>()
    var lastResult by mutableStateOf<DemoResult?>(null)
        private set
    var transport by mutableStateOf(Transport.Ktor)
    var postId by mutableStateOf(1)
    var busy by mutableStateOf(false)
        private set

    private val ktorEngine: DemoEngine = KtorDemoEngine(
        store = ktorStore,
        onLog = ::logFromEngine,
    )

    private val okhttpEngine =
        Platform.createOkHttpEngine(store = okhttpStore, onLog = ::logFromEngine)

    private val retrofitEngine =
        Platform.createRetrofitEngine(store = retrofitStore, onLog = ::logFromEngine)

    private fun logFromEngine(message: String) {
        appendEvent(message)
    }

    private fun engine(): DemoEngine = when (transport) {
        Transport.Ktor -> ktorEngine
        Transport.OkHttp -> okhttpEngine ?: ktorEngine
        Transport.Retrofit -> retrofitEngine ?: ktorEngine
    }

    fun availableTransports(): List<Transport> = buildList {
        add(Transport.Ktor)
        if (okhttpEngine != null) add(Transport.OkHttp)
        if (retrofitEngine != null) add(Transport.Retrofit)
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
                appendEvent("→ ${transport.label}: PUT /posts/$postId (mutate)")
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
        events.add(
            0,
            DemoEvent(timestampMs = Platform.nowMs(), message = message, isError = isError)
        )
        Logger.d("DemoEngine", message)
        if (events.size > 100) {
            while (events.size > 100) events.removeAt(events.lastIndex)
        }
    }

    fun shutdown() {
        ktorEngine.close()
        okhttpEngine?.close()
        retrofitEngine?.close()
    }
}

@Composable
fun rememberDemoState(): DemoState {
    val scope = rememberCoroutineScope()
    val state = remember { DemoState(scope) }

    DisposableEffect(state) {
        onDispose { state.shutdown() }
    }

    return state
}