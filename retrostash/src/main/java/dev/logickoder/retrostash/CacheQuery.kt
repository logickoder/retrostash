package dev.logickoder.retrostash

/**
 * Marks a Retrofit method as a cacheable query and provides the cache key template.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class CacheQuery(val key: String)