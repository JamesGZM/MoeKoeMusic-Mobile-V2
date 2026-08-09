package cn.james.music.data.kugou

import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsSnapshot
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import cn.james.music.core.model.settings.LyricsTextSizePreference
import cn.james.music.core.model.settings.PlaybackQualityPreference
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouAudioResource
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.session.KugouAnonymousSessionInitializer
import cn.james.music.kugou.api.session.KugouDeviceIdentityFactory
import cn.james.music.kugou.api.session.KugouDeviceProfile
import cn.james.music.kugou.api.session.KugouDeviceProfileProvider
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.session.KugouSessionStore
import cn.james.music.kugou.api.transport.KtorKugouTransport
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.playback.RemotePlaybackSourceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
            val transport = KtorKugouTransport()
            val sessionProvider =
                KugouAnonymousSessionInitializer(
                    store = MemorySessionStore(),
                    identityFactory = KugouDeviceIdentityFactory(),
                    profileProvider = KugouDeviceProfileProvider { LIVE_DEVICE_PROFILE },
                    requestFactory = requestFactory,
                    transport = transport,
                )
            val initialization = sessionProvider.initialize()
            assertTrue(
                "Current service rejected anonymous registration: $initialization",
                initialization is KugouInitializationResult.Ready,
            )
            val session = (initialization as KugouInitializationResult.Ready).session
            val executor = KugouCallExecutor(requestFactory, transport)
            val onlineClient = KugouOnlineClient(executor)
            val search = onlineClient.searchSongs(LIVE_SEARCH_KEYWORD, 1, 5, session.requestContext())
            assertTrue("Current service rejected playback fixture search", search is KugouApiResult.Success)
            val song = (search as KugouApiResult.Success).value.items.first()

            val privilege =
                onlineClient.fetchPrivilegeCandidates(
                    listOf(KugouAudioResource(song.hash)),
                    session.requestContext(),
                )
            assertTrue("Current privilege response shape is unsupported", privilege is KugouApiResult.Success)

            val address =
                KugouPlaybackSourceResolver(
                    sessionProvider = sessionProvider,
                    onlineClient = onlineClient,
                    appSettingsRepository = defaultSettingsRepository(),
                ).resolve(song.hash)

            assertTrue(
                "Current service returned no secure playable address",
                address is RemotePlaybackSourceResult.Resolved && address.url.startsWith("https://"),
            )
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

    private fun defaultSettingsRepository() =
        object : AppSettingsRepository {
            override val settings: Flow<AppSettingsSnapshot> = flowOf(AppSettingsSnapshot())

            override suspend fun setTheme(theme: AppThemePreference) = AppSettingsUpdateResult.Success

            override suspend fun setPlaybackQuality(quality: PlaybackQualityPreference) = AppSettingsUpdateResult.Success

            override suspend fun setAutoSkipFailedPlayback(enabled: Boolean) = AppSettingsUpdateResult.Success

            override suspend fun setDynamicCoverColors(enabled: Boolean) = AppSettingsUpdateResult.Success

            override suspend fun setShowLyricsSupplementalText(enabled: Boolean) = AppSettingsUpdateResult.Success

            override suspend fun setLyricsTextSize(size: LyricsTextSizePreference) = AppSettingsUpdateResult.Success
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
