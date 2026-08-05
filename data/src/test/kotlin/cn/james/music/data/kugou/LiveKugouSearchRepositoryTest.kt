package cn.james.music.data.kugou

import cn.james.music.core.model.online.SearchResult
import cn.james.music.kugou.api.endpoint.KugouSongSearchDecoder
import cn.james.music.kugou.api.session.KugouAnonymousSessionInitializer
import cn.james.music.kugou.api.session.KugouDeviceIdentityFactory
import cn.james.music.kugou.api.session.KugouDeviceProfile
import cn.james.music.kugou.api.session.KugouDeviceProfileProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.session.KugouSessionStore
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.OkHttpKugouTransport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class LiveKugouSearchRepositoryTest {
    @Test
    fun anonymousRegistrationSearchDecodeAndDomainMappingWorkAgainstCurrentService() =
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
            val repository =
                KugouSearchRepository(
                    sessionProvider = sessionProvider,
                    executor = KugouCallExecutor(requestFactory, transport),
                    decoder = KugouSongSearchDecoder(),
                )

            val result = repository.searchSongs(LIVE_SEARCH_KEYWORD, page = 1, pageSize = 5)

            assertTrue(
                "Current service did not complete the search domain flow: ${(result as? SearchResult.Failure)?.error}",
                result is SearchResult.Success,
            )
            assertTrue("Current service returned no mapped songs", (result as SearchResult.Success).page.items.isNotEmpty())
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
        const val LIVE_SEARCH_KEYWORD = "周杰伦"
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
