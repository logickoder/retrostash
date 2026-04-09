package dev.logickoder.retrostash

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks mutation-driven dirty query keys with optional persistence.
 */
class NetworkCacheInvalidator(
    context: Context,
    private val ttlMs: Long = DEFAULT_TTL_MS
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val map = ConcurrentHashMap<String, Long>()

    init {
        val raw = prefs.getString(PREF_KEY, null)
        if (!raw.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            runCatching {
                val json = JSONObject(raw)
                json.keys().forEach { key ->
                    val ts = json.optLong(key, 0L)
                    if (ts > 0L && now - ts <= ttlMs) map[key] = ts
                }
            }
        }
    }

    fun markDirty(keys: Collection<String>) {
        if (keys.isEmpty()) return
        val now = System.currentTimeMillis()
        keys.filter { it.isNotBlank() }.forEach { map[it] = now }
        persist()
    }

    /** Checks and consumes a dirty key in one call. */
    fun consumeIfDirty(key: String): Boolean {
        if (!isDirty(key)) return false
        clearDirty(key)
        return true
    }

    /** Returns true when [key] is dirty and not expired. */
    fun isDirty(key: String): Boolean {
        if (key.isBlank()) return false
        val now = System.currentTimeMillis()
        val ts = map[key] ?: return false
        val dirty = now - ts <= ttlMs
        if (!dirty && map.remove(key) != null) persist()
        return dirty
    }

    /** Removes dirty mark for [key] if present. */
    fun clearDirty(key: String) {
        if (key.isBlank()) return
        if (map.remove(key) != null) persist()
    }

    fun clear() {
        map.clear()
        prefs.edit { remove(PREF_KEY) }
    }

    private fun persist() {
        val snapshot = JSONObject()
        map.forEach { (k, v) -> snapshot.put(k, v) }
        prefs.edit { putString(PREF_KEY, snapshot.toString()) }
    }

    companion object {
        private const val PREFS_NAME = "retrostash_invalidator"
        private const val PREF_KEY = "dirty_index"
        const val DEFAULT_TTL_MS = 24 * 60 * 60 * 1000L

        fun clear(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                clear()
            }
        }
    }
}