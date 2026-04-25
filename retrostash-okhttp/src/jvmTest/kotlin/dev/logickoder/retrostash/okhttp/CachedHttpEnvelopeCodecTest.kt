package dev.logickoder.retrostash.okhttp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CachedHttpEnvelopeCodecTest {

    @Test
    fun encode_decode_round_trip_preserves_fields() {
        val original = CachedHttpEnvelope(
            payload = "hello".encodeToByteArray(),
            contentType = "application/json",
            statusCode = 201,
            statusMessage = "Created",
            headers = listOf("X-Test" to "yes", "Cache-Control" to "no-store"),
        )

        val encoded = CachedHttpEnvelopeCodec.encode(original)
        val decoded = CachedHttpEnvelopeCodec.decode(encoded)

        requireNotNull(decoded)
        assertArrayEquals(original.payload, decoded.payload)
        assertEquals(original.contentType, decoded.contentType)
        assertEquals(original.statusCode, decoded.statusCode)
        assertEquals(original.statusMessage, decoded.statusMessage)
        assertEquals(original.headers, decoded.headers)
    }

    @Test
    fun decode_returns_null_for_non_envelope_payload() {
        val decoded = CachedHttpEnvelopeCodec.decode("plain-body".encodeToByteArray())
        assertNull(decoded)
    }
}
