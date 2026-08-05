package cn.james.music.data.kugou

import cn.james.music.core.model.online.SearchError
import cn.james.music.core.model.online.SearchResult
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouInitializationError
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouRetryDelayer
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouSearchRepositoryTest {
    @Test
    fun successfulResponseMapsToStableDomainPage() =
        runBlocking {
            val transport = RecordingTransport(successResponse())
            val repository = repository(readyProvider(), transport)

            val result = repository.searchSongs(keyword = "  query  ", page = 2, pageSize = 10)

            assertTrue(result is SearchResult.Success)
            val page = (result as SearchResult.Success).page
            assertEquals(2, page.page)
            assertEquals(21, page.totalCount)
            assertTrue(page.hasMore)
            assertEquals(123_000L, page.items.single().durationMs)
            assertEquals("https://img/300/cover.jpg", page.items.single().artworkUrl)
            assertEquals("query", transport.requests.single().query["keyword"])
        }

    @Test
    fun initializationFailureDoesNotCallTheService() =
        runBlocking {
            val transport = RecordingTransport(successResponse())
            val repository =
                repository(
                    object : KugouSessionProvider {
                        override suspend fun initialize() = KugouInitializationResult.Failure(KugouInitializationError.StorageRead)
                    },
                    transport,
                )

            val result = repository.searchSongs("query", page = 1)

            assertEquals(SearchResult.Failure(SearchError.SessionInitialization), result)
            assertTrue(transport.requests.isEmpty())
        }

    @Test
    fun authenticationAndTimeoutFailuresMapToDomainErrors() =
        runBlocking {
            val authentication =
                repository(
                    readyProvider(),
                    RecordingTransport(rawResponse("""{"status":0,"error_code":152}""")),
                ).searchSongs("query", page = 1)
            val timeoutTransport =
                RecordingTransport(
                    KugouTransportResult.Failure(KugouError.Network(KugouError.Network.Kind.Timeout)),
                )
            val timeout = repository(readyProvider(), timeoutTransport).searchSongs("query", page = 1)

            assertEquals(SearchResult.Failure(SearchError.AuthenticationRequired), authentication)
            assertEquals(SearchResult.Failure(SearchError.Timeout), timeout)
            assertEquals(3, timeoutTransport.requests.size)
        }

    @Test
    fun lastPageDoesNotReportMoreResults() =
        runBlocking {
            val result =
                repository(
                    readyProvider(),
                    RecordingTransport(successResponse(total = 20)),
                ).searchSongs("query", page = 2, pageSize = 10)

            assertFalse((result as SearchResult.Success).page.hasMore)
        }

    private fun repository(
        provider: KugouSessionProvider,
        transport: KugouTransport,
    ) = KugouSearchRepository(
        sessionProvider = provider,
        onlineClient =
            KugouOnlineClient(
                KugouCallExecutor(
                    requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L }),
                    transport = transport,
                    retryDelayer = KugouRetryDelayer {},
                ),
            ),
    )

    private fun readyProvider() =
        object : KugouSessionProvider {
            override suspend fun initialize() =
                KugouInitializationResult.Ready(
                    KugouSessionSnapshot(KugouDeviceIdentity(guid = "fixture-guid", mid = "fixture-mid")),
                )
        }

    private fun successResponse(total: Int = 21) =
        rawResponse(
            """
            {
              "status": 1,
              "error_code": 0,
              "data": {
                "total": $total,
                "lists": [{
                  "MixSongID": "mix-1",
                  "FileHash": "ABC",
                  "SongName": "Title",
                  "SingerName": "Artist",
                  "Duration": 123,
                  "Image": "http://img/{size}/cover.jpg"
                }]
              }
            }
            """.trimIndent(),
        )

    private fun rawResponse(body: String) =
        KugouTransportResult.Success(
            KugouRawResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = body.encodeToByteArray(),
            ),
        )

    private class RecordingTransport(
        private val result: KugouTransportResult,
    ) : KugouTransport {
        val requests = mutableListOf<KugouPreparedRequest>()

        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult {
            requests += request
            return result
        }
    }
}
