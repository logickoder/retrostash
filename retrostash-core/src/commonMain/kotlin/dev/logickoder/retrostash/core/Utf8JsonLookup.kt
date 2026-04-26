package dev.logickoder.retrostash.core

object Utf8JsonLookup {

    fun findFirstPrimitiveByKey(payload: ByteArray, key: String): String? {
        val root = JsonParser(payload.decodeToString()).parseValue() ?: return null
        return findInNode(root, key)
    }

    private fun findInNode(node: JsonNode, key: String): String? = when (node) {
        is JsonNode.ObjectNode -> {
            node.entries[key]?.asPrimitiveString()?.let { return it }
            node.entries.values.firstNotNullOfOrNull { findInNode(it, key) }
        }

        is JsonNode.ArrayNode -> node.values.firstNotNullOfOrNull { findInNode(it, key) }
        is JsonNode.StringNode -> null
        is JsonNode.NumberNode -> null
        is JsonNode.BooleanNode -> null
        JsonNode.NullNode -> null
    }

    private fun JsonNode.asPrimitiveString(): String? = when (this) {
        is JsonNode.StringNode -> value
        is JsonNode.NumberNode -> value
        is JsonNode.BooleanNode -> value.toString()
        JsonNode.NullNode -> null
        is JsonNode.ArrayNode -> null
        is JsonNode.ObjectNode -> null
    }

    private sealed class JsonNode {
        data class ObjectNode(val entries: Map<String, JsonNode>) : JsonNode()
        data class ArrayNode(val values: List<JsonNode>) : JsonNode()
        data class StringNode(val value: String) : JsonNode()
        data class NumberNode(val value: String) : JsonNode()
        data class BooleanNode(val value: Boolean) : JsonNode()
        data object NullNode : JsonNode()
    }

    private class JsonParser(private val source: String) {
        private var index: Int = 0

        fun parseValue(): JsonNode? {
            skipWhitespace()
            if (index >= source.length) return null

            return when (source[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonNode.StringNode(parseString())
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> JsonNode.NumberNode(parseNumber())
                else -> null
            }
        }

        private fun parseObject(): JsonNode.ObjectNode {
            expect('{')
            skipWhitespace()
            val result = linkedMapOf<String, JsonNode>()
            if (peek() == '}') {
                expect('}')
                return JsonNode.ObjectNode(result)
            }

            while (index < source.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue() ?: JsonNode.NullNode
                result[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        continue
                    }

                    '}' -> {
                        expect('}')
                        break
                    }

                    else -> break
                }
            }

            return JsonNode.ObjectNode(result)
        }

        private fun parseArray(): JsonNode.ArrayNode {
            expect('[')
            skipWhitespace()
            val result = mutableListOf<JsonNode>()
            if (peek() == ']') {
                expect(']')
                return JsonNode.ArrayNode(result)
            }

            while (index < source.length) {
                val value = parseValue() ?: JsonNode.NullNode
                result += value
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        continue
                    }

                    ']' -> {
                        expect(']')
                        break
                    }

                    else -> break
                }
            }

            return JsonNode.ArrayNode(result)
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (index < source.length) {
                val ch = source[index++]
                if (ch == '"') break
                if (ch == '\\') {
                    if (index >= source.length) break
                    when (val esc = source[index++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> sb.append(parseUnicode())
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        private fun parseUnicode(): Char {
            if (index + 4 > source.length) return '\u0000'
            val hex = source.substring(index, index + 4)
            index += 4
            return hex.toIntOrNull(16)?.toChar() ?: '\u0000'
        }

        private fun parseBoolean(): JsonNode.BooleanNode {
            return if (source.startsWith("true", index)) {
                index += 4
                JsonNode.BooleanNode(true)
            } else {
                index += 5
                JsonNode.BooleanNode(false)
            }
        }

        private fun parseNull(): JsonNode.NullNode {
            if (source.startsWith("null", index)) {
                index += 4
            }
            return JsonNode.NullNode
        }

        private fun parseNumber(): String {
            val start = index
            while (index < source.length) {
                val ch = source[index]
                if (ch !in "-+0123456789.eE") break
                index++
            }
            return source.substring(start, index)
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) {
                index++
            }
        }

        private fun peek(): Char? = if (index < source.length) source[index] else null

        private fun expect(ch: Char) {
            if (index < source.length && source[index] == ch) {
                index++
            }
        }
    }
}
