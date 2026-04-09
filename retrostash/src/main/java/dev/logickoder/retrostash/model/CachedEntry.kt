package dev.logickoder.retrostash.model

data class CachedEntry(val body: ByteArray, val contentType: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedEntry

        if (!body.contentEquals(other.body)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = body.contentHashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        return result
    }
}