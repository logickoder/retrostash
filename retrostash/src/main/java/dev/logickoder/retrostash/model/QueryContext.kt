package dev.logickoder.retrostash.model

/** Resolved cache key and method kind for a cacheable Retrofit invocation. */
data class QueryContext(val key: String, val isPost: Boolean)