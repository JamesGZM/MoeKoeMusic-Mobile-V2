package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouRequestContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

interface KugouHomeService {
    suspend fun fetchHomeBanners(context: KugouRequestContext): KugouApiResult<List<KugouHomeBannerDto>>

    suspend fun fetchDailyRecommendations(context: KugouRequestContext): KugouApiResult<List<KugouHomeSongDto>>

    suspend fun fetchTopPlaylists(
        context: KugouRequestContext,
        page: Int = 1,
        pageSize: Int = 6,
    ): KugouApiResult<List<KugouHomePlaylistDto>>
}

data class KugouHomeBannerDto(
    val id: String,
    val title: String?,
    val artworkUrl: String,
)

data class KugouHomeSongDto(
    val hash: String,
    val title: String,
    val artistName: String,
    val albumId: String?,
    val albumAudioId: String?,
    val durationMs: Long,
    val artworkUrl: String?,
    val privilege: Int?,
    val recommendationNote: String?,
)

data class KugouHomePlaylistDto(
    val id: String,
    val title: String,
    val artworkUrl: String?,
    val playCount: Long?,
)

internal class KugouHomeBannerDecoder {
    fun decode(body: JsonElement): KugouApiResult<List<KugouHomeBannerDto>> =
        decodeList(body, "ads") { item ->
            val artworkUrl = item.text("img_url", "image")?.takeIf(String::isNotBlank) ?: return@decodeList null
            KugouHomeBannerDto(
                id = item.text("id")?.takeIf(String::isNotBlank) ?: artworkUrl,
                title =
                    item.text("title")?.takeIf(String::isNotBlank) ?: item.objectValue("extra")?.text("title")?.takeIf(String::isNotBlank),
                artworkUrl = artworkUrl.replace("{size}", "720"),
            )
        }
}

internal class KugouDailyRecommendationDecoder {
    fun decode(body: JsonElement): KugouApiResult<List<KugouHomeSongDto>> =
        decodeList(body, "song_list") { item -> item.asHomeSongOrNull() }
}

internal class KugouTopPlaylistDecoder {
    fun decode(body: JsonElement): KugouApiResult<List<KugouHomePlaylistDto>> =
        decodeList(body, "special_list") { item ->
            val id = item.text("global_collection_id")?.takeIf(String::isNotBlank) ?: return@decodeList null
            val title = item.text("specialname")?.takeIf(String::isNotBlank) ?: return@decodeList null
            KugouHomePlaylistDto(
                id = id,
                title = title,
                artworkUrl = item.text("flexible_cover", "cover")?.takeIf(String::isNotBlank)?.replace("{size}", "480"),
                playCount = item.nonNegativeLong("play_count"),
            )
        }
}

private inline fun <T> decodeList(
    body: JsonElement,
    listName: String,
    transform: (JsonObject) -> T?,
): KugouApiResult<List<T>> {
    val root = body as? JsonObject ?: return malformed()
    val dataElement = root["data"] ?: return missingField()
    val data = dataElement as? JsonObject ?: return malformed()
    val listElement = data[listName] ?: return missingField()
    val rawItems = listElement as? JsonArray ?: return malformed()
    val items = rawItems.mapNotNull { element -> (element as? JsonObject)?.let(transform) }
    if (rawItems.isNotEmpty() && items.isEmpty()) return missingField()
    return KugouApiResult.Success(items)
}

private fun JsonObject.asHomeSongOrNull(): KugouHomeSongDto? {
    val hash = text("hash")?.takeIf(String::isNotBlank) ?: return null
    val filename = text("filename").orEmpty()
    val (filenameArtist, filenameTitle) = splitArtistTitle(filename)
    val title = text("ori_audio_name", "songname")?.takeIf(String::isNotBlank) ?: filenameTitle.takeIf(String::isNotBlank) ?: return null
    val transParam = objectValue("trans_param")
    return KugouHomeSongDto(
        hash = hash,
        title = title,
        artistName = text("author_name")?.takeIf(String::isNotBlank) ?: filenameArtist,
        albumId = text("album_id")?.takeIf(String::isNotBlank),
        albumAudioId = text("album_audio_id", "audio_id")?.takeIf(String::isNotBlank),
        durationMs = normalizedDurationMs("time_length", "timelength", "timelen"),
        artworkUrl =
            transParam
                ?.text("union_cover")
                ?.takeIf(String::isNotBlank)
                ?.replace("{size}", "240")
                ?: text("sizable_cover", "album_sizable_cover")?.takeIf(String::isNotBlank)?.replace("{size}", "240"),
        privilege = long("privilege")?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt(),
        recommendationNote = text("rec_copy_write", "recommend_reason", "publish_date")?.takeIf(String::isNotBlank),
    )
}

private fun JsonObject.text(vararg names: String): String? =
    names.firstNotNullOfOrNull { name ->
        (get(name) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
    }

private fun JsonObject.objectValue(name: String): JsonObject? = get(name) as? JsonObject

private fun JsonObject.long(name: String): Long? = (get(name) as? JsonPrimitive)?.longOrNull

private fun JsonObject.nonNegativeLong(name: String): Long? = long(name)?.takeIf { it >= 0 }

private fun JsonObject.normalizedDurationMs(vararg names: String): Long {
    val value = names.firstNotNullOfOrNull(::long)?.takeIf { it > 0 } ?: return 0
    return if (value < 10_000) value * 1_000 else value
}

private fun splitArtistTitle(filename: String): Pair<String, String> {
    val separator = filename.indexOf(" - ")
    if (separator < 0) return "" to filename.trim()
    return filename.substring(0, separator).trim() to filename.substring(separator + 3).trim()
}

private fun malformed(): KugouApiResult.Failure = KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse))

private fun missingField(): KugouApiResult.Failure =
    KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField))
