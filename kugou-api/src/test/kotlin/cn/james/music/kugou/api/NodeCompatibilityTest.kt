package cn.james.music.kugou.api

import cn.james.music.kugou.api.crypto.KugouAuthCrypto
import cn.james.music.kugou.api.crypto.KugouDigests
import cn.james.music.kugou.api.crypto.KugouMid
import cn.james.music.kugou.api.crypto.KugouPlaylistCrypto
import cn.james.music.kugou.api.crypto.KugouRsa
import cn.james.music.kugou.api.signing.KugouRequestSigner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NodeCompatibilityTest {
    private val signer = KugouRequestSigner()
    private val params =
        mapOf(
            "appid" to "1005",
            "clienttime" to "1700000000",
            "clientver" to "20489",
            "dfid" to "fixture-dfid",
            "keyword" to "MoeKoe 测试",
            "mid" to "fixture-mid",
            "uuid" to "-",
        )

    @Test
    fun digestAndMidMatchFixedNodeOutput() {
        assertEquals("89eed92439479380cca39793951eb32e", KugouDigests.md5("MoeKoe 测试"))
        assertEquals("69d4c2a3509c3834e429c9938e7f8890c6d2f28b", KugouDigests.sha1("MoeKoe 测试"))
        assertEquals(
            "149047798212028265178410220297437707128",
            KugouMid.fromGuid("12345678-1234-4abc-8def-1234567890ab"),
        )
    }

    @Test
    fun signaturesMatchFixedNodeOutput() {
        assertEquals("b3de7fd94687365ad6559a48b7c087ff", signer.androidSignature(params))
        assertEquals("9c4be42f037465be9c780532ae6687c3", signer.registerSignature(params))
        assertEquals("f43c3d8a3944ebb584820feee6d32ff4", signer.webSignature(params))
        assertEquals("bcd8cf9e54fbd406da3260d9cc9abee0", signer.signKey("abcdef012345", "fixture-mid", appId = "1005"))
        assertEquals("e3e86865c059fdec1dd020b6d640b8a5", signer.signParamsKey("1700000000123", appId = "1005", clientVersion = "20489"))
    }

    @Test
    fun androidBodySignatureUsesExactUtf8Bytes() {
        val body = """{"resource":[{"type":"audio","hash":"ABCDEF012345"}]}""".encodeToByteArray()
        assertEquals("8d258ca344b65e39f2180a1520ca8687", signer.androidSignature(params, body))
    }

    @Test
    fun playlistAesMatchesFixedNodeOutputAndRoundTrips() {
        val plaintext = """{"name":"MoeKoe 测试","page":1}"""
        val encrypted = KugouPlaylistCrypto.encryptBase64(plaintext, "abc123")

        assertEquals(
            "nOr07SKhq4d46W9ybWgCl5FBHHuczIGujaPL87fjIeih/SPQcuh6WtUjJ/++71ZM",
            encrypted,
        )
        assertEquals(plaintext, KugouPlaylistCrypto.decryptBase64(encrypted, "abc123"))
    }

    @Test
    fun registerRsaUsesPkcs1AndProducesAFullBlock() {
        val first = KugouRsa.encryptPkcs1("fixture payload".encodeToByteArray(), STANDARD_REGISTER_PUBLIC_KEY)
        val second = KugouRsa.encryptPkcs1("fixture payload".encodeToByteArray(), STANDARD_REGISTER_PUBLIC_KEY)

        assertEquals(128, first.size)
        assertEquals(128, second.size)
        assertFalse(first.contentEquals(second))
        assertNotEquals(first.joinToString(), second.joinToString())
    }

    @Test
    fun authAesAndRawRsaMatchFixedNodeOutput() {
        val plaintext = """{"mobile":"13800000000","code":"246810"}"""
        val encrypted = KugouAuthCrypto.encryptJson(plaintext, "fixturekey123456")

        assertEquals(
            "b8be3bbf25d910e805a8fa2cc4f784339f3673b7fe4914d55c885fa72a3dde4685f1d891d14581826d0e576b8c9926b2",
            encrypted.ciphertextHex,
        )
        assertEquals(plaintext, KugouAuthCrypto.decryptJson(encrypted.ciphertextHex, encrypted.temporaryKey))
        assertEquals(
            "2371591A7A8D41AA5890429388F973328BFFD3EDDD332F0B32F8DF5EF2FD28DC1E062E5775D66E81B14B0AFDE62CB271F73991F2F7EA4C404FF3C23974C820061220246AA50C8CD47F714F59AED731E4E29D0F34EFF3C46002E0368DD326BD7CBC7B4586ABBF9C8EB61D4E55837E3404243A24D51B705811AA1CA5745394515A",
            KugouAuthCrypto.encryptKeyEnvelope("""{"clienttime_ms":1700000000123,"key":"fixturekey123456"}"""),
        )
    }

    private companion object {
        // Public protocol key from KuGouMusicApi@6efe84e util/crypto.js (MIT).
        const val STANDARD_REGISTER_PUBLIC_KEY =
            """-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIAG7QOELSYoIJvTFJhMpe1s/gbjDJX51HBNnEl5HXqTW6lQ7LC8jr9fWZTwusknp+sVGzwd40MwP6U5yDE27M/X1+UR4tvOGOqp94TJtQ1EPnWGWXngpeIW5GxoQGao1rmYWAu6oi1z9XkChrsUdC6DJE5E221wf/4WLFxwAtRQIDAQAB
-----END PUBLIC KEY-----"""
    }
}
