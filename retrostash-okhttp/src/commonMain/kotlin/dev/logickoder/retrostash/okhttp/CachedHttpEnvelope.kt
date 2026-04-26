package dev.logickoder.retrostash.okhttp

data class CachedHttpEnvelope(
    val payload: ByteArray,
    val contentType: String?,
    val statusCode: Int,
    val statusMessage: String,
    val headers: List<Pair<String, String>>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedHttpEnvelope

        if (statusCode != other.statusCode) return false
        if (!payload.contentEquals(other.payload)) return false
        if (contentType != other.contentType) return false
        if (statusMessage != other.statusMessage) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + headers.hashCode()
        return result
    }
}
