package cn.james.music.kugou.api

import cn.james.music.kugou.api.crypto.KugouAuthCrypto
import cn.james.music.kugou.api.endpoint.EpochMillisProvider
import cn.james.music.kugou.api.endpoint.KugouAuthClient
import cn.james.music.kugou.api.endpoint.KugouAuthRequestBuilder
import cn.james.music.kugou.api.endpoint.KugouAuthTemporaryKeyProvider
import cn.james.music.kugou.api.endpoint.KugouMobileLoginDecoder
import cn.james.music.kugou.api.endpoint.KugouMobileLoginResult
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

    private fun response(body: String) =
        KugouTransportResult.Success(
            KugouRawResponse(
                statusCode = 200,
                headers = emptyMap(),
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
