package dev.logickoder.retrostash.core

data class QueryMetadata(
    val scopeName: String,
    val template: String,
    val bindings: Map<String, String> = emptyMap(),
    val bodyBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QueryMetadata

        if (scopeName != other.scopeName) return false
        if (template != other.template) return false
        if (bindings != other.bindings) return false
        if (!bodyBytes.contentEquals(other.bodyBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scopeName.hashCode()
        result = 31 * result + template.hashCode()
        result = 31 * result + bindings.hashCode()
        result = 31 * result + (bodyBytes?.contentHashCode() ?: 0)
        return result
    }
}
