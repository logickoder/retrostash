package dev.logickoder.retrostash

/**
 * Marks a Retrofit method as a mutation and defines query cache keys to invalidate on success.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheMutate(val invalidate: Array<String>)

