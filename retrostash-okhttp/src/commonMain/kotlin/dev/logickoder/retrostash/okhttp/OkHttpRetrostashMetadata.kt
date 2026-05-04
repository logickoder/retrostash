package dev.logickoder.retrostash.okhttp

/**
 * Per-request OkHttp metadata read by [RetrostashOkHttpInterceptor]. Attach via the
 * [retrostash], [retrostashQuery], or [retrostashMutate] extension on `okhttp3.Request.Builder`.
 *
 * For Retrofit users this is populated automatically from `@CacheQuery` / `@CacheMutate` via
 * [RetrofitMetadataExtractor] — manual attachment is only needed when calling OkHttp directly.
 *
 * @property scopeName Logical namespace (typically the API interface name).
 * @property queryTemplate Cache template; non-null marks this request as a query.
 * @property maxAgeMs TTL in milliseconds for persisted entries. `0` disables persistence.
 * @property bindings Pre-extracted placeholder values.
 * @property invalidateTemplates Templates to clear on a `2xx` mutation response.
 * @property tagTemplates Tag templates to resolve and persist with the cached entry (query side).
 * @property invalidateTagTemplates Tag templates to resolve and clear on a `2xx` mutation
 * response (mutation side, parallel to [invalidateTemplates]).
 */
data class OkHttpRetrostashMetadata(
    val scopeName: String,
    val queryTemplate: String? = null,
    val maxAgeMs: Long = 0L,
    val bindings: Map<String, String> = emptyMap(),
    val invalidateTemplates: List<String> = emptyList(),
    val tagTemplates: List<String> = emptyList(),
    val invalidateTagTemplates: List<String> = emptyList(),
)
