package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.KugouEndpoints
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class LiveKugouIntegrationTest {
    @Test
    fun anonymousRegistrationThenSongSearchIsAcceptedByCurrentService() =
        runBlocking {
            assumeTrue("Live Kugou tests are opt-in", System.getenv(LIVE_TEST_ENV) == "true")
            val requestFactory = KugouRequestFactory()
            val transport = OkHttpKugouTransport()
            val initialization =
                KugouAnonymousSessionInitializer(
                    store = MemorySessionStore(),
                    identityFactory = KugouDeviceIdentityFactory(),
                    profileProvider = KugouDeviceProfileProvider { LIVE_DEVICE_PROFILE },
                    requestFactory = requestFactory,
                    transport = transport,
                ).initialize()

            assertTrue(
                "Current service rejected anonymous device registration: $initialization",
                initialization is KugouInitializationResult.Ready,
            )
            val session = (initialization as KugouInitializationResult.Ready).session
            val executor =
                KugouCallExecutor(
                    requestFactory = requestFactory,
                    transport = transport,
                )

            val result =
                executor.executeJson(
                    KugouEndpoints.searchSongsAnonymous(LIVE_SEARCH_KEYWORD, page = 1),
                    session.requestContext(),
                )

            assertTrue("Current service rejected anonymous song search: $result", result is KugouProtocolResult.Success)
            val body = (result as KugouProtocolResult.Success).body.jsonObject
            val items = body["data"]?.jsonObject?.get("lists")?.jsonArray
            assertTrue("Current service returned no song list", !items.isNullOrEmpty())
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
