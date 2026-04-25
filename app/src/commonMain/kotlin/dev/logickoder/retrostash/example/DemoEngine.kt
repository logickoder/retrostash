package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.example.domain.Transport
import dev.logickoder.retrostash.example.model.DemoResult

interface DemoEngine {
    val transport: Transport
    suspend fun runQuery(postId: Int): DemoResult
    suspend fun runMutation(postId: Int): DemoResult
    suspend fun clearCache()
    fun close()
}