package cn.james.music.data.home

import cn.james.music.core.model.home.HomeBanner
import cn.james.music.core.model.home.HomeContent
import cn.james.music.core.model.home.HomePlaylist
import cn.james.music.core.model.home.HomeRecommendation
import cn.james.music.core.model.online.Song
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal class HomeContentCacheCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(content: HomeContent): String =
        buildJsonObject {
            put("version", CACHE_SCHEMA_VERSION)
            put("banners", buildJsonArray { content.banners.forEach { add(it.toJson()) } })
            put("recommendations", buildJsonArray { content.recommendations.forEach { add(it.toJson()) } })
            put("playlists", buildJsonArray { content.playlists.forEach { add(it.toJson()) } })
        }.toString()

    fun decode(
        payload: String,
        updatedAtEpochMs: Long,
    ): HomeContent? =
        runCatching {
            check(updatedAtEpochMs >= 0)
            val root = json.parseToJsonElement(payload).objectValue()
            check(root.int("version") == CACHE_SCHEMA_VERSION)
            HomeContent(
                banners = root.array("banners").map { it.objectValue().toBanner() },
                recommendations = root.array("recommendations").map { it.objectValue().toRecommendation() },
                playlists = root.array("playlists").map { it.objectValue().toPlaylist() },
                updatedAtEpochMs = updatedAtEpochMs,
            )
        }.getOrNull()

    private fun HomeBanner.toJson(): JsonObject =
        buildJsonObject {
            put("id", id)
            put("title", title)
            put("artworkUrl", artworkUrl)
        }

    private fun HomeRecommendation.toJson(): JsonObject =
        buildJsonObject {
            put("note", note)
            put(
                "song",
                buildJsonObject {
                    put("id", song.id)
                    put("hash", song.hash)
                    put("title", song.title)
                    put("artistName", song.artistName)
                    put("albumId", song.albumId)
                    put("albumTitle", song.albumTitle)
                    put("durationMs", song.durationMs)
                    put("artworkUrl", song.artworkUrl)
                },
            )
        }

    private fun HomePlaylist.toJson(): JsonObject =
        buildJsonObject {
            put("id", id)
            put("title", title)
            put("artworkUrl", artworkUrl)
            playCount?.let { put("playCount", it) }
        }

    private fun JsonObject.toBanner(): HomeBanner =
        HomeBanner(
            id = requiredText("id"),
            title = optionalText("title"),
            artworkUrl = requiredText("artworkUrl"),
        )

    private fun JsonObject.toRecommendation(): HomeRecommendation {
        val song = getValue("song").objectValue()
        return HomeRecommendation(
            song =
                Song(
                    id = song.requiredText("id"),
                    hash = song.requiredText("hash"),
                    title = song.requiredText("title"),
                    artistName = song.requiredText("artistName"),
                    albumId = song.optionalText("albumId"),
                    albumTitle = song.optionalText("albumTitle"),
                    durationMs = song.nonNegativeLong("durationMs"),
                    artworkUrl = song.optionalText("artworkUrl"),
                ),
            note = optionalText("note"),
        )
    }

    private fun JsonObject.toPlaylist(): HomePlaylist =
        HomePlaylist(
            id = requiredText("id"),
            title = requiredText("title"),
            artworkUrl = optionalText("artworkUrl"),
            playCount = optionalNonNegativeLong("playCount"),
        )

    private fun JsonElement.objectValue(): JsonObject = this as? JsonObject ?: error("Expected object")

    private fun JsonObject.array(name: String): JsonArray = getValue(name) as? JsonArray ?: error("Expected array")

    private fun JsonObject.int(name: String): Int = (getValue(name) as? JsonPrimitive)?.intOrNull ?: error("Expected int")

    private fun JsonObject.requiredText(name: String): String = optionalText(name)?.takeIf(String::isNotBlank) ?: error("Expected text")

    private fun JsonObject.optionalText(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

    private fun JsonObject.nonNegativeLong(name: String): Long =
        (getValue(name) as? JsonPrimitive)?.longOrNull?.takeIf { it >= 0 } ?: error("Expected non-negative long")

    private fun JsonObject.optionalNonNegativeLong(name: String): Long? =
        get(name)?.let { element -> (element as? JsonPrimitive)?.longOrNull?.takeIf { it >= 0 } ?: error("Expected non-negative long") }

    companion object {
        const val CACHE_SCHEMA_VERSION = 1
    }
}
