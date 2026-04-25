package dev.logickoder.retrostash.okhttp

data class RetrostashOkHttpConfig @JvmOverloads constructor(
    val timeoutMs: Long = 250L,
    val maxEntries: Int = 64,
    val maxBytes: Long = 2 * 1024 * 1024L,
    val defaultMaxAgeMs: Long = 10 * 60 * 1000L,
    val getMaxAgeSeconds: Long = 24 * 60 * 60L,
    val enableGetCaching: Boolean = true,
    val logger: ((String) -> Unit)? = null,
    val cacheDirName: String = "retrostash_okhttp_cache",
    val prefsName: String = "retrostash_okhttp_store",
)
