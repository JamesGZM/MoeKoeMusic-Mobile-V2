package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.KugouKrcDecodeResult
import cn.james.music.kugou.api.endpoint.KugouKrcDecoder
import cn.james.music.kugou.api.endpoint.KugouLyricsCandidateDecodeResult
import cn.james.music.kugou.api.endpoint.KugouLyricsCandidateDecoder
import cn.james.music.kugou.api.endpoint.KugouLyricsDownloadDecodeResult
import cn.james.music.kugou.api.endpoint.KugouLyricsDownloadDecoder
import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.DeflaterOutputStream

class KugouLyricsProtocolTest {
    @Test
    fun candidateDecoderAcceptsNumericIdFromFirstCandidate() {
        val result =
            KugouLyricsCandidateDecoder().decode(
                json(
                    """
                    {"candidates":[
                      {"id":42,"accesskey":"fixture-access-key","duration":120000},
                      {"id":43,"accesskey":"unused"}
                    ]}
                    """.trimIndent(),
                ),
            )

        assertTrue(result is KugouLyricsCandidateDecodeResult.Found)
        val candidate = (result as KugouLyricsCandidateDecodeResult.Found).candidate
        assertEquals("42", candidate.id)
        assertEquals("fixture-access-key", candidate.accessKey)
    }

    @Test
    fun emptyCandidatesAreNotFoundButUnusableCandidatesAreProtocolFailures() {
        val empty = KugouLyricsCandidateDecoder().decode(json("""{"candidates":[]}"""))
        val unusable =
            KugouLyricsCandidateDecoder().decode(
                json("""{"candidates":[{"id":42},{"id":43,"accesskey":"must-not-mask-drift"}]}"""),
            )

        assertEquals(KugouLyricsCandidateDecodeResult.NotFound, empty)
        assertProtocolFailure(unusable, KugouError.Protocol.Reason.MissingRequiredField)
    }

    @Test
    fun downloadDecoderAcceptsNumericAndStringKrcContentTypes() {
        val numeric =
            KugouLyricsDownloadDecoder().decode(
                json("""{"contenttype":0,"content":"$NODE_KRC_VECTOR"}"""),
            )
        val string =
            KugouLyricsDownloadDecoder().decode(
                json("""{"contenttype":"0","content":"$NODE_KRC_VECTOR"}"""),
            )

        assertEquals(FIXTURE_KRC_TEXT, (numeric as KugouLyricsDownloadDecodeResult.Success).source.krcText)
        assertEquals(FIXTURE_KRC_TEXT, (string as KugouLyricsDownloadDecodeResult.Success).source.krcText)
    }

    @Test
    fun unknownContentTypeAndMissingContentAreProtocolFailures() {
        val unknown = KugouLyricsDownloadDecoder().decode(json("""{"contenttype":1,"content":"ignored"}"""))
        val missing = KugouLyricsDownloadDecoder().decode(json("""{"contenttype":0}"""))

        assertProtocolFailure(unknown, KugouError.Protocol.Reason.MalformedResponse)
        assertProtocolFailure(missing, KugouError.Protocol.Reason.MissingRequiredField)
    }

    @Test
    fun krcDecoderMatchesFixedNodeZlibXorBase64Vector() {
        val result = KugouKrcDecoder().decode(NODE_KRC_VECTOR)

        assertEquals(FIXTURE_KRC_TEXT, (result as KugouKrcDecodeResult.Success).text)
    }

    @Test
    fun krcDecoderRejectsInvalidBase64HeaderZlibUtf8AndSizeLimits() {
        val invalidBase64 = KugouKrcDecoder().decode("not base64")
        val invalidHeader = KugouKrcDecoder().decode(Base64.getEncoder().encodeToString("nope".encodeToByteArray()))
        val invalidZlib =
            KugouKrcDecoder().decode(
                Base64.getEncoder().encodeToString("krc1corrupt-zlib".encodeToByteArray()),
            )
        val invalidUtf8 = KugouKrcDecoder().decode(encodeKrc(byteArrayOf(0xC3.toByte(), 0x28)))
        val encodedLimit = KugouKrcDecoder(maxEncodedChars = 8).decode(NODE_KRC_VECTOR)
        val decodedLimit = KugouKrcDecoder(maxDecodedBytes = 4).decode(NODE_KRC_VECTOR)

        listOf(invalidBase64, invalidHeader, invalidZlib, invalidUtf8, encodedLimit, decodedLimit).forEach { result ->
            assertTrue(result is KugouKrcDecodeResult.Failure)
        }
    }

    private fun assertProtocolFailure(
        result: Any,
        reason: KugouError.Protocol.Reason,
    ) {
        val error =
            when (result) {
                is KugouLyricsCandidateDecodeResult.Failure -> result.error
                is KugouLyricsDownloadDecodeResult.Failure -> result.error
                else -> error("Expected protocol failure, got $result")
            }
        assertEquals(reason, error.reason)
    }

    private fun encodeKrc(plaintext: ByteArray): String {
        val compressed =
            ByteArrayOutputStream().use { output ->
                DeflaterOutputStream(output).use { it.write(plaintext) }
                output.toByteArray()
            }
        compressed.indices.forEach { index ->
            compressed[index] = (compressed[index].toInt() xor XOR_KEY[index % XOR_KEY.size].toInt()).toByte()
        }
        return Base64.getEncoder().encodeToString("krc1".encodeToByteArray() + compressed)
    }

    private fun json(value: String) = Json.parseToJsonElement(value)

    private companion object {
        const val FIXTURE_KRC_TEXT = "[0,1200]<0,600,0>Moe<600,600,0>Koe\\n[language:W10=]"

        // Node.js zlib.deflateSync output using the fixed KuGouMusicApi@6efe84e XOR key.
        const val NODE_KRC_VECTOR = "a3JjMTjb6kGOA0B1Yb6EHB7jXVmQdtGEk33BRuAWDcIyBvbVqNuly6rgsLMFnUFuzQk2aQpfb3Q="
        val XOR_KEY = byteArrayOf(64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45, -50, -46, 110, 105)
    }
}
