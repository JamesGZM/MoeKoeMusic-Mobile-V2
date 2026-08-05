package cn.james.music.kugou.api

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

    private companion object {
        // Public protocol key from KuGouMusicApi@6efe84e util/crypto.js (MIT).
        const val STANDARD_REGISTER_PUBLIC_KEY =
            """-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIAG7QOELSYoIJvTFJhMpe1s/gbjDJX51HBNnEl5HXqTW6lQ7LC8jr9fWZTwusknp+sVGzwd40MwP6U5yDE27M/X1+UR4tvOGOqp94TJtQ1EPnWGWXngpeIW5GxoQGao1rmYWAu6oi1z9XkChrsUdC6DJE5E221wf/4WLFxwAtRQIDAQAB
-----END PUBLIC KEY-----"""
    }
}
