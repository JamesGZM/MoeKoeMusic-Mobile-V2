package cn.james.music.data.home

import cn.james.music.core.database.home.HomeContentSnapshotDao
import cn.james.music.core.database.home.HomeContentSnapshotEntity
import cn.james.music.core.model.home.HomeAutomaticRefreshState
import cn.james.music.core.model.home.HomeBanner
import cn.james.music.core.model.home.HomeContent
import cn.james.music.core.model.home.HomePlaylist
import cn.james.music.core.model.home.HomeRecommendation
import cn.james.music.core.model.home.HomeRefreshProblem
import cn.james.music.core.model.home.HomeRefreshResult
import cn.james.music.core.model.online.Song
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouHomeBannerDto
import cn.james.music.kugou.api.endpoint.KugouHomePlaylistDto
import cn.james.music.kugou.api.endpoint.KugouHomeService
import cn.james.music.kugou.api.endpoint.KugouHomeSongDto
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionObserver
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.session.KugouSessionState
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouRequestContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouHomeRepositoryTest {
    @Test
    fun freshCacheSkipsNetwork() =
        runBlocking {
            val dao = FakeDao().apply { put(CACHE_KEY_ANONYMOUS, content("cached", NOW)) }
            val service = FakeHomeService()

            val result = repository(dao, service).refresh()

            assertEquals(HomeRefreshResult.NotNeeded, result)
            assertEquals(0, service.dailyCalls + service.playlistCalls + service.bannerCalls)
        }

    @Test
    fun ttlBoundaryAndFutureTimestampControlAutomaticRefresh() =
        runBlocking {
            val stillFreshService = FakeHomeService()
            val stillFreshDao = FakeDao().apply { put(CACHE_KEY_ANONYMOUS, content("fresh-edge", NOW - CACHE_FRESH_MS + 1)) }
            assertEquals(HomeRefreshResult.NotNeeded, repository(stillFreshDao, stillFreshService).refresh())
            assertEquals(0, stillFreshService.dailyCalls)

            val expiredService = FakeHomeService()
            val expiredDao = FakeDao().apply { put(CACHE_KEY_ANONYMOUS, content("expired", NOW - CACHE_FRESH_MS)) }
            assertTrue(repository(expiredDao, expiredService).refresh() is HomeRefreshResult.Success)
            assertEquals(1, expiredService.dailyCalls)

            val futureService = FakeHomeService()
            val futureDao = FakeDao().apply { put(CACHE_KEY_ANONYMOUS, content("future", NOW + 1)) }
            assertTrue(repository(futureDao, futureService).refresh() is HomeRefreshResult.Success)
            assertEquals(1, futureService.dailyCalls)
        }

    @Test
    fun invalidSchemaPayloadVersionAndNegativeTimeAreDeleted() =
        runBlocking {
            val validPayload = HomeContentCacheCodec().encode(content("fixture", NOW))
            val invalidEntities =
                listOf(
                    HomeContentSnapshotEntity(CACHE_KEY_ANONYMOUS, validPayload, 2, NOW),
                    HomeContentSnapshotEntity(CACHE_KEY_ANONYMOUS, validPayload.replace("\"version\":1", "\"version\":2"), 1, NOW),
                    HomeContentSnapshotEntity(CACHE_KEY_ANONYMOUS, validPayload, 1, -1),
                )
            invalidEntities.forEach { invalid ->
                val dao = FakeDao().apply { upsert(invalid) }
                val service =
                    FakeHomeService(
                        daily = { KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse)) },
                        playlist = { KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse)) },
                    )

                assertTrue(repository(dao, service).refresh() is HomeRefreshResult.Failure)
                assertNull(dao.entities[CACHE_KEY_ANONYMOUS])
            }
        }

    @Test
    fun completeRefreshPersistsVersionedSnapshotAndObservationReadsIt() =
        runBlocking {
            val dao = FakeDao()
            val service = FakeHomeService()
            val repository = repository(dao, service)

            val result = repository.refresh(force = true)

            assertTrue(result is HomeRefreshResult.Success && result.persisted)
            assertEquals(HomeContentCacheCodec.CACHE_SCHEMA_VERSION, dao.entities[CACHE_KEY_ANONYMOUS]?.schemaVersion)
            assertEquals(
                "daily",
                repository
                    .observeContent()
                    .first()
                    ?.recommendations
                    ?.single()
                    ?.song
                    ?.title,
            )
            assertEquals(1, service.dailyCalls)
            assertEquals(1, service.playlistCalls)
            assertEquals(1, service.bannerCalls)
        }

    @Test
    fun observationEmitsStaleCacheBeforeFastAutomaticRefresh() =
        runBlocking {
            val dao = FakeDao().apply { put(CACHE_KEY_ANONYMOUS, content("stale", 1)) }
            val repository = repository(dao, FakeHomeService())

            val values = repository.observeContent().take(2).toList()

            assertEquals(
                listOf("stale", "daily"),
                values.map {
                    it
                        ?.recommendations
                        ?.single()
                        ?.song
                        ?.title
                },
            )
        }

    @Test
    fun automaticRefreshFailureIsObservableWithoutClearingStaleContent() =
        runBlocking {
            val dao = FakeDao().apply { put(CACHE_KEY_ANONYMOUS, content("stale", 1)) }
            val repository =
                repository(
                    dao,
                    FakeHomeService(
                        daily = { KugouApiResult.Failure(KugouError.Network(KugouError.Network.Kind.Offline)) },
                        playlist = { KugouApiResult.Failure(KugouError.Network(KugouError.Network.Kind.Offline)) },
                    ),
                )
            val failure =
                async {
                    repository
                        .observeAutomaticRefresh()
                        .filterIsInstance<HomeAutomaticRefreshState.Complete>()
                        .first()
                        .result
                }
            val staleObserved = CompletableDeferred<Unit>()
            val observation =
                launch {
                    repository.observeContent().collect { content ->
                        if (content
                                ?.recommendations
                                ?.single()
                                ?.song
                                ?.title == "stale"
                        ) {
                            staleObserved.complete(Unit)
                        }
                    }
                }

            staleObserved.await()
            assertEquals(HomeRefreshResult.Failure(HomeRefreshProblem.Offline), withTimeout(1_000) { failure.await() })
            assertEquals(
                "stale",
                dao
                    .decode(CACHE_KEY_ANONYMOUS)
                    ?.recommendations
                    ?.single()
                    ?.song
                    ?.title,
            )
            observation.cancelAndJoin()
        }

    @Test
    fun corruptCacheIsDeletedAndExposedAsMissing() =
        runBlocking {
            val dao = FakeDao()
            dao.upsert(HomeContentSnapshotEntity(CACHE_KEY_ANONYMOUS, "not-json", 1, NOW))
            val repository = repository(dao, FakeHomeService())

            assertNull(repository.observeContent().first())
            assertNull(dao.entities[CACHE_KEY_ANONYMOUS])
        }

    @Test
    fun requiredFailureKeepsOldSnapshotButReturnsPartialWithoutCache() =
        runBlocking {
            val failingService =
                FakeHomeService(
                    playlist = { KugouApiResult.Failure(KugouError.Network(KugouError.Network.Kind.Offline)) },
                )
            val cachedDao = FakeDao().apply { put(CACHE_KEY_ANONYMOUS, content("old", 1)) }

            val cachedResult = repository(cachedDao, failingService).refresh(force = true)

            assertEquals(HomeRefreshResult.Failure(HomeRefreshProblem.Offline), cachedResult)
            assertEquals(
                "old",
                cachedDao
                    .decode(CACHE_KEY_ANONYMOUS)
                    ?.recommendations
                    ?.single()
                    ?.song
                    ?.title,
            )

            val emptyDao = FakeDao()
            val partialResult = repository(emptyDao, failingService).refresh(force = true)
            assertTrue(partialResult is HomeRefreshResult.Partial)
            assertEquals(HomeRefreshProblem.Offline, (partialResult as HomeRefreshResult.Partial).problem)
            assertNull(emptyDao.entities[CACHE_KEY_ANONYMOUS])
        }

    @Test
    fun optionalBannerFailureStillPersistsCompleteRequiredContent() =
        runBlocking {
            val dao = FakeDao()
            val service =
                FakeHomeService(
                    banners = { KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.ServiceRejected)) },
                )

            val result = repository(dao, service).refresh(force = true)

            assertTrue(result is HomeRefreshResult.Success && result.persisted)
            assertTrue(dao.decode(CACHE_KEY_ANONYMOUS)?.banners?.isEmpty() == true)
        }

    @Test
    fun concurrentAutomaticRefreshesShareOneFlight() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val service =
                FakeHomeService(
                    daily = {
                        started.complete(Unit)
                        release.await()
                        KugouApiResult.Success(listOf(SONG_DTO))
                    },
                )
            val repository = repository(FakeDao(), service)

            val first = async { repository.refresh() }
            started.await()
            val second = async { repository.refresh() }
            release.complete(Unit)

            assertEquals(first.await(), second.await())
            assertEquals(1, service.dailyCalls)
            assertEquals(1, service.playlistCalls)
        }

    @Test
    fun forcedRefreshStartsNewGenerationAndOlderResultCannotOverwrite() =
        runBlocking {
            val firstStarted = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()
            var call = 0
            val service =
                FakeHomeService(
                    daily = {
                        call += 1
                        if (call == 1) {
                            firstStarted.complete(Unit)
                            releaseFirst.await()
                            KugouApiResult.Success(listOf(SONG_DTO.copy(title = "old")))
                        } else {
                            KugouApiResult.Success(listOf(SONG_DTO.copy(title = "new")))
                        }
                    },
                )
            val dao = FakeDao()
            val repository = repository(dao, service)

            val first = async { repository.refresh(force = true) }
            firstStarted.await()
            val second = async { repository.refresh(force = true) }
            assertTrue(second.await() is HomeRefreshResult.Success)
            releaseFirst.complete(Unit)

            assertEquals(HomeRefreshResult.Superseded, first.await())
            assertEquals(
                "new",
                dao
                    .decode(CACHE_KEY_ANONYMOUS)
                    ?.recommendations
                    ?.single()
                    ?.song
                    ?.title,
            )
        }

    @Test
    fun callerCancellationPropagatesAndDoesNotWritePartialSnapshot() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val never = CompletableDeferred<Unit>()
            val dao = FakeDao()
            val repository =
                repository(
                    dao,
                    FakeHomeService(
                        daily = {
                            started.complete(Unit)
                            never.await()
                            KugouApiResult.Success(listOf(SONG_DTO))
                        },
                    ),
                )

            val refresh = async { repository.refresh(force = true) }
            started.await()
            refresh.cancelAndJoin()

            assertTrue(refresh.isCancelled)
            assertNull(dao.entities[CACHE_KEY_ANONYMOUS])
        }

    @Test
    fun identityChangeDuringRefreshPreventsOldPartitionCommit() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val dao = FakeDao()
            val observer = FakeSessionObserver(ANONYMOUS_SESSION)
            val repository =
                repository(
                    dao,
                    FakeHomeService(
                        daily = {
                            started.complete(Unit)
                            release.await()
                            KugouApiResult.Success(listOf(SONG_DTO))
                        },
                    ),
                    observer,
                )

            val refresh = async { repository.refresh(force = true) }
            started.await()
            observer.publish(AUTHENTICATED_SESSION)
            observer.publish(ANONYMOUS_SESSION)
            release.complete(Unit)

            assertEquals(HomeRefreshResult.Superseded, refresh.await())
            assertNull(dao.entities[CACHE_KEY_ANONYMOUS])
        }

    @Test
    fun sessionIdentityChangesSwitchObservedCachePartition() =
        runBlocking {
            val dao =
                FakeDao().apply {
                    put(CACHE_KEY_ANONYMOUS, content("anonymous", NOW))
                    put(CACHE_KEY_USER, content("signed-in", NOW))
                }
            val observer = FakeSessionObserver(ANONYMOUS_SESSION)
            val repository = repository(dao, FakeHomeService(), observer)
            val anonymousObserved = CompletableDeferred<Unit>()

            val values =
                async {
                    repository
                        .observeContent()
                        .onEach { content ->
                            if (content
                                    ?.recommendations
                                    ?.single()
                                    ?.song
                                    ?.title == "anonymous"
                            ) {
                                anonymousObserved.complete(Unit)
                            }
                        }.take(2)
                        .toList()
                }
            anonymousObserved.await()
            observer.publish(AUTHENTICATED_SESSION)

            assertEquals(
                listOf("anonymous", "signed-in"),
                withTimeout(1_000) { values.await() }.map {
                    it
                        ?.recommendations
                        ?.single()
                        ?.song
                        ?.title
                },
            )
        }

    @Test
    fun contentObservationRecoversAfterLaterSessionInitialization() =
        runBlocking {
            val dao = FakeDao().apply { put(CACHE_KEY_ANONYMOUS, content("recovered", NOW)) }
            val observer = FakeSessionObserver(null)
            val repository = repository(dao, FakeHomeService(), observer)
            val unavailableObserved = CompletableDeferred<Unit>()
            val initializationFailure =
                async {
                    repository
                        .observeAutomaticRefresh()
                        .filterIsInstance<HomeAutomaticRefreshState.Complete>()
                        .first()
                        .result
                }

            val values =
                async {
                    repository
                        .observeContent()
                        .onEach { content -> if (content == null) unavailableObserved.complete(Unit) }
                        .take(2)
                        .toList()
                }
            unavailableObserved.await()
            assertEquals(
                HomeRefreshResult.Failure(HomeRefreshProblem.SessionInitialization),
                initializationFailure.await(),
            )
            observer.publish(ANONYMOUS_SESSION)

            assertEquals(
                listOf(null, "recovered"),
                withTimeout(1_000) { values.await() }.map {
                    it
                        ?.recommendations
                        ?.single()
                        ?.song
                        ?.title
                },
            )
        }

    private fun repository(
        dao: FakeDao,
        service: FakeHomeService,
        observer: FakeSessionObserver = FakeSessionObserver(ANONYMOUS_SESSION),
    ) = KugouHomeRepository(
        sessionProvider = FakeSessionProvider(observer),
        sessionObserver = observer,
        homeService = service,
        cacheDao = dao,
        timeProvider = HomeTimeProvider { NOW },
    )

    private class FakeSessionProvider(
        private val observer: FakeSessionObserver,
    ) : KugouSessionProvider {
        override suspend fun initialize(): KugouInitializationResult =
            observer.state.value.session
                ?.let(KugouInitializationResult::Ready)
                ?: KugouInitializationResult.Failure(cn.james.music.kugou.api.session.KugouInitializationError.StorageRead)
    }

    private class FakeSessionObserver(
        initial: KugouSessionSnapshot?,
    ) : KugouSessionObserver {
        private val mutableState = MutableStateFlow(KugouSessionState(initial, if (initial == null) 0 else 1))
        override val state: StateFlow<KugouSessionState> = mutableState

        fun publish(session: KugouSessionSnapshot?) {
            mutableState.value = KugouSessionState(session, mutableState.value.generation + 1)
        }
    }

    private class FakeDao : HomeContentSnapshotDao {
        val entities = mutableMapOf<String, HomeContentSnapshotEntity>()
        private val flows = mutableMapOf<String, MutableStateFlow<HomeContentSnapshotEntity?>>()

        override fun observe(cacheKey: String): Flow<HomeContentSnapshotEntity?> =
            flows.getOrPut(cacheKey) { MutableStateFlow(entities[cacheKey]) }

        override suspend fun find(cacheKey: String): HomeContentSnapshotEntity? = entities[cacheKey]

        override suspend fun upsert(entity: HomeContentSnapshotEntity) {
            entities[entity.cacheKey] = entity
            flows.getOrPut(entity.cacheKey) { MutableStateFlow(null) }.value = entity
        }

        override suspend fun delete(cacheKey: String) {
            entities.remove(cacheKey)
            flows.getOrPut(cacheKey) { MutableStateFlow(null) }.value = null
        }

        suspend fun put(
            cacheKey: String,
            content: HomeContent,
        ) {
            upsert(
                HomeContentSnapshotEntity(
                    cacheKey,
                    HomeContentCacheCodec().encode(content),
                    HomeContentCacheCodec.CACHE_SCHEMA_VERSION,
                    content.updatedAtEpochMs,
                ),
            )
        }

        fun decode(cacheKey: String): HomeContent? =
            entities[cacheKey]?.let { entity -> HomeContentCacheCodec().decode(entity.payloadJson, entity.updatedAtEpochMs) }
    }

    private class FakeHomeService(
        private val banners: suspend () -> KugouApiResult<List<KugouHomeBannerDto>> = {
            KugouApiResult.Success(listOf(BANNER_DTO))
        },
        private val daily: suspend () -> KugouApiResult<List<KugouHomeSongDto>> = {
            KugouApiResult.Success(listOf(SONG_DTO))
        },
        private val playlist: suspend () -> KugouApiResult<List<KugouHomePlaylistDto>> = {
            KugouApiResult.Success(listOf(PLAYLIST_DTO))
        },
    ) : KugouHomeService {
        var bannerCalls = 0
        var dailyCalls = 0
        var playlistCalls = 0

        override suspend fun fetchHomeBanners(context: KugouRequestContext): KugouApiResult<List<KugouHomeBannerDto>> {
            bannerCalls += 1
            return banners()
        }

        override suspend fun fetchDailyRecommendations(context: KugouRequestContext): KugouApiResult<List<KugouHomeSongDto>> {
            dailyCalls += 1
            return daily()
        }

        override suspend fun fetchTopPlaylists(
            context: KugouRequestContext,
            page: Int,
            pageSize: Int,
        ): KugouApiResult<List<KugouHomePlaylistDto>> {
            playlistCalls += 1
            return playlist()
        }
    }

    private fun content(
        title: String,
        updatedAtEpochMs: Long,
    ) = HomeContent(
        banners = listOf(HomeBanner("banner", null, "https://example.test/banner")),
        recommendations =
            listOf(
                HomeRecommendation(
                    Song("song", "hash", title, "artist", null, null, 1_000, null),
                    null,
                ),
            ),
        playlists = listOf(HomePlaylist("playlist", "playlist", null, null)),
        updatedAtEpochMs = updatedAtEpochMs,
    )

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val CACHE_FRESH_MS = 15 * 60 * 1_000L
        const val CACHE_KEY_ANONYMOUS = "home:v1:anonymous"
        const val CACHE_KEY_USER = "home:v1:user:42"
        val IDENTITY = KugouDeviceIdentity("fixture-guid", "fixture-mid")
        val ANONYMOUS_SESSION = KugouSessionSnapshot(IDENTITY, dfid = "fixture-dfid")
        val AUTHENTICATED_SESSION = ANONYMOUS_SESSION.copy(token = "fixture-token", userId = "42")
        val BANNER_DTO = KugouHomeBannerDto("banner", null, "http://example.test/banner")
        val SONG_DTO = KugouHomeSongDto("hash", "daily", "artist", null, null, 1_000, null, null, null)
        val PLAYLIST_DTO = KugouHomePlaylistDto("playlist", "playlist", null, 10)
    }
}
