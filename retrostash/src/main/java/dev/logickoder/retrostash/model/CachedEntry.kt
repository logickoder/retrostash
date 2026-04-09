package dev.logickoder.retrostash.model

/** In-memory representation of a cached payload and optional content type. */
data class CachedEntry(
    val body: ByteArray,
    val contentType: String?,
    val statusCode: Int,
    val statusMessage: String,
    val headers: List<Pair<String, String>>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedEntry

        if (!body.contentEquals(other.body)) return false
        if (contentType != other.contentType) return false
        if (statusCode != other.statusCode) return false
        if (statusMessage != other.statusMessage) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = body.contentHashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + statusCode
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + headers.hashCode()
        return result
    }
}