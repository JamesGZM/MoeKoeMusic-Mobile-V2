package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.InflaterInputStream

data class KugouLyricsSourceDto(
    val krcText: String,
) {
    override fun toString(): String = "KugouLyricsSourceDto(krcBytes=${krcText.encodeToByteArray().size})"
}

sealed interface KugouLyricsFetchResult {
    data class Available(
        val source: KugouLyricsSourceDto,
    ) : KugouLyricsFetchResult

    data object NotFound : KugouLyricsFetchResult
}

internal data class KugouLyricsCandidate(
    val id: String,
    val accessKey: String,
) {
    override fun toString(): String = "KugouLyricsCandidate(id=<redacted>, accessKey=<redacted>)"
}

internal sealed interface KugouLyricsCandidateDecodeResult {
    data class Found(
        val candidate: KugouLyricsCandidate,
    ) : KugouLyricsCandidateDecodeResult

    data object NotFound : KugouLyricsCandidateDecodeResult

    data class Failure(
        val error: KugouError.Protocol,
    ) : KugouLyricsCandidateDecodeResult
}

internal class KugouLyricsCandidateDecoder {
    fun decode(body: JsonElement): KugouLyricsCandidateDecodeResult =
        try {
            val candidates = body.jsonObject["candidates"]?.jsonArray ?: return missingField()
            if (candidates.isEmpty()) return KugouLyricsCandidateDecodeResult.NotFound
            val candidate = candidates.first().jsonObject
            val id = (candidate["id"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 }?.toString()
            val accessKey =
                (candidate["accesskey"] as? JsonPrimitive)
                    ?.contentOrNull
                    ?.takeIf(String::isNotBlank)
            if (id == null || accessKey == null) {
                missingField()
            } else {
                KugouLyricsCandidateDecodeResult.Found(KugouLyricsCandidate(id, accessKey))
            }
        } catch (_: Exception) {
            malformedResponse()
        }

    private fun missingField() =
        KugouLyricsCandidateDecodeResult.Failure(
            KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
        )

    private fun malformedResponse() =
        KugouLyricsCandidateDecodeResult.Failure(
            KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
        )
}

internal sealed interface KugouLyricsDownloadDecodeResult {
    data class Success(
        val source: KugouLyricsSourceDto,
    ) : KugouLyricsDownloadDecodeResult

    data class Failure(
        val error: KugouError.Protocol,
    ) : KugouLyricsDownloadDecodeResult
}

internal class KugouLyricsDownloadDecoder(
    private val krcDecoder: KugouKrcDecoder = KugouKrcDecoder(),
) {
    fun decode(body: JsonElement): KugouLyricsDownloadDecodeResult =
        try {
            val root = body.jsonObject
            val contentType = (root["contenttype"] as? JsonPrimitive)?.contentOrNull ?: return missingField()
            if (contentType != "0") return malformedResponse()
            val content =
                (root["content"] as? JsonPrimitive)
                    ?.contentOrNull
                    ?.takeIf(String::isNotBlank)
                    ?: return missingField()
            when (val decoded = krcDecoder.decode(content)) {
                is KugouKrcDecodeResult.Failure -> {
                    KugouLyricsDownloadDecodeResult.Failure(decoded.error)
                }

                is KugouKrcDecodeResult.Success -> {
                    KugouLyricsDownloadDecodeResult.Success(KugouLyricsSourceDto(decoded.text))
                }
            }
        } catch (_: Exception) {
            malformedResponse()
        }

    private fun missingField() =
        KugouLyricsDownloadDecodeResult.Failure(
            KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
        )

    private fun malformedResponse() =
        KugouLyricsDownloadDecodeResult.Failure(
            KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
        )
}

internal sealed interface KugouKrcDecodeResult {
    data class Success(
        val text: String,
    ) : KugouKrcDecodeResult

    data class Failure(
        val error: KugouError.Protocol,
    ) : KugouKrcDecodeResult
}

internal class KugouKrcDecoder(
    private val maxEncodedChars: Int = MAX_ENCODED_CHARS,
    private val maxDecodedBytes: Int = MAX_DECODED_BYTES,
) {
    init {
        require(maxEncodedChars > 0) { "Encoded KRC limit must be positive" }
        require(maxDecodedBytes > 0) { "Decoded KRC limit must be positive" }
    }

    fun decode(encodedContent: String): KugouKrcDecodeResult =
        try {
            require(encodedContent.length <= maxEncodedChars)
            val encodedBytes = Base64.getDecoder().decode(encodedContent)
            require(encodedBytes.size > KRC_HEADER.size)
            require(encodedBytes.copyOfRange(0, KRC_HEADER.size).contentEquals(KRC_HEADER))

            val compressed =
                ByteArray(encodedBytes.size - KRC_HEADER.size) { index ->
                    (encodedBytes[index + KRC_HEADER.size].toInt() xor XOR_KEY[index % XOR_KEY.size].toInt()).toByte()
                }
            val decodedBytes =
                InflaterInputStream(ByteArrayInputStream(compressed)).use { input ->
                    input.readAtMost(maxDecodedBytes)
                }
            require(decodedBytes.isNotEmpty())
            val text =
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(decodedBytes))
                    .toString()
            require(text.isNotBlank())
            KugouKrcDecodeResult.Success(text)
        } catch (_: Exception) {
            KugouKrcDecodeResult.Failure(
                KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
            )
        }

    private fun InflaterInputStream.readAtMost(limit: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(limit, DEFAULT_BUFFER_SIZE))
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit)
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private companion object {
        val KRC_HEADER = "krc1".encodeToByteArray()
        val XOR_KEY = byteArrayOf(64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45, -50, -46, 110, 105)
        const val MAX_ENCODED_CHARS = 2 * 1024 * 1024
        const val MAX_DECODED_BYTES = 8 * 1024 * 1024
    }
}
