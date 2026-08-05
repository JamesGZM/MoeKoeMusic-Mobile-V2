package cn.james.music.kugou.api

import cn.james.music.kugou.api.crypto.KugouAuthCrypto
import cn.james.music.kugou.api.endpoint.EpochMillisProvider
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouAuthClient
import cn.james.music.kugou.api.endpoint.KugouAuthRequestBuilder
import cn.james.music.kugou.api.endpoint.KugouAuthTemporaryKeyProvider
import cn.james.music.kugou.api.endpoint.KugouMobileLoginDecoder
import cn.james.music.kugou.api.endpoint.KugouMobileLoginResult
import cn.james.music.kugou.api.endpoint.KugouPasswordLoginDecoder
import cn.james.music.kugou.api.endpoint.KugouPasswordLoginResult
import cn.james.music.kugou.api.endpoint.KugouRiskChallengeDto
import cn.james.music.kugou.api.endpoint.KugouRiskMethodDecoder
import cn.james.music.kugou.api.endpoint.KugouRiskMethodDto
import cn.james.music.kugou.api.endpoint.KugouRiskProofDto
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestContext
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouAuthProtocolTest {
    private val requestBuilder =
        KugouAuthRequestBuilder(
            clock = EpochMillisProvider { FIXTURE_TIME_MS },
            keyProvider = KugouAuthTemporaryKeyProvider { FIXTURE_KEY },
        )
    private val requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { FIXTURE_TIME_SECONDS })
    private val context = KugouRequestContext(mid = "fixture-mid", dfid = "fixture-dfid")

    @Test
    fun mobileLoginRequestMatchesFixedNodeContract() {
        val authRequest = requestBuilder.loginWithMobileCode("13800000000", "246810", selectedUserId = "42")
        val request = requestFactory.prepare(authRequest.spec, context)
        val body = Json.parseToJsonElement(requireNotNull(request.body).decodeToString()).jsonObject

        assertEquals("https://loginserviceretry.kugou.com", request.baseUrl)
        assertEquals("/v7/login_by_verifycode", request.path)
        assertEquals("13*****0", body.getValue("mobile").jsonPrimitive.content)
        assertEquals("42", body.getValue("userid").jsonPrimitive.content)
        assertEquals("e3e86865c059fdec1dd020b6d640b8a5", body.getValue("key").jsonPrimitive.content)
        assertEquals(
            "b8be3bbf25d910e805a8fa2cc4f784339f3673b7fe4914d55c885fa72a3dde4685f1d891d14581826d0e576b8c9926b2",
            body.getValue("params").jsonPrimitive.content,
        )
        assertEquals(
            "2371591A7A8D41AA5890429388F973328BFFD3EDDD332F0B32F8DF5EF2FD28DC1E062E5775D66E81B14B0AFDE62CB271F73991F2F7EA4C404FF3C23974C820061220246AA50C8CD47F714F59AED731E4E29D0F34EFF3C46002E0368DD326BD7CBC7B4586ABBF9C8EB61D4E55837E3404243A24D51B705811AA1CA5745394515A",
            body.getValue("pk").jsonPrimitive.content,
        )
        assertEquals("1", request.headers["support-calm"])
        assertEquals("Android16-1070-11440-130-0-LOGIN-wifi", request.headers["User-Agent"])
        assertEquals("b7e12ff10d30ed4d68033e9b528112de", request.query["signature"])
        assertEquals(cn.james.music.kugou.api.transport.KugouRetryMode.None, request.retryMode)
        assertFalse(request.toString().contains("246810"))
    }

    @Test
    fun passwordLoginRequestMatchesFixedNodeContract() {
        val authRequest = requestBuilder.loginWithPassword("fixture@example.test", "fixture-password")
        val request = requestFactory.prepare(authRequest.spec, context)
        val bodyText = requireNotNull(request.body).decodeToString()
        val body = Json.parseToJsonElement(bodyText).jsonObject

        assertEquals("https://gateway.kugou.com", request.baseUrl)
        assertEquals("/v9/login_by_pwd", request.path)
        assertEquals("login.user.kugou.com", request.headers["x-router"])
        assertEquals("fixture@example.test", body.getValue("username").jsonPrimitive.content)
        assertEquals(
            "6851e0040e371bf2c8149dc636ee523faa192e83d6e027829a1fab847c986b286f11b7eef0c825aa9f451e475287e9ae09189e91753ffb70d3b6957e27116c19928fd9ec682389403b2baee8c649c28f",
            body.getValue("params").jsonPrimitive.content,
        )
        assertEquals(
            "2371591A7A8D41AA5890429388F973328BFFD3EDDD332F0B32F8DF5EF2FD28DC1E062E5775D66E81B14B0AFDE62CB271F73991F2F7EA4C404FF3C23974C820061220246AA50C8CD47F714F59AED731E4E29D0F34EFF3C46002E0368DD326BD7CBC7B4586ABBF9C8EB61D4E55837E3404243A24D51B705811AA1CA5745394515A",
            body.getValue("pk").jsonPrimitive.content,
        )
        assertEquals("58439887896579f52650ed33b4237ac3", request.query["signature"])
        assertEquals(cn.james.music.kugou.api.transport.KugouRetryMode.None, request.retryMode)
        assertFalse(bodyText.contains("fixture-password"))
        assertFalse(request.toString().contains("fixture-password"))
    }

    @Test
    fun riskSmsVerificationRequestMatchesFixedNodeContract() {
        val challenge = KugouRiskChallengeDto("fixture-event", "fixture-sid", "fixture-edt")
        val spec = requestBuilder.verifyRisk(challenge, KugouRiskProofDto.Sms("246810"), userId = null)
        val request = requestFactory.prepare(spec, context)
        val body = Json.parseToJsonElement(requireNotNull(request.body).decodeToString()).jsonObject

        assertEquals("https://verifyservice.kugou.com", request.baseUrl)
        assertEquals("/v4/verify_user_info", request.path)
        assertEquals("11510", request.query["clientver"])
        assertEquals("32", body.getValue("v_type").jsonPrimitive.content)
        assertEquals("0", body.getValue("userid").jsonPrimitive.content)
        assertEquals("fixture-sid", body.getValue("sid").jsonPrimitive.content)
        assertEquals(
            "b0fde81c1508743a631706655e17f33ee1a49fb66c6ad89e5303f5b8c0f7ad65",
            body.getValue("params").jsonPrimitive.content,
        )
        assertEquals(
            "157FC54389EEDFA716E5466B60C6275BD40953955AB840A6AEA1A79E1DDE4FEDA5A33DDF36A3F0CBB48473386F675B57B14AD04B83D1210527767A60490C87A4EB14E8A09BD496623FB4E226782532E582E4717412FDE9694A753C1EA29C23474E3D99C935754ADD35D9196AE476EE930ACCD3E150B715726E6BB36F55B2A507",
            body.getValue("pk").jsonPrimitive.content,
        )
        assertEquals("03a8b8a06773046a433a0a1db0e9fbcf", request.query["signature"])
        assertEquals(cn.james.music.kugou.api.transport.KugouRetryMode.None, request.retryMode)
        assertFalse(request.toString().contains("246810"))
    }

    @Test
    fun tencentRiskProofUsesFixedTicketEnvelopeWithoutSmsField() {
        val challenge = KugouRiskChallengeDto("fixture-event", null, null)
        val proof = KugouRiskProofDto.Tencent("fixture-ticket", "fixture-random", "fixture-app")
        val request = requestFactory.prepare(requestBuilder.verifyRisk(challenge, proof, null), context)
        val body = Json.parseToJsonElement(requireNotNull(request.body).decodeToString()).jsonObject

        assertEquals("23", body.getValue("v_type").jsonPrimitive.content)
        assertEquals("", body.getValue("sid").jsonPrimitive.content)
        assertFalse("code" in body)
        assertEquals(
            "KGCodeTX|{\"ticket\":\"fixture-ticket\",\"randstr\":\"fixture-random\",\"txappid\":\"fixture-app\"}",
            body.getValue("verifycode").jsonPrimitive.content,
        )
        assertEquals("bf25688c537cfc847676034c7c9827bd", body.getValue("params").jsonPrimitive.content)
        assertFalse(proof.toString().contains("fixture-ticket"))
    }

    @Test
    fun riskMethodRequestKeepsChallengeInsideSignedBody() {
        val request = requestFactory.prepare(requestBuilder.getRiskMethod("fixture-event", null), context)
        val bodyText = requireNotNull(request.body).decodeToString()
        val body = Json.parseToJsonElement(bodyText).jsonObject

        assertEquals("/verifyservice/v3/get_verify_info", request.path)
        assertEquals("fixture-event", body.getValue("eventid").jsonPrimitive.content)
        assertEquals("0", body.getValue("userid").jsonPrimitive.content)
        assertEquals(cn.james.music.kugou.api.transport.KugouRetryMode.None, request.retryMode)
        assertFalse(request.toString().contains("fixture-event"))
    }

    @Test
    fun decoderReturnsAuthenticatedSessionAndCookiePatch() {
        val encrypted =
            KugouAuthCrypto.encryptJson(
                """{"token":"fixture-token","userid":42,"vip_type":1,"vip_token":"fixture-vip"}""",
                FIXTURE_KEY,
            )
        val result =
            KugouMobileLoginDecoder().decode(
                body =
                    Json.parseToJsonElement(
                        """{"status":1,"data":{"secu_params":"${encrypted.ciphertextHex}","t1":"fixture-t1"}}""",
                    ),
                responseCookies = KugouCookies.from(mapOf("server" to "fixture")),
                temporaryKey = FIXTURE_KEY,
            )

        assertTrue(result is KugouMobileLoginResult.Authenticated)
        val session = (result as KugouMobileLoginResult.Authenticated).session
        assertEquals("fixture-token", session.token)
        assertEquals("42", session.userId)
        assertEquals("fixture-token", session.cookies.value("token"))
        assertEquals("fixture-t1", session.cookies.value("t1"))
        assertEquals("fixture", session.cookies.value("server"))
        assertFalse(session.toString().contains("fixture-token"))
    }

    @Test
    fun decoderSeparatesMultipleAccountsFromServiceRejection() {
        val multiple =
            KugouMobileLoginDecoder().decode(
                body =
                    Json.parseToJsonElement(
                        """{"status":0,"error_code":20001,"data":{"info_list":[{"userid":42,"nickname":"Fixture","pic":"https://example.test/avatar","p_grade":7},{"userid":0}]}}""",
                    ),
                responseCookies = KugouCookies.Empty,
                temporaryKey = FIXTURE_KEY,
            )
        val rejected =
            KugouMobileLoginDecoder().decode(
                body = Json.parseToJsonElement("""{"status":0,"error_code":20014}"""),
                responseCookies = KugouCookies.Empty,
                temporaryKey = FIXTURE_KEY,
            )

        assertTrue(multiple is KugouMobileLoginResult.MultipleAccounts)
        assertEquals(listOf("42"), (multiple as KugouMobileLoginResult.MultipleAccounts).accounts.map { it.userId })
        assertEquals(
            KugouError.Protocol(KugouError.Protocol.Reason.ServiceRejected, "20014"),
            (rejected as KugouMobileLoginResult.Failure).error,
        )
    }

    @Test
    fun decoderRejectsSuccessWithoutEncryptedSession() {
        val result =
            KugouMobileLoginDecoder().decode(
                body = Json.parseToJsonElement("""{"status":1,"data":{"userid":42}}"""),
                responseCookies = KugouCookies.Empty,
                temporaryKey = FIXTURE_KEY,
            )

        assertEquals(
            KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
            (result as KugouMobileLoginResult.Failure).error,
        )
    }

    @Test
    fun passwordDecoderPreservesBodyOrHeaderRiskWithoutInventingFingerprint() {
        val decoder = KugouPasswordLoginDecoder()
        val challenge =
            decoder.decode(
                body =
                    Json.parseToJsonElement(
                        """{"status":0,"error_code":20028,"ssaCode":"fixture-event","sid":"fixture-sid","edt":"fixture-edt"}""",
                    ),
                responseCookies = KugouCookies.Empty,
                temporaryKey = FIXTURE_KEY,
            )
        val headerOnly =
            decoder.decode(
                body = Json.parseToJsonElement("""{"status":0,"error_code":20028}"""),
                responseCookies = KugouCookies.Empty,
                temporaryKey = FIXTURE_KEY,
                responseRiskCode = "header-event",
            )

        assertTrue(challenge is KugouPasswordLoginResult.RiskChallenge)
        val risk = challenge as KugouPasswordLoginResult.RiskChallenge
        assertEquals("fixture-event", risk.challenge.eventId)
        assertFalse(risk.challenge.toString().contains("fixture-sid"))
        val headerRisk = (headerOnly as KugouPasswordLoginResult.RiskChallenge).challenge
        assertEquals("header-event", headerRisk.eventId)
        assertEquals(null, headerRisk.sid)
        assertEquals(null, headerRisk.edt)
    }

    @Test
    fun riskMethodDecoderTypesSmsTencentAndUnsupportedWithoutDynamicJson() {
        val decoder = KugouRiskMethodDecoder()

        assertEquals(
            KugouApiResult.Success(KugouRiskMethodDto.Sms),
            decoder.decode(Json.parseToJsonElement("""{"status":1,"data":{"v_type":32}}""")),
        )
        assertEquals(
            KugouApiResult.Success(KugouRiskMethodDto.Tencent("fixture-app")),
            decoder.decode(Json.parseToJsonElement("""{"status":1,"data":{"v_type":23,"txappid":"fixture-app"}}""")),
        )
        assertEquals(
            KugouApiResult.Success(KugouRiskMethodDto.Unsupported(99)),
            decoder.decode(Json.parseToJsonElement("""{"status":1,"data":{"v_type":99}}""")),
        )
    }

    @Test
    fun authClientScopesCaptchaIdentityAndDecodesLogin() =
        runBlocking {
            val loginPayload =
                KugouAuthCrypto.encryptJson("""{"token":"fixture-token","userid":"42"}""", FIXTURE_KEY)
            val transport =
                QueueTransport(
                    response("""{"status":1}"""),
                    response("""{"status":1,"data":{"secu_params":"${loginPayload.ciphertextHex}"}}"""),
                )
            val client =
                KugouAuthClient(
                    executor = KugouCallExecutor(requestFactory, transport),
                    requestBuilder = requestBuilder,
                    mobileLoginDecoder = KugouMobileLoginDecoder(),
                )
            val authenticatedContext =
                KugouRequestContext(
                    mid = "fixture-mid",
                    dfid = "fixture-dfid",
                    token = "must-not-leak",
                    userId = "99",
                    cookies = KugouCookies.from(mapOf("private" to "must-not-leak")),
                )

            client.sendMobileCode("13800000000", authenticatedContext)
            val login = client.loginWithMobileCode("13800000000", "246810", null, context)

            val captchaRequest = transport.requests.first()
            assertEquals("mid=fixture-mid", captchaRequest.headers["Cookie"])
            assertFalse("token" in captchaRequest.query)
            assertFalse("userid" in captchaRequest.query)
            assertFalse(captchaRequest.toString().contains("must-not-leak"))
            assertTrue(login is KugouMobileLoginResult.Authenticated)
        }

    @Test
    fun authClientKeepsHeaderOnlyPasswordRiskTyped() =
        runBlocking {
            val transport =
                QueueTransport(
                    response(
                        body = """{"status":1}""",
                        headers = mapOf("SSA-CODE" to listOf("fixture-event")),
                    ),
                )
            val client =
                KugouAuthClient(
                    executor = KugouCallExecutor(requestFactory, transport),
                    requestBuilder = requestBuilder,
                    mobileLoginDecoder = KugouMobileLoginDecoder(),
                )

            val result = client.loginWithPassword("fixture-account", "fixture-password", context)

            val challenge = (result as KugouPasswordLoginResult.RiskChallenge).challenge
            assertEquals("fixture-event", challenge.eventId)
            assertEquals(null, challenge.sid)
            assertFalse(result.toString().contains("fixture-password"))
        }

    private fun response(
        body: String,
        headers: Map<String, List<String>> = emptyMap(),
    ) = KugouTransportResult.Success(
        KugouRawResponse(
            statusCode = 200,
            headers = headers,
            body = body.encodeToByteArray(),
        ),
    )

    private class QueueTransport(
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
        const val FIXTURE_TIME_MS = 1_700_000_000_123L
        const val FIXTURE_TIME_SECONDS = 1_700_000_000L
        const val FIXTURE_KEY = "fixturekey123456"
    }
}
