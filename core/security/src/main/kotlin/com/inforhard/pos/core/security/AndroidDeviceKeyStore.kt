package com.inforhard.pos.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.ProviderException
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class AndroidDeviceKeyStore(
    private val preferStrongBox: Boolean = true,
) : DeviceKeyStore {
    override fun hasKey(): Boolean = keyStore().containsAlias(KEY_ALIAS)

    @Synchronized
    override fun createKeyIfMissing(): PublicKey {
        existingPublicKey()?.let { return it }

        val shouldTryStrongBox = preferStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        return try {
            generateKey(useStrongBox = shouldTryStrongBox)
        } catch (failure: ProviderException) {
            if (!shouldTryStrongBox) throw failure
            generateKey(useStrongBox = false)
        }
    }

    override fun sign(payload: ByteArray): ByteArray {
        val privateKey = keyStore().getKey(KEY_ALIAS, null) as? PrivateKey
            ?: error("Device key is not installed")
        return Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(privateKey)
            update(payload)
            sign()
        }
    }

    private fun existingPublicKey(): PublicKey? =
        keyStore().getCertificate(KEY_ALIAS)?.publicKey

    private fun generateKey(useStrongBox: Boolean): PublicKey {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setIsStrongBoxBacked(useStrongBox)
                }
            }
            .build()

        return KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEY_STORE,
        ).run {
            initialize(spec)
            generateKeyPair().public
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "ih_pos_device_identity_v1"
        const val EC_CURVE = "secp256r1"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }
}
