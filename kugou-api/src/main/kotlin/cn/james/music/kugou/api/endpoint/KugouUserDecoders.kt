package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

data class KugouUserDetailDto(
    val userId: String?,
    val nickname: String?,
    val avatarUrl: String?,
    val backgroundUrl: String?,
    val signature: String,
    val fans: Long,
    val follows: Long,
    val listenMinutes: Long,
)

data class KugouVipSummaryDto(
    val isActive: Boolean,
    val label: String?,
)

internal class KugouUserDetailDecoder {
    fun decode(body: JsonElement): KugouApiResult<KugouUserDetailDto> {
        val root = body as? JsonObject ?: return malformed()
        val dataElement = root["data"] ?: return missingField()
        val data = dataElement as? JsonObject ?: return malformed()
        return KugouApiResult.Success(
            KugouUserDetailDto(
                userId = data.text("userid")?.takeIf { it.isNotBlank() && it != "0" },
                nickname = data.text("nickname")?.takeIf(String::isNotBlank),
                avatarUrl = data.text("pic")?.takeIf(String::isNotBlank),
                backgroundUrl = data.text("bg_pic")?.takeIf(String::isNotBlank),
                signature = data.text("descri").orEmpty(),
                fans = data.nonNegativeLong("fans"),
                follows = data.nonNegativeLong("follows"),
                listenMinutes = data.nonNegativeLong("duration"),
            ),
        )
    }
}

internal class KugouVipSummaryDecoder {
    fun decode(body: JsonElement): KugouApiResult<KugouVipSummaryDto> {
        val root = body as? JsonObject ?: return malformed()
        val dataElement = root["data"] ?: return missingField()
        val data = dataElement as? JsonObject ?: return malformed()
        val vipElement = data["busi_vip"] ?: return missingField()
        val vipRecords = vipElement as? JsonArray ?: return malformed()
        if (vipRecords.any { it !is JsonObject }) return malformed()
        val active =
            vipRecords
                .filterIsInstance<JsonObject>()
                .firstOrNull { it.long("is_vip") == 1L }
        val label =
            active?.let { record ->
                if (record.text("product_type").equals("svip", ignoreCase = true)) "SVIP" else "VIP"
            }
        return KugouApiResult.Success(
            KugouVipSummaryDto(
                isActive = active != null,
                label = label,
            ),
        )
    }
}

private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject.long(name: String): Long? = (get(name) as? JsonPrimitive)?.longOrNull

private fun JsonObject.nonNegativeLong(name: String): Long = long(name)?.coerceAtLeast(0) ?: 0

private fun malformed() =
    KugouApiResult.Failure(
        KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
    )

private fun missingField() =
    KugouApiResult.Failure(
        KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
    )
