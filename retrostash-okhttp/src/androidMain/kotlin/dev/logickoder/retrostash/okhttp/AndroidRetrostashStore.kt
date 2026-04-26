package dev.logickoder.retrostash.okhttp

import android.content.Context
import dev.logickoder.retrostash.core.RetrostashStore
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Android-specific [RetrostashStore] backed by:
 *  - `Context.cacheDir/<config.cacheDirName>/` — payload files (one per cached entry).
 *  - `SharedPreferences(<config.prefsName>)` — JSON index of `key → metadata`.
 *
 * Survives process restarts. Capped by [RetrostashOkHttpConfig.maxEntries] and
 * [RetrostashOkHttpConfig.maxBytes] (LRU eviction). Thread-safe via internal monitor.
 */
class AndroidRetrostashStore(
    context: Context,
    private val config: RetrostashOkHttpConfig = RetrostashOkHttpConfig(),
) : RetrostashStore {

    private val prefs = context.getSharedPreferences(config.prefsName, Context.MODE_PRIVATE)
    private val cacheDir = File(context.cacheDir, config.cacheDirName).apply { mkdirs() }
    private val lock = Any()

    private val index = linkedMapOf<String, Entry>()
    private var totalBytes: Long = 0L

    init {
        loadIndex()
    }

    override suspend fun get(key: String): ByteArray? = synchronized(lock) {
        val entry = index[key] ?: return@synchronized null
        if (entry.expiresAt < now()) {
            removeLocked(key, entry)
            persistLocked()
            return@synchronized null
        }

        val file = File(cacheDir, entry.fileName)
        if (!file.exists()) {
            removeLocked(key, entry)
            persistLocked()
            return@synchronized null
        }

        entry.lastAccess = now()
        persistLocked()
        return@synchronized file.readBytes()
    }

    override suspend fun put(key: String, payload: ByteArray, maxAgeMs: Long) = synchronized(lock) {
        if (key.isBlank()) return@synchronized
        val fileName = sha256(key) + ".bin"
        val file = File(cacheDir, fileName)
        file.writeBytes(payload)

        val previous = index.remove(key)
        if (previous != null) {
            totalBytes -= previous.size
            if (previous.fileName != fileName) {
                File(cacheDir, previous.fileName).delete()
            }
        }

        val createdAt = now()
        val ttl = if (maxAgeMs > 0L) maxAgeMs else config.defaultMaxAgeMs
        val entry = Entry(
            fileName = fileName,
            size = payload.size.toLong(),
            createdAt = createdAt,
            lastAccess = createdAt,
            expiresAt = createdAt + ttl,
        )
        index[key] = entry
        totalBytes += entry.size

        evictExpiredLocked()
        evictCapacityLocked()
        persistLocked()
    }

    override suspend fun invalidate(template: String) = synchronized(lock) {
        if (template.isBlank()) return@synchronized
        val keys = index.keys.filter { key -> key == template || key.contains("|$template|") }
        keys.forEach { key ->
            index[key]?.let { removeLocked(key, it) }
        }
        persistLocked()
    }

    override suspend fun clear() = synchronized(lock) {
        index.values.forEach { File(cacheDir, it.fileName).delete() }
        index.clear()
        totalBytes = 0L
        prefs.edit().remove(INDEX_KEY).apply()
    }

    private fun loadIndex() {
        synchronized(lock) {
            index.clear()
            totalBytes = 0L
            val raw = prefs.getString(INDEX_KEY, null).orEmpty()
            if (raw.isBlank()) return

            val root = runCatching { JSONObject(raw) }.getOrNull() ?: return
            val now = now()
            root.keys().forEach { key ->
                val obj = root.optJSONObject(key) ?: return@forEach
                val expiresAt = obj.optLong("ea", 0L)
                val fileName = obj.optString("f").orEmpty()
                val size = obj.optLong("s", 0L)
                val createdAt = obj.optLong("ca", 0L)
                val lastAccess = obj.optLong("la", createdAt)
                if (fileName.isBlank() || expiresAt <= now) {
                    if (fileName.isNotBlank()) File(cacheDir, fileName).delete()
                    return@forEach
                }
                val file = File(cacheDir, fileName)
                if (!file.exists()) return@forEach
                val entry = Entry(fileName, size, createdAt, lastAccess, expiresAt)
                index[key] = entry
                totalBytes += size
            }
            evictCapacityLocked()
            persistLocked()
        }
    }

    private fun evictExpiredLocked() {
        val now = now()
        val toRemove = index.filterValues { it.expiresAt <= now }
        toRemove.forEach { (key, entry) -> removeLocked(key, entry) }
    }

    private fun evictCapacityLocked() {
        if (index.isEmpty()) return
        val keysByLru = index.entries
            .sortedBy { it.value.lastAccess }
            .map { it.key }
            .toMutableList()

        while (index.size > config.maxEntries || totalBytes > config.maxBytes) {
            val key = keysByLru.removeFirstOrNull() ?: break
            val entry = index[key] ?: continue
            removeLocked(key, entry)
        }
    }

    private fun removeLocked(key: String, entry: Entry) {
        index.remove(key)
        totalBytes -= entry.size
        File(cacheDir, entry.fileName).delete()
    }

    private fun persistLocked() {
        val root = JSONObject()
        index.forEach { (key, entry) ->
            root.put(key, JSONObject().apply {
                put("f", entry.fileName)
                put("s", entry.size)
                put("ca", entry.createdAt)
                put("la", entry.lastAccess)
                put("ea", entry.expiresAt)
            })
        }
        prefs.edit().putString(INDEX_KEY, root.toString()).apply()
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private data class Entry(
        val fileName: String,
        val size: Long,
        val createdAt: Long,
        var lastAccess: Long,
        val expiresAt: Long,
    )

    companion object {
        private const val INDEX_KEY = "index"

        fun clear(context: Context, config: RetrostashOkHttpConfig = RetrostashOkHttpConfig()) {
            context.getSharedPreferences(config.prefsName, Context.MODE_PRIVATE)
                .edit()
                .remove(INDEX_KEY)
                .apply()
            File(context.cacheDir, config.cacheDirName).deleteRecursively()
        }
    }
}
