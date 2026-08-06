package cn.james.music.kugou.api

import cn.james.music.kugou.api.transport.KtorKugouTransport
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouResponseFormat
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class KtorKugouTransportTest {
    @Test
    fun getRequestIsEncodedAndRawResponseIsPreserved() =
        runBlocking {
            val interceptor =
                CapturingInterceptor { request ->
                    response(
                        request,
                        body = "fixture",
                        headers =
                            mapOf("Set-Cookie" to "dfid=test"),
                    )
                }
            val transport = KtorKugouTransport(OkHttpClient.Builder().addInterceptor(interceptor).build())

            val result =
                transport.execute(
                    request(
                        path = "/risk/v2/r_register_dev",
                        query = mapOf("keyword" to "MoeKoe 测试", "signature" to "fixture-signature"),
                        headers = mapOf("x-router" to "complexsearch.kugou.com"),
                    ),
                ) as KugouTransportResult.Success

            val captured = interceptor.requests.single()
            assertEquals("/risk/v2/r_register_dev", captured.url.encodedPath)
            assertEquals("MoeKoe 测试", captured.url.queryParameter("keyword"))
            assertEquals("fixture-signature", captured.url.queryParameter("signature"))
            assertEquals("complexsearch.kugou.com", captured.header("x-router"))
            assertEquals(200, result.response.statusCode)
            assertEquals("fixture", result.response.body.decodeToString())
            assertEquals(
                listOf("dfid=test"),
                result.response.headers.entries
                    .first { (name) -> name.equals("Set-Cookie", ignoreCase = true) }
                    .value,
            )
        }

    @Test
    fun postRequestUsesExactBodyBytesAndContentType() =
        runBlocking {
            val interceptor = CapturingInterceptor(::response)
            val transport = KtorKugouTransport(OkHttpClient.Builder().addInterceptor(interceptor).build())
            val body = byteArrayOf(0, 1, 2, 127, -1)

            transport.execute(
                request(
                    method = KugouHttpMethod.Post,
                    headers = mapOf("Content-Type" to "application/octet-stream"),
                    body = body,
                ),
            )

            val captured = interceptor.requests.single()
            val buffer = Buffer()
            captured.body?.writeTo(buffer)
            assertTrue(body.contentEquals(buffer.readByteArray()))
            assertEquals("application/octet-stream", captured.body?.contentType().toString())
        }

    @Test
    fun preparedRequestProtocolIsPreserved() =
        runBlocking {
            val interceptor = CapturingInterceptor(::response)
            val transport = KtorKugouTransport(OkHttpClient.Builder().addInterceptor(interceptor).build())

            transport.execute(request(baseUrl = "http://login.user.kugou.com"))

            assertEquals("http", interceptor.requests.single().url.scheme)
            assertEquals("login.user.kugou.com", interceptor.requests.single().url.host)
        }

    @Test
    fun ioFailuresMapToTypedNetworkErrors() =
        runBlocking {
            val timeout = transportThrowing(SocketTimeoutException("fixture timeout")).execute(request())
            val offline = transportThrowing(UnknownHostException("fixture host")).execute(request())

            assertEquals(
                KugouError.Network(KugouError.Network.Kind.Timeout),
                (timeout as KugouTransportResult.Failure).error,
            )
            assertEquals(
                KugouError.Network(KugouError.Network.Kind.Offline),
                (offline as KugouTransportResult.Failure).error,
            )
        }

    private fun transportThrowing(error: java.io.IOException): KtorKugouTransport =
        KtorKugouTransport(
            OkHttpClient
                .Builder()
                .addInterceptor { throw error }
                .build(),
        )

    private fun request(
        method: KugouHttpMethod = KugouHttpMethod.Get,
        baseUrl: String = "https://example.test",
        path: String = "/fixture",
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null,
    ) = KugouPreparedRequest(
        id = "fixture",
        method = method,
        baseUrl = baseUrl,
        path = path,
        query = query,
        headers = headers,
        body = body,
        responseFormat = KugouResponseFormat.Json,
    )

    private class CapturingInterceptor(
        private val handler: (Request) -> Response,
    ) : Interceptor {
        val requests = mutableListOf<Request>()

        override fun intercept(chain: Interceptor.Chain): Response = handler(chain.request().also(requests::add))
    }

    private companion object {
        fun response(
            request: Request,
            body: String = "{}",
            headers: Map<String, String> = emptyMap(),
        ): Response =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody())
                .apply { headers.forEach(::header) }
                .build()
    }
}
