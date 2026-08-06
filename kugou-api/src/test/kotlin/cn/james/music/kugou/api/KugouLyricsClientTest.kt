package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouLyricsFetchResult
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException

class KugouLyricsClientTest {
    @Test
    fun fetchLyricsSearchesThenDownloadsWithoutExposingCandidateCredentials() =
        runBlocking {
            val transport =
                QueueTransport(
                    jsonResponse("""{"status":200,"candidates":[{"id":42,"accesskey":"fixture-key"}]}"""),
                    jsonResponse("""{"status":200,"contenttype":0,"content":"$NODE_KRC_VECTOR"}"""),
                )
            val result = client(transport).fetchLyrics("ABCDEF")

            assertTrue(result is KugouApiResult.Success)
            val lyrics = (result as KugouApiResult.Success).value as KugouLyricsFetchResult.Available
            assertEquals(FIXTURE_KRC_TEXT, lyrics.source.krcText)
            assertEquals(listOf("/search", "/download"), transport.requests.map { it.path })
            assertEquals("ABCDEF", transport.requests.first().query["hash"])
            transport.requests.forEach { request ->
                assertTrue("mid" !in request.query)
                assertTrue("dfid" !in request.query)
                assertTrue("Cookie" !in request.headers)
            }
            assertFalseSensitiveDataInResult(result)
        }

    @Test
    fun noCandidateStopsBeforeDownload() =
        runBlocking {
            val transport = QueueTransport(jsonResponse("""{"status":200,"candidates":[]}"""))
            val result = client(transport).fetchLyrics("ABCDEF")

            assertEquals(KugouLyricsFetchResult.NotFound, (result as KugouApiResult.Success).value)
            assertEquals(listOf("/search"), transport.requests.map { it.path })
        }

    @Test
    fun cancellationIsNotConvertedToProtocolFailure() {
        val transport = KugouTransport { throw CancellationException("fixture cancellation") }

        assertThrows(CancellationException::class.java) {
            runBlocking { client(transport).fetchLyrics("ABCDEF") }
        }
    }

    private fun client(transport: KugouTransport) =
        KugouOnlineClient(
            KugouCallExecutor(
                requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L }),
                transport = transport,
            ),
        )

    private fun jsonResponse(body: String) =
        KugouTransportResult.Success(
            KugouRawResponse(statusCode = 200, headers = emptyMap(), body = body.encodeToByteArray()),
        )

    private fun assertFalseSensitiveDataInResult(result: Any) {
        val diagnostic = result.toString()
        assertTrue("fixture-key" !in diagnostic)
        assertTrue("42" !in diagnostic)
        assertTrue(FIXTURE_KRC_TEXT !in diagnostic)
    }

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
        const val FIXTURE_KRC_TEXT = "[0,1200]<0,600,0>Moe<600,600,0>Koe\\n[language:W10=]"
        const val NODE_KRC_VECTOR = "a3JjMTjb6kGOA0B1Yb6EHB7jXVmQdtGEk33BRuAWDcIyBvbVqNuly6rgsLMFnUFuzQk2aQpfb3Q="
    }
}
