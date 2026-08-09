package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.config.KugouPlatformConfig
import cn.james.music.kugou.api.transport.KugouCleartextPolicy
import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouRequestSpec
import cn.james.music.kugou.api.transport.KugouResponseFormat
import cn.james.music.kugou.api.transport.KugouRetryMode
import cn.james.music.kugou.api.transport.KugouSignatureMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object KugouEndpoints {
    fun sendMobileCode(mobile: String): KugouRequestSpec {
        require(MOBILE_PATTERN.matches(mobile)) { "Mobile number must contain 11 digits and start with 1" }
        val body =
            buildJsonObject {
                put("businessid", 5)
                put("mobile", mobile)
                put("plat", 3)
            }
        return KugouRequestSpec(
            id = "captcha_sent",
            method = KugouHttpMethod.Post,
            baseUrl = "http://login.user.kugou.com",
            path = "/v7/send_mobile_code",
            headers = mapOf("Content-Type" to "application/json"),
            body = Json.encodeToString(body).encodeToByteArray(),
            cleartextPolicy = KugouCleartextPolicy.LoginMobileCode,
        )
    }

    fun registerDevice(
        encryptedBody: ByteArray,
        encryptedIdentity: String,
    ): KugouRequestSpec {
        require(encryptedBody.isNotEmpty()) { "Encrypted device body must not be empty" }
        require(encryptedIdentity.isNotBlank()) { "Encrypted device identity must not be blank" }
        return KugouRequestSpec(
            id = "register_dev",
            method = KugouHttpMethod.Post,
            baseUrl = "https://userservice.kugou.com",
            path = "/risk/v2/r_register_dev",
            params = mapOf("part" to "1", "platid" to "1", "p" to encryptedIdentity),
            body = encryptedBody.copyOf(),
            responseFormat = KugouResponseFormat.Bytes,
        )
    }

    fun searchSongs(
        keyword: String,
        page: Int,
        pageSize: Int = 30,
    ): KugouRequestSpec {
        val normalizedKeyword = keyword.trim()
        require(normalizedKeyword.isNotEmpty()) { "Search keyword must not be blank" }
        require(page > 0) { "Page must be positive" }
        require(pageSize in 1..100) { "Page size must be between 1 and 100" }
        return KugouRequestSpec(
            id = "search.song",
            method = KugouHttpMethod.Get,
            path = "/v3/search/song",
            params =
                linkedMapOf(
                    "albumhide" to "0",
                    "iscorrection" to "1",
                    "keyword" to normalizedKeyword,
                    "nocollect" to "0",
                    "page" to page.toString(),
                    "pagesize" to pageSize.toString(),
                    "platform" to "AndroidFilter",
                ),
            headers = mapOf("x-router" to "complexsearch.kugou.com"),
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    fun searchSongsAnonymous(
        keyword: String,
        page: Int,
        pageSize: Int = 30,
    ): KugouRequestSpec {
        val normalizedKeyword = keyword.trim()
        require(normalizedKeyword.isNotEmpty()) { "Search keyword must not be blank" }
        require(page > 0) { "Page must be positive" }
        require(pageSize in 1..100) { "Page size must be between 1 and 100" }
        return KugouRequestSpec(
            id = "search.song.anonymous",
            method = KugouHttpMethod.Get,
            baseUrl = "https://songsearch.kugou.com",
            path = "/song_search_v2",
            params =
                linkedMapOf(
                    "keyword" to normalizedKeyword,
                    "page" to page.toString(),
                    "pagesize" to pageSize.toString(),
                    "platform" to "WebFilter",
                ),
            signatureMode = cn.james.music.kugou.api.transport.KugouSignatureMode.None,
            includeDefaultParams = false,
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    fun searchLyrics(songHash: String): KugouRequestSpec {
        val normalizedHash = songHash.trim()
        require(normalizedHash.isNotEmpty()) { "Song hash must not be blank" }
        return KugouRequestSpec(
            id = "lyrics.search",
            method = KugouHttpMethod.Get,
            baseUrl = LYRICS_BASE_URL,
            path = "/search",
            params =
                linkedMapOf(
                    "ver" to "1",
                    "man" to "yes",
                    "client" to "pc",
                    "hash" to normalizedHash,
                ),
            signatureMode = KugouSignatureMode.None,
            includeDefaultParams = false,
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    internal fun downloadLyrics(
        candidateId: String,
        accessKey: String,
    ): KugouRequestSpec {
        require(candidateId.isNotBlank()) { "Lyrics candidate id must not be blank" }
        require(accessKey.isNotBlank()) { "Lyrics access key must not be blank" }
        return KugouRequestSpec(
            id = "lyrics.download",
            method = KugouHttpMethod.Get,
            baseUrl = LYRICS_BASE_URL,
            path = "/download",
            params =
                linkedMapOf(
                    "ver" to "1",
                    "client" to "android",
                    "id" to candidateId,
                    "accesskey" to accessKey,
                    "fmt" to "krc",
                    "charset" to "utf8",
                ),
            signatureMode = KugouSignatureMode.None,
            includeDefaultParams = false,
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    fun privilegeLite(resources: List<KugouAudioResource>): KugouRequestSpec {
        require(resources.isNotEmpty()) { "At least one resource is required" }
        val body =
            buildJsonObject {
                put("appid", KugouPlatformConfig.Standard.appId.toInt())
                put("area_code", 1)
                put("behavior", "play")
                put("clientver", KugouPlatformConfig.Standard.clientVersion.toInt())
                put("need_hash_offset", 1)
                put("relate", 1)
                put("support_verify", 1)
                put(
                    "resource",
                    buildJsonArray {
                        resources.forEach { resource ->
                            add(
                                buildJsonObject {
                                    put("type", "audio")
                                    put("page_id", 0)
                                    put("hash", resource.hash)
                                    put("album_id", resource.albumId)
                                },
                            )
                        }
                    },
                )
                put(
                    "qualities",
                    buildJsonArray {
                        QUALITIES.forEach { quality -> add(JsonPrimitive(quality)) }
                    },
                )
            }
        return KugouRequestSpec(
            id = "privilege_lite",
            method = KugouHttpMethod.Post,
            path = "/v2/get_res_privilege/lite",
            headers = mapOf("x-router" to "media.store.kugou.com", "Content-Type" to "application/json"),
            body = Json.encodeToString(body).encodeToByteArray(),
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    fun songUrl(
        hash: String,
        albumId: Long = 0,
        albumAudioId: Long = 0,
        quality: String = "128",
        freePart: Boolean = true,
    ): KugouRequestSpec =
        songUrlWithWireQuality(
            hash = hash,
            albumId = albumId,
            albumAudioId = albumAudioId,
            quality = MAGIC_QUALITIES[quality] ?: quality,
            freePart = freePart,
        )

    fun songUrl(
        hash: String,
        quality: KugouPlaybackQuality,
        albumId: Long = 0,
        albumAudioId: Long = 0,
        freePart: Boolean = true,
    ): KugouRequestSpec =
        songUrlWithWireQuality(
            hash = hash,
            albumId = albumId,
            albumAudioId = albumAudioId,
            quality = quality.wireValue,
            freePart = freePart,
        )

    private fun songUrlWithWireQuality(
        hash: String,
        albumId: Long,
        albumAudioId: Long,
        quality: String,
        freePart: Boolean,
    ): KugouRequestSpec {
        require(hash.isNotBlank()) { "Song hash must not be blank" }
        return KugouRequestSpec(
            id = "song_url",
            method = KugouHttpMethod.Get,
            path = "/v5/url",
            params =
                linkedMapOf(
                    "album_id" to albumId.toString(),
                    "area_code" to "1",
                    "hash" to hash.lowercase(),
                    "ssa_flag" to "is_fromtrack",
                    "version" to "11430",
                    "page_id" to "151369488",
                    "quality" to quality,
                    "album_audio_id" to albumAudioId.toString(),
                    "behavior" to "play",
                    "pid" to "2",
                    "cmd" to "26",
                    "pidversion" to "3001",
                    "IsFreePart" to if (freePart) "1" else "0",
                    "ppage_id" to "463467626,350369493,788954147",
                    "cdnBackup" to "1",
                    "module" to "",
                    "clientver" to "11430",
                ),
            headers = mapOf("x-router" to "trackercdn.kugou.com"),
            generateSignKey = true,
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    private val QUALITIES = listOf("128", "320", "flac", "high", "viper_atmos", "viper_tape", "viper_clear", "super", "multitrack")
    private val MAGIC_QUALITIES = setOf("piano", "acappella", "subwoofer", "ancient", "dj", "surnay").associateWith { "magic_$it" }
    private val MOBILE_PATTERN = Regex("^1\\d{10}$")
    private const val LYRICS_BASE_URL = "https://lyrics.kugou.com"
}

data class KugouAudioResource(
    val hash: String,
    val albumId: Long = 0,
) {
    init {
        require(hash.isNotBlank()) { "Audio hash must not be blank" }
    }
}
