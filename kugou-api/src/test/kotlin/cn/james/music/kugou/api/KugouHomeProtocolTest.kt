package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.EpochMillisProvider
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouDailyRecommendationDecoder
import cn.james.music.kugou.api.endpoint.KugouHomeBannerDecoder
import cn.james.music.kugou.api.endpoint.KugouHomeRequestBuilder
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.endpoint.KugouTopPlaylistDecoder
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestContext
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouRetryMode
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouHomeProtocolTest {
    private val requestBuilder = KugouHomeRequestBuilder(clock = EpochMillisProvider { FIXTURE_TIME_MILLIS })
    private val requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { FIXTURE_TIME_SECONDS })
    private val context =
        KugouRequestContext(
            mid = "fixture-mid",
            dfid = "fixture-dfid",
            userId = "42",
            cookies = KugouCookies.from(mapOf("userid" to "42", "token" to "fixture-token")),
        )

    @Test
    fun homeRequestsMatchFixedNodeContracts() {
        val banners = requestFactory.prepare(requestBuilder.banners(context), context)
        val daily = requestFactory.prepare(requestBuilder.dailyRecommendations(), context)
        val playlists = requestFactory.prepare(requestBuilder.topPlaylists(context), context)

        assertEquals("/ads.gateway/v3/listen_banner", banners.path)
        assertEquals(EXPECTED_BANNER_BODY, requireNotNull(banners.body).decodeToString())
        assertEquals("be885593ada29cf06b02d1f58274507f", banners.query["signature"])
        assertEquals("/everyday_song_recommend", daily.path)
        assertEquals("ios", daily.query["platform"])
        assertEquals("everydayrec.service.kugou.com", daily.headers["x-router"])
        assertEquals("89c87d22b01224add808972eb86f0c1a", daily.query["signature"])
        assertEquals("/v2/special_recommend", playlists.path)
        assertEquals("specialrec.service.kugou.com", playlists.headers["x-router"])
        assertEquals("1700000000", playlists.query["clienttime"])
        val playlistBody = Json.parseToJsonElement(requireNotNull(playlists.body).decodeToString()).jsonObject
        assertEquals("fixture-mid", playlistBody.getValue("mid").jsonPrimitive.content)
        assertEquals("42", playlistBody.getValue("userid").jsonPrimitive.content)
        assertEquals(
            "1",
            playlistBody
                .getValue("special_recommend")
                .jsonObject
                .getValue("withsong")
                .jsonPrimitive.content,
        )
        assertEquals("a1f65b6a8fe7e191521406ce8661ae02", playlistBody.getValue("key").jsonPrimitive.content)
        assertEquals(EXPECTED_PLAYLIST_BODY, requireNotNull(playlists.body).decodeToString())
        assertEquals("df9a4a543057a629189431b6750853b0", playlists.query["signature"])
        listOf(banners, daily, playlists).forEach { request ->
            assertEquals(KugouRetryMode.IdempotentRead, request.retryMode)
            assertEquals("token=fixture-token; userid=42", request.headers["Cookie"])
            assertFalse(request.toString().contains("fixture-token"))
        }
    }

    @Test
    fun anonymousHomeRequestsUseNumericZeroWithoutCookie() {
        val anonymous = KugouRequestContext(mid = "fixture-mid", dfid = "fixture-dfid")
        val banners = requestFactory.prepare(requestBuilder.banners(anonymous), anonymous)
        val playlists = requestFactory.prepare(requestBuilder.topPlaylists(anonymous), anonymous)

        assertEquals(
            "0",
            Json
                .parseToJsonElement(requireNotNull(banners.body).decodeToString())
                .jsonObject
                .getValue("userid")
                .jsonPrimitive.content,
        )
        assertEquals(
            "0",
            Json
                .parseToJsonElement(requireNotNull(playlists.body).decodeToString())
                .jsonObject
                .getValue("userid")
                .jsonPrimitive.content,
        )
        assertFalse("userid" in banners.query)
        assertFalse("userid" in playlists.query)
        assertFalse("Cookie" in banners.headers)
        assertFalse("Cookie" in playlists.headers)
    }

    @Test
    fun topPlaylistKeepsNodeRoundedBodyTimeSeparateFromFlooredRequestTime() {
        val builder = KugouHomeRequestBuilder(clock = EpochMillisProvider { FIXTURE_TIME_MILLIS + 999 })
        val playlists = requestFactory.prepare(builder.topPlaylists(context), context)
        val body = Json.parseToJsonElement(requireNotNull(playlists.body).decodeToString()).jsonObject

        assertEquals("1700000000", playlists.query["clienttime"])
        assertEquals("1700000001", body.getValue("clienttime").jsonPrimitive.content)
        assertEquals("ff907dc06d2fac33b41b3f12226f0dbf", body.getValue("key").jsonPrimitive.content)
    }

    @Test
    fun bannerDecoderAcceptsCompatibleFieldsAndFiltersBadRows() {
        val result =
            KugouHomeBannerDecoder().decode(
                json(
                    """{"data":{"ads":[{"id":7,"extra":{"title":"精选"},"img_url":"https://example.test/{size}/a.jpg"},{"title":"bad"}]}}""",
                ),
            ) as KugouApiResult.Success

        assertEquals(1, result.value.size)
        assertEquals("7", result.value.single().id)
        assertEquals("精选", result.value.single().title)
        assertEquals("https://example.test/720/a.jpg", result.value.single().artworkUrl)
    }

    @Test
    fun dailyDecoderNormalizesMixedFieldsAndFilenameFallback() {
        val result =
            KugouDailyRecommendationDecoder().decode(
                json(
                    """{"data":{"song_list":[{"hash":"ABC","filename":"Artist - Title","album_id":7,"audio_id":"9","time_length":"215","trans_param":{"union_cover":"https://example.test/{size}/a.jpg"},"privilege":"10","publish_date":"20260806"},{"songname":"bad"}]}}""",
                ),
            ) as KugouApiResult.Success

        val song = result.value.single()
        assertEquals("Title", song.title)
        assertEquals("Artist", song.artistName)
        assertEquals("7", song.albumId)
        assertEquals("9", song.albumAudioId)
        assertEquals(215_000, song.durationMs)
        assertEquals(10, song.privilege)
        assertEquals("https://example.test/240/a.jpg", song.artworkUrl)
    }

    @Test
    fun playlistDecoderKeepsOptionalDisplayFieldsTyped() {
        val result =
            KugouTopPlaylistDecoder().decode(
                json(
                    """{"data":{"special_list":[{"global_collection_id":"collection-1","specialname":"清新旋律","flexible_cover":"https://example.test/{size}/p.jpg","play_count":"12345"}]}}""",
                ),
            ) as KugouApiResult.Success

        val playlist = result.value.single()
        assertEquals("collection-1", playlist.id)
        assertEquals("清新旋律", playlist.title)
        assertEquals(12_345L, playlist.playCount)
        assertEquals("https://example.test/480/p.jpg", playlist.artworkUrl)
    }

    @Test
    fun emptyListsAreValidButMissingOrEntirelyUnusableListsFail() {
        val empty = KugouHomeBannerDecoder().decode(json("""{"data":{"ads":[]}}"""))
        val missing = KugouDailyRecommendationDecoder().decode(json("""{"data":{}}"""))
        val unusable = KugouTopPlaylistDecoder().decode(json("""{"data":{"special_list":[{"specialname":"missing id"}]}}"""))
        val malformed = KugouTopPlaylistDecoder().decode(json("""{"data":{"special_list":{}}}"""))

        assertTrue(empty is KugouApiResult.Success)
        assertEquals(KugouError.Protocol.Reason.MissingRequiredField, (missing as KugouApiResult.Failure).protocolReason())
        assertEquals(KugouError.Protocol.Reason.MissingRequiredField, (unusable as KugouApiResult.Failure).protocolReason())
        assertEquals(KugouError.Protocol.Reason.MalformedResponse, (malformed as KugouApiResult.Failure).protocolReason())
    }

    @Test
    fun clientUsesTypedDecoderAfterFakeTransport() =
        runBlocking {
            val transport =
                RecordingTransport(
                    response("""{"data":{"ads":[]}}"""),
                    response("""{"data":{"song_list":[]}}"""),
                    response("""{"data":{"special_list":[]}}"""),
                )
            val client = KugouOnlineClient(KugouCallExecutor(requestFactory, transport))

            assertTrue(client.fetchHomeBanners(context) is KugouApiResult.Success)
            assertTrue(client.fetchDailyRecommendations(context) is KugouApiResult.Success)
            assertTrue(client.fetchTopPlaylists(context) is KugouApiResult.Success)
            assertEquals(listOf("yueku_banner", "everyday_recommend", "top_playlist"), transport.requests.map { it.id })
            assertNull(transport.failure)
        }

    @Test
    fun clientPropagatesTypedTransportFailureAndCancellation() {
        val offline = KugouError.Network(KugouError.Network.Kind.Offline)
        val failureClient = KugouOnlineClient(KugouCallExecutor(requestFactory, KugouTransport { KugouTransportResult.Failure(offline) }))

        val failure = runBlocking { failureClient.fetchDailyRecommendations(context) }
        assertEquals(offline, (failure as KugouApiResult.Failure).error)

        val cancelledClient =
            KugouOnlineClient(
                KugouCallExecutor(requestFactory, KugouTransport { throw CancellationException("fixture cancellation") }),
            )
        assertThrows(CancellationException::class.java) {
            runBlocking { cancelledClient.fetchTopPlaylists(context) }
        }
    }

    private fun KugouApiResult.Failure.protocolReason() = (error as KugouError.Protocol).reason

    private fun json(value: String) = Json.parseToJsonElement(value)

    private fun response(body: String) =
        KugouTransportResult.Success(
            KugouRawResponse(statusCode = 200, headers = emptyMap(), body = body.encodeToByteArray()),
        )

    private class RecordingTransport(
        vararg responses: KugouTransportResult,
    ) : KugouTransport {
        private val queued = ArrayDeque(responses.toList())
        val requests = mutableListOf<KugouPreparedRequest>()
        var failure: Throwable? = null

        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult {
            requests += request
            return runCatching { queued.removeFirst() }
                .onFailure { failure = it }
                .getOrThrow()
        }
    }

    private companion object {
        const val FIXTURE_TIME_SECONDS = 1_700_000_000L
        const val FIXTURE_TIME_MILLIS = FIXTURE_TIME_SECONDS * 1_000
        const val EXPECTED_BANNER_BODY =
            "{\"plat\":0,\"channel\":201,\"operator\":7,\"networktype\":2,\"userid\":\"42\"," +
                "\"vip_type\":0,\"m_type\":0,\"tags\":[],\"apiver\":5,\"ability\":2,\"mode\":\"normal\"}"
        const val EXPECTED_PLAYLIST_BODY =
            "{\"appid\":1005,\"mid\":\"fixture-mid\",\"clientver\":20489,\"platform\":\"android\"," +
                "\"clienttime\":1700000000,\"userid\":\"42\",\"module_id\":1,\"page\":1,\"pagesize\":6," +
                "\"key\":\"a1f65b6a8fe7e191521406ce8661ae02\",\"special_recommend\":{" +
                "\"withtag\":1,\"withsong\":1,\"sort\":1,\"ugc\":1,\"is_selected\":0," +
                "\"withrecommend\":1,\"area_code\":1,\"categoryid\":0},\"req_multi\":1," +
                "\"retrun_min\":5,\"return_special_falg\":1}"
    }
}
