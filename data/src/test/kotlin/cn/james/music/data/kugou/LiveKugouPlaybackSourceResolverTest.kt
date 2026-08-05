package cn.james.music.data.kugou

import cn.james.music.kugou.api.endpoint.KugouAudioResource
import cn.james.music.kugou.api.endpoint.KugouEndpoints
import cn.james.music.kugou.api.endpoint.KugouPlaybackAddressDecoder
import cn.james.music.kugou.api.endpoint.KugouPrivilegeDecodeResult
import cn.james.music.kugou.api.endpoint.KugouPrivilegeDecoder
import cn.james.music.kugou.api.endpoint.KugouSongSearchDecodeResult
import cn.james.music.kugou.api.endpoint.KugouSongSearchDecoder
import cn.james.music.kugou.api.session.KugouAnonymousSessionInitializer
import cn.james.music.kugou.api.session.KugouDeviceIdentityFactory
import cn.james.music.kugou.api.session.KugouDeviceProfile
import cn.james.music.kugou.api.session.KugouDeviceProfileProvider
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.session.KugouSessionStore
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouProtocolResult
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.OkHttpKugouTransport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class LiveKugouPlaybackSourceResolverTest {
    @Test
    fun currentServiceReturnsPrivilegeShapeAndSecurePlaybackAddress() =
        runBlocking {
            assumeTrue("Live Kugou tests are opt-in", System.getenv(LIVE_TEST_ENV) == "true")
            val requestFactory = KugouRequestFactory()
            val transport = OkHttpKugouTransport()
            val sessionProvider =
                KugouAnonymousSessionInitializer(
                    store = MemorySessionStore(),
                    identityFactory = KugouDeviceIdentityFactory(),
                    profileProvider = KugouDeviceProfileProvider { LIVE_DEVICE_PROFILE },
                    requestFactory = requestFactory,
                    transport = transport,
                )
            val initialization = sessionProvider.initialize()
            assertTrue("Current service rejected anonymous registration", initialization is KugouInitializationResult.Ready)
            val session = (initialization as KugouInitializationResult.Ready).session
            val executor = KugouCallExecutor(requestFactory, transport)
            val search = executor.executeJson(KugouEndpoints.searchSongsAnonymous(LIVE_SEARCH_KEYWORD, 1, 5), session.requestContext())
            assertTrue("Current service rejected playback fixture search", search is KugouProtocolResult.Success)
            val searchResult = KugouSongSearchDecoder().decode((search as KugouProtocolResult.Success).body)
            assertTrue("Current service returned no usable playback fixture", searchResult is KugouSongSearchDecodeResult.Success)
            val song = (searchResult as KugouSongSearchDecodeResult.Success).page.items.first()

            val privilege =
                executor.executeJson(
                    KugouEndpoints.privilegeLite(listOf(KugouAudioResource(song.hash))),
                    session.requestContext(),
                )
            assertTrue("Current service rejected privilege request", privilege is KugouProtocolResult.Success)
            val privilegeResult = KugouPrivilegeDecoder().decode((privilege as KugouProtocolResult.Success).body)
            assertTrue("Current privilege response shape is unsupported", privilegeResult is KugouPrivilegeDecodeResult.Success)

            val address =
                KugouPlaybackSourceResolver(
                    sessionProvider = sessionProvider,
                    executor = executor,
                    decoder = KugouPlaybackAddressDecoder(),
                ).resolve(song.hash)

            assertTrue("Current service returned no secure playable address", address?.startsWith("https://") == true)
        }

    private class MemorySessionStore : KugouSessionStore {
        private var snapshot: KugouSessionSnapshot? = null

        override suspend fun read(): KugouSessionSnapshot? = snapshot

        override suspend fun write(snapshot: KugouSessionSnapshot) {
            this.snapshot = snapshot
        }

        override suspend fun clear() {
            snapshot = null
        }
    }

    private companion object {
        const val LIVE_TEST_ENV = "MOEKOE_RUN_LIVE_KUGOU_TESTS"
        const val LIVE_SEARCH_KEYWORD = "Linkin Park"
        val LIVE_DEVICE_PROFILE =
            KugouDeviceProfile(
                availableRamBytes = 4_000_000_000,
                availableInternalBytes = 32_000_000_000,
                availableExternalBytes = 0,
                brand = "MoeKoe",
                buildSerial = "unknown",
                device = "android",
                manufacturer = "MoeKoe",
            )
    }
}
