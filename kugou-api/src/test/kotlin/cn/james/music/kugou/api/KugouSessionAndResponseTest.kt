package cn.james.music.kugou.api

import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouProtocolResult
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouResponseDecoder
import cn.james.music.kugou.api.transport.KugouServiceErrorPolicy
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouSessionAndResponseTest {
    private val decoder = KugouResponseDecoder()

    @Test
    fun cookieMergePreservesEqualsAndRemovesEmptyValues() {
        val cookies =
            KugouCookies
                .from(mapOf("token" to "old", "keep" to "yes"))
                .mergeSetCookie(
                    listOf(
                        "token=new==; Path=/; HttpOnly",
                        "keep=; Max-Age=0",
                        "dfid=fixture-dfid; Domain=.kugou.com",
                        "bad\r\nInjected=value",
                        "invalid",
                    ),
                )

        assertEquals("new==", cookies.value("token"))
        assertEquals("fixture-dfid", cookies.value("dfid"))
        assertFalse("keep" in cookies.asMap())
        assertFalse("bad\r\nInjected" in cookies.asMap())
        assertFalse(cookies.toString().contains("new=="))
    }

    @Test
    fun cookieHeaderInjectionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            KugouCookies.from(mapOf("token" to "value\r\nInjected: true"))
        }
    }

    @Test
    fun successfulJsonReturnsOnlyResponseCookiePatch() {
        val result =
            decoder.decode(
                response(
                    body = """{"status":1,"data":{"items":[]}}""",
                    headers = mapOf("Set-Cookie" to listOf("dfid=fixture-dfid; Path=/")),
                ),
            )

        assertTrue(result is KugouProtocolResult.Success)
        assertEquals("fixture-dfid", (result as KugouProtocolResult.Success).responseCookies.value("dfid"))
    }

    @Test
    fun responseFailuresAreTypedAndDoNotExposeBody() {
        assertEquals(
            KugouError.Http(503),
            (decoder.decode(response(status = 503, body = "upstream detail")) as KugouProtocolResult.Failure).error,
        )
        assertEquals(
            KugouError.Risk("verify"),
            (
                decoder.decode(
                    response(body = """{"status":1}""", headers = mapOf("SSA-CODE" to listOf("verify"))),
                ) as KugouProtocolResult.Failure
            ).error,
        )
        assertEquals(
            KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
            (decoder.decode(response(body = "not-json")) as KugouProtocolResult.Failure).error,
        )
        assertEquals(
            KugouError.Protocol(KugouError.Protocol.Reason.ServiceRejected, serviceCode = "20014"),
            (
                decoder.decode(response(body = """{"status":0,"error_code":20014,"message":"sensitive"}"""))
                    as KugouProtocolResult.Failure
            ).error,
        )
    }

    @Test
    fun endpointDecoderCanInspectDeclaredServiceFailure() {
        val result =
            decoder.decode(
                response(
                    body = """{"status":0,"error_code":20028,"ssaCode":"fixture-event","data":{"info_list":[]}}""",
                    headers = mapOf("Set-Cookie" to listOf("fixture=value; Path=/")),
                ),
                KugouServiceErrorPolicy.DecodeByEndpoint,
            )

        assertTrue(result is KugouProtocolResult.Success)
        result as KugouProtocolResult.Success
        assertEquals(
            "fixture-event",
            result.body.jsonObject
                .getValue("ssaCode")
                .jsonPrimitive.content,
        )
        assertEquals("value", result.responseCookies.value("fixture"))
    }

    @Test
    fun fakeTransportCapturesPreparedRequestWithoutNetwork() =
        runBlocking {
            val fake = RecordingTransport(response(body = """{"status":1}"""))
            val request = fixtureRequest()

            val result = fake.execute(request)

            assertTrue(result is KugouTransportResult.Success)
            assertEquals(listOf(request), fake.requests)
        }

    private fun response(
        status: Int = 200,
        body: String,
        headers: Map<String, List<String>> = emptyMap(),
    ) = KugouRawResponse(statusCode = status, headers = headers, body = body.encodeToByteArray())

    private fun fixtureRequest() =
        KugouPreparedRequest(
            id = "fixture",
            method = cn.james.music.kugou.api.transport.KugouHttpMethod.Get,
            baseUrl = "https://example.test",
            path = "/fixture",
            query = emptyMap(),
            headers = emptyMap(),
            body = null,
            responseFormat = cn.james.music.kugou.api.transport.KugouResponseFormat.Json,
        )

    private class RecordingTransport(
        private val response: KugouRawResponse,
    ) : KugouTransport {
        val requests = mutableListOf<KugouPreparedRequest>()

        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult {
            requests += request
            return KugouTransportResult.Success(response)
        }
    }
}
