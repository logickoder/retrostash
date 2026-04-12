package dev.logickoder.retrostash

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheMutate(
    val invalidates: Array<String>,
)
