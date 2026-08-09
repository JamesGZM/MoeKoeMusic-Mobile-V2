package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

enum class KugouPlaybackQuality(
    val wireValue: String,
) {
    Standard("128"),
    High("320"),
    Lossless("flac"),
    HiRes("high"),
    ViperAtmos("viper_atmos"),
    ViperClear("viper_clear"),
    ViperTape("viper_tape"),
    ;

    companion object {
        fun fromWireValue(value: String): KugouPlaybackQuality? = entries.firstOrNull { quality -> quality.wireValue == value }
    }
}

data class KugouPrivilegeCandidate(
    val hash: String,
    val quality: KugouPlaybackQuality,
)

sealed interface KugouPrivilegeDecodeResult {
    data class Success(
        val candidates: List<KugouPrivilegeCandidate>,
    ) : KugouPrivilegeDecodeResult

    data class Failure(
        val error: KugouError.Protocol,
    ) : KugouPrivilegeDecodeResult
}

class KugouPrivilegeDecoder {
    fun decode(body: JsonElement): KugouPrivilegeDecodeResult =
        try {
            val resources = body.jsonObject["data"]?.jsonArray ?: return missingField()
            val candidates =
                resources
                    .flatMap { resource -> resource.variants() }
                    .mapNotNull { variant -> variant.toCandidateOrNull() }
                    .distinctBy(KugouPrivilegeCandidate::quality)
                    .sortedByDescending { candidate -> candidate.quality.ordinal }
            KugouPrivilegeDecodeResult.Success(candidates)
        } catch (_: Exception) {
            KugouPrivilegeDecodeResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse))
        }

    private fun JsonElement.variants(): List<JsonElement> {
        val item = jsonObject
        return listOf(this) + (item["relate_goods"] as? JsonArray).orEmpty()
    }

    private fun JsonElement.toCandidateOrNull(): KugouPrivilegeCandidate? =
        runCatching {
            val item = jsonObject
            val hash = item.stringValue("hash")?.takeIf(String::isNotBlank) ?: return null
            val quality =
                item
                    .stringValue("quality")
                    ?.let(KugouPlaybackQuality::fromWireValue)
                    ?: return null
            if (item["level"].intValue() == 0) return null
            KugouPrivilegeCandidate(hash = hash, quality = quality)
        }.getOrNull()

    private fun missingField() =
        KugouPrivilegeDecodeResult.Failure(
            KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
        )
}

data class KugouPlaybackAddress(
    val urls: List<String>,
    val durationMs: Long,
    val extension: String?,
)

enum class KugouPlaybackUnavailableReason {
    NoCopyright,
    VipRequired,
}

sealed interface KugouPlaybackAddressDecodeResult {
    data class Success(
        val address: KugouPlaybackAddress,
    ) : KugouPlaybackAddressDecodeResult

    data class Unavailable(
        val reason: KugouPlaybackUnavailableReason,
    ) : KugouPlaybackAddressDecodeResult

    data class Failure(
        val error: KugouError.Protocol,
    ) : KugouPlaybackAddressDecodeResult
}

class KugouPlaybackAddressDecoder {
    fun decode(body: JsonElement): KugouPlaybackAddressDecodeResult =
        try {
            val root = body.jsonObject
            val urls =
                listOf("url", "backupUrl", "backup_url")
                    .flatMap { name -> root[name].urlValues() }
                    .map(::upgradeToHttps)
                    .filter(::isSecureHttpUrl)
                    .distinct()
            if (urls.isEmpty()) {
                return KugouPlaybackAddressDecodeResult.Unavailable(
                    if (root["status"].intValue() == NO_COPYRIGHT_STATUS) {
                        KugouPlaybackUnavailableReason.NoCopyright
                    } else {
                        KugouPlaybackUnavailableReason.VipRequired
                    },
                )
            }
            val durationMs = root["timeLength"].longValue()?.coerceAtLeast(0) ?: 0
            KugouPlaybackAddressDecodeResult.Success(
                KugouPlaybackAddress(
                    urls = urls,
                    durationMs = durationMs,
                    extension = root.stringValue("extName")?.takeIf(String::isNotBlank),
                ),
            )
        } catch (_: Exception) {
            KugouPlaybackAddressDecodeResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse))
        }

    private fun JsonElement?.urlValues(): List<String> =
        when (this) {
            is JsonPrimitive -> {
                contentOrNull
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let(::listOf)
                    .orEmpty()
            }

            is JsonArray -> {
                mapNotNull { element -> (element as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
            }

            else -> {
                emptyList()
            }
        }

    private fun isSecureHttpUrl(value: String): Boolean = value.startsWith("https://", ignoreCase = true)

    private fun upgradeToHttps(value: String): String =
        if (value.startsWith("http://", ignoreCase = true)) {
            "https://${value.substringAfter("://")}"
        } else {
            value
        }

    private companion object {
        const val NO_COPYRIGHT_STATUS = 3
    }
}

private fun JsonObject.stringValue(vararg names: String): String? =
    names.firstNotNullOfOrNull { name -> (this[name] as? JsonPrimitive)?.contentOrNull }

private fun JsonElement?.intValue(): Int? = (this as? JsonPrimitive)?.intOrNull

private fun JsonElement?.longValue(): Long? = (this as? JsonPrimitive)?.longOrNull
