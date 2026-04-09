package dev.logickoder.retrostash

import android.content.Context
import android.content.SharedPreferences
import dev.logickoder.retrostash.model.CachedEntry
import dev.logickoder.retrostash.model.Entry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Persistent bounded cache for successful POST query responses.
 */
class PostResponseCacheStore(
    context: Context,
    private val maxEntries: Int = 32,
    private val maxBytes: Long = 2 * 1024 * 1024L,
    private val ttlMs: Long = 10 * 60 * 1000L,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cacheDir: File = File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    private val lock = ReentrantLock()

    private val index: LinkedHashMap<String, Entry> = loadIndex()
    private var totalBytes: Long = index.values.sumOf { it.size }

    /** Returns cached payload for [key] when entry exists and is fresh. */
    fun get(key: String): CachedEntry? {
        val fileName: String
        val contentType: String?
        val statusCode: Int
        val statusMessage: String
        val headers: List<Pair<String, String>>
        val entrySize: Long
        val now = System.currentTimeMillis()

        lock.withLock {
            val entry = index[key] ?: return null
            if (now > entry.expiresAt) {
                index.remove(key)
                totalBytes -= entry.size
                File(cacheDir, entry.fileName).delete()
                persistIndex()
                return null
            }

            fileName = entry.fileName
            contentType = entry.contentType
            statusCode = entry.statusCode
            statusMessage = entry.statusMessage
            headers = entry.headers
            entrySize = entry.size
        }

        return runCatching {
            CachedEntry(
                body = File(cacheDir, fileName).readBytes(),
                contentType = contentType,
                statusCode = statusCode,
                statusMessage = statusMessage,
                headers = headers,
            )
        }.getOrElse {
            lock.withLock {
                if (index.remove(key) != null) {
                    totalBytes -= entrySize
                    persistIndex()
                }
            }
            null
        }
    }

    /** Stores [payload] and enforces TTL + LRU bounds. */
    fun put(
        key: String,
        payload: ByteArray,
        contentType: String?,
        statusCode: Int,
        statusMessage: String,
        headers: List<Pair<String, String>>,
    ) {
        val fileName = "${sha256(key)}.cache"

        runCatching { File(cacheDir, fileName).writeBytes(payload) }.onFailure { return }

        val now = System.currentTimeMillis()
        val filesToDelete = mutableListOf<String>()

        lock.withLock {
            index.remove(key)?.let { old ->
                totalBytes -= old.size
                if (old.fileName != fileName) filesToDelete += old.fileName
            }
            index[key] = Entry(
                fileName = fileName,
                contentType = contentType,
                statusCode = statusCode,
                statusMessage = statusMessage,
                headers = headers,
                size = payload.size.toLong(),
                createdAt = now,
                lastAccess = now,
                expiresAt = now + ttlMs
            )
            totalBytes += payload.size
            evictExpired(filesToDelete)
            evictLru(filesToDelete)
            persistIndex()
        }

        filesToDelete.forEach { File(cacheDir, it).delete() }
    }

    fun clear() {
        lock.withLock {
            index.clear()
            totalBytes = 0L
            prefs.edit { remove(PREF_INDEX) }
        }
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    private fun evictExpired(filesToDelete: MutableList<String>) {
        val now = System.currentTimeMillis()
        val iter = index.iterator()
        while (iter.hasNext()) {
            val (_, entry) = iter.next()
            if (entry.expiresAt <= now) {
                filesToDelete += entry.fileName
                totalBytes -= entry.size
                iter.remove()
            }
        }
    }

    private fun evictLru(filesToDelete: MutableList<String>) {
        val iter = index.iterator()
        while ((index.size > maxEntries || totalBytes > maxBytes) && iter.hasNext()) {
            val (_, entry) = iter.next()
            filesToDelete += entry.fileName
            totalBytes -= entry.size
            iter.remove()
        }
    }

    private fun persistIndex() {
        val json = JSONObject()
        index.forEach { (key, entry) ->
            json.put(key, JSONObject().apply {
                put(F_FILE, entry.fileName)
                put(F_CONTENT_TYPE, entry.contentType ?: "")
                put(F_STATUS_CODE, entry.statusCode)
                put(F_STATUS_MESSAGE, entry.statusMessage)
                put(F_HEADERS, JSONArray().apply {
                    entry.headers.forEach { (name, value) ->
                        put(JSONArray().apply {
                            put(name)
                            put(value)
                        })
                    }
                })
                put(F_SIZE, entry.size)
                put(F_CREATED_AT, entry.createdAt)
                put(F_LAST_ACCESS, entry.lastAccess)
                put(F_EXPIRES_AT, entry.expiresAt)
            })
        }
        prefs.edit { putString(PREF_INDEX, json.toString()) } // apply() async
    }

    private fun loadIndex(): LinkedHashMap<String, Entry> {
        val raw = prefs.getString(PREF_INDEX, null)
            ?: return LinkedHashMap(16, 0.75f, true)
        val json = runCatching { JSONObject(raw) }.getOrNull()
            ?: return LinkedHashMap(16, 0.75f, true)
        val now = System.currentTimeMillis()
        val entries = mutableListOf<Pair<String, Entry>>()

        json.keys().forEach { key ->
            val obj = json.optJSONObject(key) ?: return@forEach
            val expiresAt = obj.optLong(F_EXPIRES_AT, 0L)
            val fileName = obj.optString(F_FILE).ifBlank { return@forEach }
            if (expiresAt <= now) {
                File(cacheDir, fileName).delete()
                return@forEach
            }
            entries += key to Entry(
                fileName = fileName,
                contentType = obj.optString(F_CONTENT_TYPE).ifBlank { null },
                statusCode = obj.optInt(F_STATUS_CODE, 200),
                statusMessage = obj.optString(F_STATUS_MESSAGE).ifBlank { "OK" },
                headers = buildList {
                    val arr = obj.optJSONArray(F_HEADERS) ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val pair = arr.optJSONArray(i) ?: continue
                        val name = pair.optString(0).orEmpty()
                        val value = pair.optString(1).orEmpty()
                        if (name.isNotBlank()) add(name to value)
                    }
                },
                size = obj.optLong(F_SIZE, 0L),
                createdAt = obj.optLong(F_CREATED_AT, 0L),
                lastAccess = obj.optLong(F_LAST_ACCESS, 0L),
                expiresAt = expiresAt
            )
        }

        return LinkedHashMap<String, Entry>(maxOf(entries.size, 16), 0.75f, true).also { map ->
            entries.sortedBy { it.second.lastAccess }.forEach { (k, v) -> map[k] = v }
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val PREFS_NAME = "retrostash_cache"
        private const val PREF_INDEX = "post_lru_index"
        private const val CACHE_DIR_NAME = PREFS_NAME

        private const val F_FILE = "f"
        private const val F_CONTENT_TYPE = "ct"
        private const val F_STATUS_CODE = "sc"
        private const val F_STATUS_MESSAGE = "sm"
        private const val F_HEADERS = "h"
        private const val F_SIZE = "s"
        private const val F_CREATED_AT = "ca"
        private const val F_LAST_ACCESS = "la"
        private const val F_EXPIRES_AT = "ea"

        /** Clears all persisted entries and index for the cache store. */
        fun clear(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                remove(PREF_INDEX)
            }
            File(context.cacheDir, CACHE_DIR_NAME).deleteRecursively()
        }
    }
}