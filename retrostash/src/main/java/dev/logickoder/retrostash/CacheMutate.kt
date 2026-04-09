package dev.logickoder.retrostash

/**
 * Marks a Retrofit method as a mutation.
 *
 * When the call succeeds, every template in [invalidate] is resolved against request arguments,
 * then corresponding cached query keys are marked dirty.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheMutate(val invalidate: Array<String>)

