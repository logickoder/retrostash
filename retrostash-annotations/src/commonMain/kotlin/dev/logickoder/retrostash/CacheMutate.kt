package dev.logickoder.retrostash

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheMutate(
    val invalidate: Array<String> = [],
    val invalidates: Array<String> = [],
)
