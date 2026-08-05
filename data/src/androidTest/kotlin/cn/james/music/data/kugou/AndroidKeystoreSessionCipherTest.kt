package cn.james.music.data.kugou

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreSessionCipherTest {
    @Test
    fun encryptDecryptAndKeyDeletionUseAndroidKeystore() {
        val cipher = AndroidKeystoreSessionCipher("cn.james.music.test.${UUID.randomUUID()}")
        val plaintext = "fixture-session-secret".encodeToByteArray()
        try {
            val payload = cipher.encrypt(plaintext)

            assertFalse(payload.ciphertext.contentEquals(plaintext))
            assertArrayEquals(plaintext, cipher.decrypt(payload))

            cipher.clearKey()
            assertThrows(KugouSessionCipherException::class.java) { cipher.decrypt(payload) }
        } finally {
            cipher.clearKey()
            plaintext.fill(0)
        }
    }
}
