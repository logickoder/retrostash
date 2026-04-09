package dev.logickoder.retrostash

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

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

    fun consumeIfDirty(key: String): Boolean {
        if (key.isBlank()) return false

        val now = System.currentTimeMillis()

        val ts = map.remove(key) ?: return false

        val dirty = now - ts <= ttlMs
        if (dirty) persist()

        return dirty
    }

    fun clear() {
        map.clear()
        prefs.edit { remove(PREF_KEY) }
    }

    private fun persist() {
        val snapshot = JSONObject()
        map.forEach { (k, v) -> snapshot.put(k, v) }
        prefs.edit { putString(PREF_KEY, snapshot.toString()) } // apply() via androidx ktx
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