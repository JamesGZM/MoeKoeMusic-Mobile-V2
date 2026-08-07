package cn.james.music

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthCryptoCompatibilityTest {
    @Test
    fun authCryptoLoadsAndRunsOnDeviceApiLevel() {
        val cryptoClass = Class.forName("cn.james.music.kugou.api.crypto.KugouAuthCrypto")
        val crypto = cryptoClass.getField("INSTANCE").get(null)
        val encrypted =
            cryptoClass
                .getMethod("encryptJson", String::class.java, String::class.java)
                .invoke(crypto, FIXTURE_JSON, FIXTURE_KEY)
        val ciphertext =
            encrypted.javaClass
                .getMethod("getCiphertextHex")
                .invoke(encrypted) as String
        val decrypted =
            cryptoClass
                .getMethod("decryptJson", String::class.java, String::class.java)
                .invoke(crypto, ciphertext, FIXTURE_KEY)

        assertEquals(FIXTURE_JSON, decrypted)
    }

    private companion object {
        const val FIXTURE_JSON = "{\"mobile\":\"fixture\"}"
        const val FIXTURE_KEY = "0123456789abcdef"
    }
}
