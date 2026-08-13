package com.inforhard.pos.core.model

import java.security.SecureRandom

@JvmInline
value class IdempotencyKey(val value: String) {
    init {
        require(HEX_128_BITS.matches(value)) {
            "Idempotency-Key must encode exactly 128 bits as lowercase hexadecimal"
        }
    }

    companion object {
        private val HEX_128_BITS = Regex("[0-9a-f]{32}")
    }
}

class IdempotencyKeyGenerator(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun generate(): IdempotencyKey {
        val bytes = ByteArray(KEY_BYTES)
        secureRandom.nextBytes(bytes)
        return IdempotencyKey(bytes.toHex())
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    private companion object {
        const val KEY_BYTES = 16
    }
}
