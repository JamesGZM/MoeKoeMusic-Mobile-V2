package cn.james.music.data.kugou

import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouInitializationError
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouRetryDelayer
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KugouPlaybackSourceResolverTest {
    @Test
    fun resolvedAddressUsesSignedSongUrlRequestAndFirstSecureCandidate() =
        runBlocking {
            val transport = RecordingTransport(success("""{"status":1,"url":["https://cdn.example/audio.mp3"]}"""))
            val resolver = resolver(readyProvider(), transport)

            val address = resolver.resolve(" ABCDEF ")

            assertEquals("https://cdn.example/audio.mp3", address)
            val request = transport.requests.single()
            assertEquals("song_url", request.id)
            assertEquals("abcdef", request.query["hash"])
            assertEquals("1", request.query["IsFreePart"])
            assertEquals("trackercdn.kugou.com", request.headers["x-router"])
        }

    @Test
    fun initializationFailureAndUnavailableResponseReturnNull() =
        runBlocking {
            val transport = RecordingTransport(success("""{"status":1,"url":[]}"""))
            val failed =
                resolver(
                    object : KugouSessionProvider {
                        override suspend fun initialize() = KugouInitializationResult.Failure(KugouInitializationError.StorageRead)
                    },
                    transport,
                )

            assertNull(failed.resolve("hash"))
            assertEquals(0, transport.requests.size)
            assertNull(resolver(readyProvider(), transport).resolve("hash"))
        }

    @Test
    fun blankHashDoesNotInitializeOrCallTransport() =
        runBlocking {
            var initialized = false
            val transport = RecordingTransport(success("""{"status":1,"url":[]}"""))
            val resolver =
                resolver(
                    object : KugouSessionProvider {
                        override suspend fun initialize(): KugouInitializationResult {
                            initialized = true
                            return readyProvider().initialize()
                        }
                    },
                    transport,
                )

            assertNull(resolver.resolve("   "))
            assertEquals(false, initialized)
            assertEquals(0, transport.requests.size)
        }

    private fun resolver(
        provider: KugouSessionProvider,
        transport: KugouTransport,
    ) = KugouPlaybackSourceResolver(
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
                    KugouSessionSnapshot(KugouDeviceIdentity("fixture-guid", "fixture-mid"), dfid = "fixture-dfid"),
                )
        }

    private fun success(body: String) = KugouTransportResult.Success(KugouRawResponse(200, emptyMap(), body.encodeToByteArray()))

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
