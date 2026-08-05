package cn.james.music.kugou.api.crypto

import java.security.KeyFactory
import java.security.SecureRandom
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
}
