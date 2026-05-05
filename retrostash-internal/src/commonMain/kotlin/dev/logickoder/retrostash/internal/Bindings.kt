package dev.logickoder.retrostash.internal

/**
 * Coerces an `Any?`-valued binding map into the `String`-valued shape `CoreKeyResolver` expects.
 * Drops null values; calls `toString()` on the rest. Used by the OkHttp and Ktor cache
 * surfaces.
 *
 * Marked [RetrostashInternalApi] — not a public API.
 */
@RetrostashInternalApi
fun Map<String, Any?>.toStringBindings(): Map<String, String> =
    mapNotNull { (k, v) -> v?.toString()?.let { k to it } }.toMap()
