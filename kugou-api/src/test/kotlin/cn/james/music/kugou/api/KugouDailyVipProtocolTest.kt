package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.KugouDailyVipClient
import cn.james.music.kugou.api.endpoint.KugouDailyVipDayResult
import cn.james.music.kugou.api.endpoint.KugouDailyVipRequestBuilder
import cn.james.music.kugou.api.endpoint.KugouDailyVipService
import cn.james.music.kugou.api.endpoint.KugouDailyVipUpgradeResult
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestContext
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouRetryMode
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouDailyVipProtocolTest {
    private val requestBuilder = KugouDailyVipRequestBuilder()
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
    fun dayAndUpgradeRequestsMatchFixedContractsAndKeepSecretsRedacted() {
        val day = requestFactory.prepare(requestBuilder.claimDay("2026-08-10"), context)
        val upgrade = requestFactory.prepare(requestBuilder.upgrade(42), context)

        assertRequest(
            request = day,
            id = "daily_vip_claim_day",
            path = "/youth/v1/recharge/receive_vip_listen_song",
            parameters = mapOf("source_id" to "90139", "receive_day" to "2026-08-10"),
            expectedSignature = "d1d87029932d4b0e53ccdeba58ca5df4",
            expectedContentType = "application/x-www-form-urlencoded",
        )
        assertRequest(
            request = upgrade,
            id = "daily_vip_upgrade",
            path = "/youth/v1/listen_song/upgrade_vip_reward",
            parameters = mapOf("kugouid" to "42", "ad_type" to "1"),
            expectedSignature = "b241154df87b9474c7bf22b36364e537",
            expectedContentType = null,
        )
        assertFalse(day.toString().contains("fixture-token"))
        assertFalse(upgrade.toString().contains("fixture-token"))
    }

    @Test
    fun publicServiceSurfaceAndRequestIdsExcludeRejectedLegacyEndpoint() {
        val publicMethods =
            KugouDailyVipService::class.java.methods
                .filter { it.declaringClass == KugouDailyVipService::class.java }
                .map { it.name }
                .toSet()
        val requestIds =
            listOf(
                requestBuilder.claimDay("2026-08-10").id,
                requestBuilder.upgrade(42).id,
            )

        assertEquals(setOf("claimDay", "upgrade"), publicMethods)
        assertTrue(
            requestIds.none {
                it.equals("youth_vip", ignoreCase = true) ||
                    it.contains("youth_vip", ignoreCase = true) ||
                    it.contains("legacy", ignoreCase = true)
            },
        )
    }

    @Test
    fun dayAndUpgradeMapEndpointSpecificCompletionAndAlreadyClaimed() =
        runBlocking {
            val client =
                client(
                    response("""{"status":1}"""),
                    response("""{"status":0,"error_code":131001}"""),
                )

            assertEquals(KugouDailyVipDayResult.Claimed, client.claimDay("2026-08-10", context))
            assertEquals(KugouDailyVipUpgradeResult.AlreadyClaimed, client.upgrade(context))
        }

    @Test
    fun upgradeMapsEndpointSpecificCompletion() =
        runBlocking {
            val client = client(response("""{"status":1}"""))

            assertEquals(KugouDailyVipUpgradeResult.Upgraded, client.upgrade(context))
        }

    @Test
    fun riskHeaderAndKnownRiskCodeStayTyped() =
        runBlocking {
            val headerRisk =
                client(
                    response(
                        body = """{"status":1}""",
                        headers = mapOf("ssa-code" to listOf("risk-fixture")),
                    ),
                )
            val bodyRisk = client(response("""{"status":0,"error_code":20028}"""))

            assertEquals(
                KugouDailyVipDayResult.Failure(KugouError.Risk("risk-fixture")),
                headerRisk.claimDay("2026-08-10", context),
            )
            assertEquals(
                KugouDailyVipUpgradeResult.Failure(KugouError.Risk("20028")),
                bodyRisk.upgrade(context),
            )
        }

    @Test
    fun malformedAndUnknownServiceCodesRejectWithoutUsingErrorMessage() =
        runBlocking {
            val malformed = client(response("not-json"))
            val unknown =
                client(
                    response(
                        """{"status":0,"error_code":999999,"error_msg":{"must_not":"be read"}}""",
                    ),
                )

            assertEquals(
                KugouDailyVipDayResult.Failure(
                    KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
                ),
                malformed.claimDay("2026-08-10", context),
            )
            assertEquals(
                KugouDailyVipUpgradeResult.Failure(
                    KugouError.Protocol(
                        reason = KugouError.Protocol.Reason.ServiceRejected,
                        serviceCode = "999999",
                    ),
                ),
                unknown.upgrade(context),
            )
        }

    @Test
    fun missingOrInvalidSessionStopsBeforeTransport() =
        runBlocking {
            val transport = RecordingTransport(response("""{"status":1}"""))
            val client = client(transport)

            assertEquals(
                KugouDailyVipDayResult.Failure(
                    KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
                ),
                client.claimDay("2026-08-10", context.copy(token = " ")),
            )
            assertEquals(
                KugouDailyVipUpgradeResult.Failure(
                    KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
                ),
                client.upgrade(context.copy(userId = "0")),
            )
            assertTrue(transport.requests.isEmpty())
        }

    @Test
    fun mutationsDoNotRetryTimeoutOrServerFailure() =
        runBlocking {
            val timeout =
                RecordingTransport(
                    KugouTransportResult.Failure(KugouError.Network(KugouError.Network.Kind.Timeout)),
                    response("""{"status":1}"""),
                )
            val server = RecordingTransport(response("""{"status":1}""", statusCode = 503), response("""{"status":1}"""))

            assertEquals(
                KugouDailyVipDayResult.Failure(KugouError.Network(KugouError.Network.Kind.Timeout)),
                client(timeout).claimDay("2026-08-10", context),
            )
            assertEquals(
                KugouDailyVipUpgradeResult.Failure(KugouError.Http(503)),
                client(server).upgrade(context),
            )
            assertEquals(1, timeout.requests.size)
            assertEquals(1, server.requests.size)
        }

    @Test(expected = CancellationException::class)
    fun cancellationPropagatesWithoutMapping() {
        runBlocking {
            client(KugouTransport { throw CancellationException("fixture cancellation") })
                .claimDay("2026-08-10", context)
        }
    }

    private fun assertRequest(
        request: KugouPreparedRequest,
        id: String,
        path: String,
        parameters: Map<String, String>,
        expectedSignature: String,
        expectedContentType: String?,
    ) {
        assertEquals(id, request.id)
        assertEquals(KugouHttpMethod.Post, request.method)
        assertEquals("https://gateway.kugou.com", request.baseUrl)
        assertEquals(path, request.path)
        parameters.forEach { (name, value) -> assertEquals(value, request.query[name]) }
        if (expectedContentType == null) {
            assertFalse(request.headers.keys.any { it.equals("Content-Type", ignoreCase = true) })
        } else {
            assertEquals(expectedContentType, request.headers["Content-Type"])
        }
        assertEquals("token=fixture-token; userid=42", request.headers["Cookie"])
        assertNull(request.body)
        assertEquals(KugouRetryMode.None, request.retryMode)
        assertEquals(expectedSignature, request.query["signature"])
    }

    private fun client(vararg responses: KugouTransportResult): KugouDailyVipClient = client(RecordingTransport(*responses))

    private fun client(transport: KugouTransport): KugouDailyVipClient = KugouDailyVipClient(KugouCallExecutor(requestFactory, transport))

    private fun response(
        body: String,
        statusCode: Int = 200,
        headers: Map<String, List<String>> = emptyMap(),
    ) = KugouTransportResult.Success(
        KugouRawResponse(
            statusCode = statusCode,
            headers = headers,
            body = body.encodeToByteArray(),
        ),
    )

    private class RecordingTransport(
        vararg responses: KugouTransportResult,
    ) : KugouTransport {
        private val responses = ArrayDeque(responses.toList())
        val requests = mutableListOf<KugouPreparedRequest>()

        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult {
            requests += request
            return responses.removeFirst()
        }
    }

    private companion object {
        const val FIXTURE_TIME_SECONDS = 1_700_000_000L
    }
}
