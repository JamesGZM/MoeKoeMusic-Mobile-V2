package cn.james.music.kugou.api.crypto

import java.util.HexFormat
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal data class KugouAuthEncryptedPayload(
    val ciphertextHex: String,
    val temporaryKey: String,
) {
    override fun toString(): String = "KugouAuthEncryptedPayload(ciphertextBytes=${ciphertextHex.length / 2}, temporaryKey=<redacted>)"
}

/** Protocol-only crypto used by KuGou login endpoints at the fixed upstream revision. */
internal object KugouAuthCrypto {
    fun encryptJson(
        json: String,
        temporaryKey: String,
    ): KugouAuthEncryptedPayload {
        validateTemporaryKey(temporaryKey)
        val ciphertext = cipher(Cipher.ENCRYPT_MODE, temporaryKey).doFinal(json.encodeToByteArray())
        return KugouAuthEncryptedPayload(
            ciphertextHex = HEX.formatHex(ciphertext),
            temporaryKey = temporaryKey,
        )
    }

    fun decryptJson(
        ciphertextHex: String,
        temporaryKey: String,
    ): String {
        validateTemporaryKey(temporaryKey)
        require(ciphertextHex.length % 2 == 0) { "Ciphertext must contain complete hex bytes" }
        val ciphertext =
            runCatching { HEX.parseHex(ciphertextHex) }
                .getOrElse { throw IllegalArgumentException("Ciphertext must be hexadecimal", it) }
        return cipher(Cipher.DECRYPT_MODE, temporaryKey).doFinal(ciphertext).decodeToString()
    }

    fun encryptKeyEnvelope(json: String): String =
        HEX
            .formatHex(KugouRsa.encryptRawZeroPadded(json.encodeToByteArray(), STANDARD_AUTH_PUBLIC_KEY))
            .uppercase()

    private fun cipher(
        mode: Int,
        temporaryKey: String,
    ): Cipher {
        val key = KugouDigests.md5(temporaryKey).encodeToByteArray()
        val iv = key.copyOfRange(key.size - IV_BYTES, key.size)
        return Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        }
    }

    private fun validateTemporaryKey(value: String) {
        require(value.length == TEMPORARY_KEY_LENGTH && value.all { it in 'a'..'z' || it.isDigit() }) {
            "Temporary auth key must contain 16 lowercase ASCII letters or digits"
        }
    }

    private val HEX = HexFormat.of()
    private const val TEMPORARY_KEY_LENGTH = 16
    private const val IV_BYTES = 16

    // Standard KuGou auth public key from KuGouMusicApi@6efe84e util/crypto.js (MIT).
    private const val STANDARD_AUTH_PUBLIC_KEY =
        """-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIAG7QOELSYoIJvTFJhMpe1s/gbjDJX51HBNnEl5HXqTW6lQ7LC8jr9fWZTwusknp+sVGzwd40MwP6U5yDE27M/X1+UR4tvOGOqp94TJtQ1EPnWGWXngpeIW5GxoQGao1rmYWAu6oi1z9XkChrsUdC6DJE5E221wf/4WLFxwAtRQIDAQAB
-----END PUBLIC KEY-----"""
}
