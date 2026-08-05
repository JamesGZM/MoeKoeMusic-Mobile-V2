package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class KugouSongDto(
    val id: String,
    val hash: String,
    val title: String,
    val artistName: String,
    val albumId: String?,
    val albumTitle: String?,
    val durationSeconds: Long,
    val artworkUrl: String?,
)

data class KugouSongSearchPageDto(
    val items: List<KugouSongDto>,
    val totalCount: Int,
)

sealed interface KugouSongSearchDecodeResult {
    data class Success(
        val page: KugouSongSearchPageDto,
    ) : KugouSongSearchDecodeResult

    data class Failure(
        val error: KugouError.Protocol,
    ) : KugouSongSearchDecodeResult
}

class KugouSongSearchDecoder {
    fun decode(body: JsonElement): KugouSongSearchDecodeResult =
        try {
            val data = body.jsonObject["data"]?.jsonObject ?: return missingField()
            val rawItems = data["lists"]?.jsonArray ?: return missingField()
            val items = rawItems.mapNotNull { element -> element.asSongOrNull() }
            if (rawItems.isNotEmpty() && items.isEmpty()) return missingField()
            val total = data["total"].longValue()?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: items.size
            KugouSongSearchDecodeResult.Success(KugouSongSearchPageDto(items = items, totalCount = total))
        } catch (_: Exception) {
            KugouSongSearchDecodeResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse))
        }

    private fun JsonElement.asSongOrNull(): KugouSongDto? =
        runCatching {
            val item = jsonObject
            val hash = item.stringValue("FileHash")?.takeIf(String::isNotBlank) ?: return null
            val title = item.stringValue("SongName", "OriSongName")?.takeIf(String::isNotBlank) ?: return null
            val id = item.stringValue("MixSongID", "ID", "Audioid")?.takeIf(String::isNotBlank) ?: hash
            KugouSongDto(
                id = id,
                hash = hash,
                title = title,
                artistName = item.stringValue("SingerName").orEmpty(),
                albumId = item.stringValue("AlbumID")?.takeIf(String::isNotBlank),
                albumTitle = item.stringValue("AlbumName")?.takeIf(String::isNotBlank),
                durationSeconds = item["Duration"].longValue()?.coerceAtLeast(0) ?: 0,
                artworkUrl = item.stringValue("Image", "AlbumImage")?.takeIf(String::isNotBlank),
            )
        }.getOrNull()

    private fun JsonObject.stringValue(vararg names: String): String? =
        names.firstNotNullOfOrNull { name ->
            (this[name] as? JsonPrimitive)?.contentOrNull
        }

    private fun JsonElement?.longValue(): Long? = (this as? JsonPrimitive)?.longOrNull

    private fun missingField() =
        KugouSongSearchDecodeResult.Failure(
            KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
        )
}
