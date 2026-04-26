package dev.logickoder.retrostash.okhttp

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

internal object CachedHttpEnvelopeCodec {
    private const val MAGIC = 0x52534831 // RSH1

    fun encode(envelope: CachedHttpEnvelope): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(envelope.statusCode)
            writeString(data, envelope.statusMessage)
            writeNullableString(data, envelope.contentType)
            data.writeInt(envelope.headers.size)
            envelope.headers.forEach { (name, value) ->
                writeString(data, name)
                writeString(data, value)
            }
            data.writeInt(envelope.payload.size)
            data.write(envelope.payload)
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): CachedHttpEnvelope? {
        return runCatching {
            DataInputStream(ByteArrayInputStream(bytes)).use { data ->
                val magic = data.readInt()
                if (magic != MAGIC) return null

                val statusCode = data.readInt()
                val statusMessage = readString(data)
                val contentType = readNullableString(data)
                val headerCount = data.readInt()
                if (headerCount < 0) return null

                val headers = buildList {
                    repeat(headerCount) {
                        add(readString(data) to readString(data))
                    }
                }

                val payloadSize = data.readInt()
                if (payloadSize < 0) return null
                val payload = ByteArray(payloadSize)
                data.readFully(payload)

                CachedHttpEnvelope(
                    payload = payload,
                    contentType = contentType,
                    statusCode = statusCode,
                    statusMessage = statusMessage,
                    headers = headers,
                )
            }
        }.getOrNull()
    }

    private fun writeNullableString(data: DataOutputStream, value: String?) {
        if (value == null) {
            data.writeBoolean(false)
            return
        }
        data.writeBoolean(true)
        writeString(data, value)
    }

    private fun readNullableString(data: DataInputStream): String? {
        val exists = data.readBoolean()
        if (!exists) return null
        return readString(data)
    }

    private fun writeString(data: DataOutputStream, value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        data.writeInt(encoded.size)
        data.write(encoded)
    }

    private fun readString(data: DataInputStream): String {
        val size = data.readInt()
        require(size >= 0)
        val bytes = ByteArray(size)
        data.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
