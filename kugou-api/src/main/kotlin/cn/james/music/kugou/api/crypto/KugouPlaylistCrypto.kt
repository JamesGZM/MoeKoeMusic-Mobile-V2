package cn.james.music.kugou.api.crypto

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object KugouPlaylistCrypto {
    fun encryptBase64(
        plaintext: String,
        keySeed: String,
    ): String = Base64.getEncoder().encodeToString(cipher(Cipher.ENCRYPT_MODE, keySeed).doFinal(plaintext.encodeToByteArray()))

    fun decryptBase64(
        ciphertext: String,
        keySeed: String,
    ): String = cipher(Cipher.DECRYPT_MODE, keySeed).doFinal(Base64.getDecoder().decode(ciphertext)).decodeToString()

    private fun cipher(
        mode: Int,
        keySeed: String,
    ): Cipher {
        val digest = KugouDigests.md5(keySeed)
        val key = digest.substring(0, 16).encodeToByteArray()
        val iv = digest.substring(16, 32).encodeToByteArray()
        return Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        }
    }
}
