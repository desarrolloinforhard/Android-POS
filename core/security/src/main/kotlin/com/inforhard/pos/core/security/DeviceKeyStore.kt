package com.inforhard.pos.core.security

import java.security.PublicKey

interface DeviceKeyStore {
    fun createKeyIfMissing(): PublicKey
    fun sign(payload: ByteArray): ByteArray
    fun hasKey(): Boolean
}

