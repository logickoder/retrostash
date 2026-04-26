package dev.logickoder.retrostash

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheQuery(
    val key: String,
    val maxAgeSeconds: Long = 0L,
)
