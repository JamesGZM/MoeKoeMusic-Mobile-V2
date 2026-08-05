package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.KugouAudioResource
import cn.james.music.kugou.api.endpoint.KugouEndpoints
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouRequestContext
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouResponseFormat
import cn.james.music.kugou.api.transport.KugouRetryMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouEndpointsTest {
    private val factory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L })
    private val context = KugouRequestContext(mid = "fixture-mid", dfid = "fixture-dfid")

    @Test
    fun searchEndpointUsesFixedSongContract() {
        val request = factory.prepare(KugouEndpoints.searchSongs("  MoeKoe 测试  ", page = 2), context)

        assertEquals("/v3/search/song", request.path)
        assertEquals("MoeKoe 测试", request.query["keyword"])
        assertEquals("2", request.query["page"])
        assertEquals("complexsearch.kugou.com", request.headers["x-router"])
        assertEquals(KugouRetryMode.IdempotentRead, request.retryMode)
    }

    @Test
    fun anonymousSearchUsesHttpsLegacyContractWithoutDeviceHeaders() {
        val request = factory.prepare(KugouEndpoints.searchSongsAnonymous("  MoeKoe 测试  ", page = 2), context)

        assertEquals("https://songsearch.kugou.com", request.baseUrl)
        assertEquals("/song_search_v2", request.path)
        assertEquals("MoeKoe 测试", request.query["keyword"])
        assertEquals("2", request.query["page"])
        assertEquals("WebFilter", request.query["platform"])
        assertFalse("signature" in request.query)
        assertFalse("mid" in request.query)
        assertFalse("mid" in request.headers)
        assertFalse("Cookie" in request.headers)
        assertEquals(KugouRetryMode.IdempotentRead, request.retryMode)
    }

    @Test
    fun registerEndpointKeepsEncryptedBytesAndDoesNotRetry() {
        val body = byteArrayOf(0, 1, 2, -1)
        val request = factory.prepare(KugouEndpoints.registerDevice(body, "fixture-encrypted-identity"), context)

        assertEquals("https://userservice.kugou.com", request.baseUrl)
        assertEquals("/risk/v2/r_register_dev", request.path)
        assertEquals("fixture-encrypted-identity", request.query["p"])
        assertTrue(body.contentEquals(request.body))
        assertEquals(KugouResponseFormat.Bytes, request.responseFormat)
        assertEquals(KugouRetryMode.None, request.retryMode)
    }

    @Test
    fun privilegeEndpointBuildsProtocolJsonWithoutDynamicFields() {
        val request =
            factory.prepare(
                KugouEndpoints.privilegeLite(
                    listOf(
                        KugouAudioResource(hash = "ABCDEF", albumId = 7),
                        KugouAudioResource(hash = "123456", albumId = 9),
                    ),
                ),
                context,
            )
        val json = Json.parseToJsonElement(requireNotNull(request.body).decodeToString()).jsonObject
        val resources = json.getValue("resource").jsonArray

        assertEquals("1005", json.getValue("appid").jsonPrimitive.content)
        assertEquals(
            "ABCDEF",
            resources
                .first()
                .jsonObject
                .getValue("hash")
                .jsonPrimitive.content,
        )
        assertEquals(
            "9",
            resources
                .last()
                .jsonObject
                .getValue("album_id")
                .jsonPrimitive.content,
        )
        assertEquals("media.store.kugou.com", request.headers["x-router"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals(KugouRetryMode.IdempotentRead, request.retryMode)
    }

    @Test
    fun songUrlMatchesFixedNodeKeyAndSignature() {
        val request =
            factory.prepare(
                KugouEndpoints.songUrl(
                    hash = "ABCDEF012345",
                    albumId = 7,
                    albumAudioId = 9,
                ),
                context,
            )

        assertEquals("abcdef012345", request.query["hash"])
        assertEquals("11430", request.query["clientver"])
        assertEquals("bcd8cf9e54fbd406da3260d9cc9abee0", request.query["key"])
        assertEquals("f5af8938b8d0c11e68a7d55ca366e99f", request.query["signature"])
        assertEquals("trackercdn.kugou.com", request.headers["x-router"])
        assertFalse(request.toString().contains("abcdef012345"))
    }
}
