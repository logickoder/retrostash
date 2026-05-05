package dev.logickoder.retrostash.internal

/**
 * Marks declarations that are an implementation detail shared between the OkHttp and Ktor
 * adapters. These declarations have NO compatibility guarantees and may change in any release —
 * do not depend on them from consumer code.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Retrostash API and may change without notice.",
)
@Retention(AnnotationRetention.BINARY)
annotation class RetrostashInternalApi
