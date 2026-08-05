package cn.james.music.kugou.api.crypto

import java.security.KeyFactory
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

internal object KugouRsa {
    fun encryptPkcs1(
        plaintext: ByteArray,
        publicKeyPem: String,
        secureRandom: SecureRandom = SecureRandom(),
    ): ByteArray {
        val encoded =
            publicKeyPem
                .lineSequence()
                .filterNot { it.startsWith("-----") }
                .joinToString(separator = "")
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(encoded)))
        return Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.ENCRYPT_MODE, publicKey, secureRandom)
            doFinal(plaintext)
        }
    }

    /**
     * KuGou login wraps the AES key with an unusual raw RSA operation: UTF-8 bytes are copied to
     * the start of a full modulus-sized block and the remaining bytes stay zero. This is not
     * PKCS#1 padding and must remain isolated from normal RSA callers.
     */
    fun encryptRawZeroPadded(
        plaintext: ByteArray,
        publicKeyPem: String,
    ): ByteArray {
        val publicKey = readPublicKey(publicKeyPem)
        val blockSize = (publicKey.modulus.bitLength() + 7) / 8
        require(plaintext.size <= blockSize) { "Data length exceeds RSA key size" }
        val padded = ByteArray(blockSize)
        plaintext.copyInto(padded)
        return Cipher.getInstance("RSA/ECB/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, publicKey)
            doFinal(padded)
        }
    }

    private fun readPublicKey(publicKeyPem: String): RSAPublicKey {
        val encoded =
            publicKeyPem
                .lineSequence()
                .filterNot { it.startsWith("-----") }
                .joinToString(separator = "")
        return KeyFactory
            .getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(encoded))) as RSAPublicKey
    }
}
