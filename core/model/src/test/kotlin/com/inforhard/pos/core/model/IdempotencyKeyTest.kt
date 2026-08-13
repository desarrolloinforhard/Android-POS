package com.inforhard.pos.core.model

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IdempotencyKeyTest {
    @Test
    fun generatorEncodesAll128Bits() {
        val key = IdempotencyKeyGenerator(FixedSecureRandom(0xab.toByte())).generate()

        assertEquals("abababababababababababababababab", key.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUuidBecauseItDoesNotEncode128RandomBits() {
        IdempotencyKey("123e4567-e89b-42d3-a456-426614174000")
    }

    @Test
    fun independentRandomInputsProduceDifferentKeys() {
        val first = IdempotencyKeyGenerator(FixedSecureRandom(0x01)).generate()
        val second = IdempotencyKeyGenerator(FixedSecureRandom(0x02)).generate()

        assertNotEquals(first, second)
    }
}

private class FixedSecureRandom(private val value: Byte) : SecureRandom() {
    override fun nextBytes(bytes: ByteArray) {
        bytes.fill(value)
    }
}
