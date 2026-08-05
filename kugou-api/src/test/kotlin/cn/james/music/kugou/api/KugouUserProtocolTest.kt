package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouUserClient
import cn.james.music.kugou.api.endpoint.KugouUserDetailDecoder
import cn.james.music.kugou.api.endpoint.KugouUserRequestBuilder
import cn.james.music.kugou.api.endpoint.KugouVipSummaryDecoder
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouUserProtocolTest {
    private val requestBuilder = KugouUserRequestBuilder(EpochSecondsProvider { FIXTURE_TIME_SECONDS })
    private val requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { FIXTURE_TIME_SECONDS })
    private val context =
        KugouRequestContext(
            mid = "fixture-mid",
            dfid = "fixture-dfid",
            token = "fixture-token",
            userId = "42",
            cookies = KugouCookies.from(mapOf("token" to "fixture-token", "userid" to "42")),
        )

    @Test
    fun userDetailRequestMatchesFixedNodeContract() {
        val request = requestFactory.prepare(requestBuilder.userDetail("fixture-token", 42), context)

        assertEquals("https://gateway.kugou.com", request.baseUrl)
        assertEquals("/v3/get_my_info", request.path)
        assertEquals("1", request.query["plat"])
        assertEquals("usercenter.kugou.com", request.headers["x-router"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals(EXPECTED_BODY, requireNotNull(request.body).decodeToString())
        assertEquals("f364533b201a70ced4948117122f0773", request.query["signature"])
        assertEquals("token=fixture-token; userid=42", request.headers["Cookie"])
        assertEquals(KugouRetryMode.IdempotentRead, request.retryMode)
        assertFalse(request.toString().contains("fixture-token"))
    }

    @Test
    fun vipRequestMatchesFixedContract() {
        val request = requestFactory.prepare(requestBuilder.vipSummary(), context)

        assertEquals("https://kugouvip.kugou.com", request.baseUrl)
        assertEquals("/v1/get_union_vip", request.path)
        assertEquals("concept", request.query["busi_type"])
        assertEquals(KugouRetryMode.IdempotentRead, request.retryMode)
        assertEquals("token=fixture-token; userid=42", request.headers["Cookie"])
    }

    @Test
    fun userDetailDecoderMapsCompatibleFieldsAndDefaults() {
        val result =
            KugouUserDetailDecoder().decode(
                Json.parseToJsonElement(
                    """{"status":1,"data":{"userid":42,"nickname":"Fixture","pic":"https://example.test/avatar","bg_pic":"","descri":"Hello","fans":"8","follows":-2,"duration":120}}""",
                ),
            )

        assertTrue(result is KugouApiResult.Success)
        val detail = (result as KugouApiResult.Success).value
        assertEquals("42", detail.userId)
        assertEquals("Fixture", detail.nickname)
        assertEquals("https://example.test/avatar", detail.avatarUrl)
        assertNull(detail.backgroundUrl)
        assertEquals("Hello", detail.signature)
        assertEquals(8, detail.fans)
        assertEquals(0, detail.follows)
        assertEquals(120, detail.listenMinutes)
    }

    @Test
    fun vipDecoderSeparatesActiveInactiveAndMalformed() {
        val active =
            KugouVipSummaryDecoder().decode(
                Json.parseToJsonElement("""{"data":{"busi_vip":[{"is_vip":"1","product_type":"svip"}]}}"""),
            ) as KugouApiResult.Success
        val inactive =
            KugouVipSummaryDecoder().decode(
                Json.parseToJsonElement("""{"data":{"busi_vip":[]}}"""),
            ) as KugouApiResult.Success
        val malformed =
            KugouVipSummaryDecoder().decode(
                Json.parseToJsonElement("""{"data":{}}"""),
            ) as KugouApiResult.Failure

        assertTrue(active.value.isActive)
        assertEquals("SVIP", active.value.label)
        assertFalse(inactive.value.isActive)
        assertNull(inactive.value.label)
        assertEquals(
            KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
            malformed.error,
        )
    }

    @Test
    fun clientRejectsMissingSessionWithoutTransport() =
        runBlocking {
            val transport = RecordingTransport(response("""{"data":{}}"""))
            val client =
                KugouUserClient(
                    executor = KugouCallExecutor(requestFactory, transport),
                    requestBuilder = requestBuilder,
                    userDetailDecoder = KugouUserDetailDecoder(),
                    vipSummaryDecoder = KugouVipSummaryDecoder(),
                )

            val result = client.fetchUserDetail(KugouRequestContext(mid = "fixture-mid"))

            assertEquals(
                KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
                (result as KugouApiResult.Failure).error,
            )
            assertTrue(transport.requests.isEmpty())
        }

    @Test
    fun vipClientRejectsNonNumericUserIdWithoutTransport() =
        runBlocking {
            val transport = RecordingTransport(response("""{"data":{"busi_vip":[]}}"""))
            val client = KugouUserClient(KugouCallExecutor(requestFactory, transport))

            val result =
                client.fetchVipSummary(
                    context.copy(userId = "invalid"),
                )

            assertEquals(
                KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
                (result as KugouApiResult.Failure).error,
            )
            assertTrue(transport.requests.isEmpty())
        }

    private fun response(body: String) =
        KugouTransportResult.Success(
            KugouRawResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = body.encodeToByteArray(),
            ),
        )

    private class RecordingTransport(
        private val response: KugouTransportResult,
    ) : KugouTransport {
        val requests = mutableListOf<KugouPreparedRequest>()

        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult {
            requests += request
            return response
        }
    }

    private companion object {
        const val FIXTURE_TIME_SECONDS = 1_700_000_000L
        const val EXPECTED_BODY =
            "{\"visit_time\":1700000000,\"usertype\":1,\"p\":\"" +
                "666F8A531320DDE7CA88EFEFA26CD79969298ECB1C99CCA5C0B6E134059854B7" +
                "2900DACC95EDB10D7565022687C772D0057DD2229030F03C08D752717EE3C195" +
                "F0DBD968A029B48D72D7FE28B44B70C4E75F2D4026058EF9B2E26E842F86E" +
                "CD8BFBEE22A22FFC41A64E0A956AD9A80D838B715164C3BE9A3E4B4EFE537B" +
                "AC19A\",\"userid\":42}"
    }
}
