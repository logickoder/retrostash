package dev.logickoder.retrostash

/**
 * Marks a Retrofit method as cacheable and declares the cache key template.
 *
 * Placeholders use `{name}` syntax and can be resolved from `@Path`, `@Query`,
 * or matching fields inside `@Body` payloads.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheQuery(val key: String)