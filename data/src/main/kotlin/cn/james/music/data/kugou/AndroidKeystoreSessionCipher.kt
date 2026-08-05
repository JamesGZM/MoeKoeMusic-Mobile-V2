package cn.james.music.data.kugou

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class EncryptedSessionPayload(
    val initializationVector: ByteArray,
    val ciphertext: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is EncryptedSessionPayload &&
            initializationVector.contentEquals(other.initializationVector) &&
            ciphertext.contentEquals(other.ciphertext)

    override fun hashCode(): Int = 31 * initializationVector.contentHashCode() + ciphertext.contentHashCode()

    override fun toString(): String = "EncryptedSessionPayload(ivBytes=${initializationVector.size}, ciphertextBytes=${ciphertext.size})"
}

interface KugouSessionCipher {
    fun encrypt(plaintext: ByteArray): EncryptedSessionPayload

    fun decrypt(payload: EncryptedSessionPayload): ByteArray

    fun clearKey()
}

class KugouSessionCipherException(
    cause: Throwable,
) : Exception("Kugou session cryptography failed", cause)

@Singleton
class AndroidKeystoreSessionCipher internal constructor(
    private val keyAlias: String,
) : KugouSessionCipher {
    @Inject
    constructor() : this(DEFAULT_KEY_ALIAS)

    override fun encrypt(plaintext: ByteArray): EncryptedSessionPayload =
        cryptography {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(ASSOCIATED_DATA)
            EncryptedSessionPayload(
                initializationVector = cipher.iv.copyOf(),
                ciphertext = cipher.doFinal(plaintext),
            )
        }

    override fun decrypt(payload: EncryptedSessionPayload): ByteArray =
        cryptography {
            require(payload.initializationVector.size == GCM_IV_BYTES) { "Unexpected GCM IV size" }
            require(payload.ciphertext.size >= GCM_TAG_BYTES) { "Ciphertext is too short" }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                requireNotNull(loadKey()) { "Kugou session key is missing" },
                GCMParameterSpec(GCM_TAG_BITS, payload.initializationVector),
            )
            cipher.updateAAD(ASSOCIATED_DATA)
            cipher.doFinal(payload.ciphertext)
        }

    override fun clearKey() {
        cryptography {
            keyStore().deleteEntry(keyAlias)
        }
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey =
        loadKey()
            ?: KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                .apply {
                    init(
                        KeyGenParameterSpec
                            .Builder(
                                keyAlias,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(AES_KEY_BITS)
                            .setRandomizedEncryptionRequired(true)
                            .build(),
                    )
                }.generateKey()

    private fun loadKey(): SecretKey? = keyStore().getKey(keyAlias, null) as? SecretKey

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

    private inline fun <T> cryptography(block: () -> T): T =
        try {
            block()
        } catch (failure: KugouSessionCipherException) {
            throw failure
        } catch (failure: Exception) {
            throw KugouSessionCipherException(failure)
        }

    private companion object {
        const val DEFAULT_KEY_ALIAS = "cn.james.music.kugou.session.v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_BITS = 256
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        const val GCM_IV_BYTES = 12
        val ASSOCIATED_DATA = "cn.james.music:kugou-session:v1".encodeToByteArray()
    }
}
