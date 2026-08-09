package cn.james.music.data.home

import cn.james.music.core.database.MoeKoeDatabase
import cn.james.music.core.database.home.HomeContentSnapshotDao
import cn.james.music.core.database.home.HomeContentSnapshotEntity
import cn.james.music.core.model.home.HomeAutomaticRefreshState
import cn.james.music.core.model.home.HomeBanner
import cn.james.music.core.model.home.HomeContent
import cn.james.music.core.model.home.HomePlaylist
import cn.james.music.core.model.home.HomeRecommendation
import cn.james.music.core.model.home.HomeRefreshProblem
import cn.james.music.core.model.home.HomeRefreshResult
import cn.james.music.core.model.home.HomeRepository
import cn.james.music.core.model.online.Song
import cn.james.music.data.cache.RegenerableContentCache
import cn.james.music.data.cache.RegenerableContentCacheGeneration
import cn.james.music.data.cache.RegenerableContentCacheWriteResult
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouHomeBannerDto
import cn.james.music.kugou.api.endpoint.KugouHomePlaylistDto
import cn.james.music.kugou.api.endpoint.KugouHomeService
import cn.james.music.kugou.api.endpoint.KugouHomeSongDto
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionObserver
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.KugouError
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

fun interface HomeTimeProvider {
    fun nowEpochMs(): Long
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class KugouHomeRepository
    @Inject
    constructor(
        private val sessionProvider: KugouSessionProvider,
        private val sessionObserver: KugouSessionObserver,
        private val homeService: KugouHomeService,
        private val cacheDao: HomeContentSnapshotDao,
        private val timeProvider: HomeTimeProvider,
        private val regenerableContentCache: RegenerableContentCache,
    ) : HomeRepository {
        private val codec = HomeContentCacheCodec()
        private val flightMutex = Mutex()
        private val generations = mutableMapOf<String, Long>()
        private val flights = mutableMapOf<String, RefreshFlight>()
        private val automaticRefresh = MutableStateFlow<HomeAutomaticRefreshState>(HomeAutomaticRefreshState.Idle)

        override fun observeContent(): Flow<HomeContent?> =
            flow {
                if (sessionProvider.initialize() is KugouInitializationResult.Failure) {
                    automaticRefresh.value =
                        HomeAutomaticRefreshState.Complete(HomeRefreshResult.Failure(HomeRefreshProblem.SessionInitialization))
                }
                emitAll(
                    sessionObserver.state
                        .map { state -> state.session?.let { session -> SessionScope(session, state.generation) } }
                        .distinctUntilChanged()
                        .flatMapLatest { scope -> scope?.let(::observeSession) ?: flowOf(null) },
                )
            }

        override fun observeAutomaticRefresh(): Flow<HomeAutomaticRefreshState> = automaticRefresh.asStateFlow()

        override suspend fun refresh(force: Boolean): HomeRefreshResult {
            val session =
                when (val initialization = sessionProvider.initialize()) {
                    is KugouInitializationResult.Failure -> return HomeRefreshResult.Failure(HomeRefreshProblem.SessionInitialization)
                    is KugouInitializationResult.Ready -> initialization.session
                }
            val observedState = sessionObserver.state.value
            return singleFlight(
                scope = SessionScope(observedState.session ?: session, observedState.generation),
                force = force,
                cacheGeneration = regenerableContentCache.snapshot(),
            )
        }

        private fun observeSession(scope: SessionScope): Flow<HomeContent?> =
            flow {
                var refreshStarted = false
                observeCache(scope.session.cacheKey()).collect { content ->
                    emit(content)
                    if (!refreshStarted) {
                        refreshStarted = true
                        automaticRefresh.value = HomeAutomaticRefreshState.Refreshing
                        automaticRefresh.value =
                            HomeAutomaticRefreshState.Complete(
                                singleFlight(
                                    scope = scope,
                                    force = false,
                                    cacheGeneration = regenerableContentCache.snapshot(),
                                ),
                            )
                    }
                }
            }

        private fun observeCache(cacheKey: String): Flow<HomeContent?> =
            cacheDao
                .observe(cacheKey)
                .map { entity -> entity?.validContentOrDelete() }
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    emit(null)
                }

        private suspend fun HomeContentSnapshotEntity.validContentOrDelete(): HomeContent? {
            val content =
                takeIf { schemaVersion == HomeContentCacheCodec.CACHE_SCHEMA_VERSION }?.let {
                    codec.decode(payloadJson, updatedAtEpochMs)
                }
            if (content == null) cacheDao.delete(cacheKey)
            return content
        }

        private suspend fun singleFlight(
            scope: SessionScope,
            force: Boolean,
            cacheGeneration: RegenerableContentCacheGeneration,
        ): HomeRefreshResult {
            val cacheKey = scope.session.cacheKey()
            var ownsFlight = false
            val flight =
                flightMutex.withLock {
                    if (!force) {
                        flights[cacheKey]
                            ?.takeIf {
                                it.sessionGeneration == scope.generation && it.cacheGeneration == cacheGeneration
                            }?.let { return@withLock it }
                    }
                    val generation = generations.getOrDefault(cacheKey, 0) + 1
                    generations[cacheKey] = generation
                    RefreshFlight(generation, scope.generation, cacheGeneration, CompletableDeferred()).also {
                        flights[cacheKey] = it
                        ownsFlight = true
                    }
                }
            if (!ownsFlight) return flight.result.await()

            return try {
                val result = refreshOnce(scope, cacheKey, flight.generation, force, cacheGeneration)
                flight.result.complete(result)
                result
            } catch (cancellation: CancellationException) {
                flight.result.cancel(cancellation)
                throw cancellation
            } catch (_: Exception) {
                val failure = HomeRefreshResult.Failure(HomeRefreshProblem.Protocol)
                flight.result.complete(failure)
                failure
            } finally {
                flightMutex.withLock {
                    if (flights[cacheKey] === flight) flights.remove(cacheKey)
                }
            }
        }

        private suspend fun refreshOnce(
            scope: SessionScope,
            cacheKey: String,
            generation: Long,
            force: Boolean,
            cacheGeneration: RegenerableContentCacheGeneration,
        ): HomeRefreshResult {
            val cached = readCache(cacheKey)
            if (cached is CacheRead.StorageFailure) return HomeRefreshResult.Failure(HomeRefreshProblem.Storage)
            val cachedContent = (cached as? CacheRead.Available)?.content
            if (!force && cachedContent?.isFresh(timeProvider.nowEpochMs()) == true) return HomeRefreshResult.NotNeeded

            val responses =
                coroutineScope {
                    val banners = async { homeService.fetchHomeBanners(scope.session.requestContext()) }
                    val recommendations = async { homeService.fetchDailyRecommendations(scope.session.requestContext()) }
                    val playlists = async { homeService.fetchTopPlaylists(scope.session.requestContext()) }
                    Responses(banners.await(), recommendations.await(), playlists.await())
                }
            val recommendations = responses.recommendations.successValueOrNull()
            val playlists = responses.playlists.successValueOrNull()
            if (recommendations != null && playlists != null) {
                val content = responses.toContent(timeProvider.nowEpochMs(), recommendations, playlists)
                if (content.recommendations.isEmpty() && content.playlists.isEmpty()) {
                    return HomeRefreshResult.Success(content, persisted = false)
                }
                return when (persistIfCurrent(cacheKey, generation, scope.generation, cacheGeneration, content)) {
                    CommitResult.Persisted -> HomeRefreshResult.Success(content, persisted = true)
                    CommitResult.StorageFailure -> HomeRefreshResult.Failure(HomeRefreshProblem.Storage)
                    CommitResult.Superseded -> HomeRefreshResult.Superseded
                }
            }

            val problem =
                (responses.recommendations as? KugouApiResult.Failure)?.error?.toProblem()
                    ?: (responses.playlists as? KugouApiResult.Failure)?.error?.toProblem()
                    ?: HomeRefreshProblem.Protocol
            val partial = responses.toContent(timeProvider.nowEpochMs(), recommendations.orEmpty(), playlists.orEmpty())
            return if (cachedContent == null && (partial.recommendations.isNotEmpty() || partial.playlists.isNotEmpty())) {
                HomeRefreshResult.Partial(partial, problem)
            } else {
                HomeRefreshResult.Failure(problem)
            }
        }

        private suspend fun readCache(cacheKey: String): CacheRead =
            try {
                val entity = cacheDao.find(cacheKey) ?: return CacheRead.Missing
                val content = entity.validContentOrDelete() ?: return CacheRead.Missing
                CacheRead.Available(content)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                CacheRead.StorageFailure
            }

        private suspend fun persistIfCurrent(
            cacheKey: String,
            generation: Long,
            sessionGeneration: Long,
            cacheGeneration: RegenerableContentCacheGeneration,
            content: HomeContent,
        ): CommitResult =
            flightMutex.withLock {
                val currentSessionState = sessionObserver.state.value
                if (
                    generations[cacheKey] != generation || currentSessionState.generation != sessionGeneration ||
                    currentSessionState.session?.cacheKey() != cacheKey
                ) {
                    return@withLock CommitResult.Superseded
                }
                when (
                    regenerableContentCache.writeIfCurrent(cacheGeneration) {
                        cacheDao.upsert(
                            HomeContentSnapshotEntity(
                                cacheKey = cacheKey,
                                payloadJson = codec.encode(content),
                                schemaVersion = HomeContentCacheCodec.CACHE_SCHEMA_VERSION,
                                updatedAtEpochMs = content.updatedAtEpochMs,
                            ),
                        )
                    }
                ) {
                    RegenerableContentCacheWriteResult.Written -> CommitResult.Persisted
                    RegenerableContentCacheWriteResult.Superseded -> CommitResult.Superseded
                    RegenerableContentCacheWriteResult.StorageFailure -> CommitResult.StorageFailure
                }
            }

        private fun Responses.toContent(
            updatedAtEpochMs: Long,
            recommendationDtos: List<KugouHomeSongDto>,
            playlistDtos: List<KugouHomePlaylistDto>,
        ): HomeContent =
            HomeContent(
                banners = banners.successValueOrNull().orEmpty().map { banner -> banner.toDomain() },
                recommendations = recommendationDtos.map { recommendation -> recommendation.toDomain() },
                playlists = playlistDtos.map { playlist -> playlist.toDomain() },
                updatedAtEpochMs = updatedAtEpochMs.coerceAtLeast(0),
            )

        private fun KugouHomeBannerDto.toDomain(): HomeBanner = HomeBanner(id, title, artworkUrl.replaceFirst("http://", "https://"))

        private fun KugouHomeSongDto.toDomain(): HomeRecommendation =
            HomeRecommendation(
                song =
                    Song(
                        id = albumAudioId ?: hash,
                        hash = hash,
                        title = title,
                        artistName = artistName,
                        albumId = albumId,
                        albumTitle = null,
                        durationMs = durationMs.coerceAtLeast(0),
                        artworkUrl = artworkUrl.https(),
                    ),
                note = recommendationNote,
            )

        private fun KugouHomePlaylistDto.toDomain(): HomePlaylist = HomePlaylist(id, title, artworkUrl.https(), playCount)

        private fun String?.https(): String? = this?.replaceFirst("http://", "https://")

        private fun HomeContent.isFresh(nowEpochMs: Long): Boolean {
            val now = nowEpochMs.coerceAtLeast(0)
            return updatedAtEpochMs <= now && now - updatedAtEpochMs < CACHE_FRESH_MS
        }

        private fun KugouSessionSnapshot.cacheKey(): String {
            val authenticatedId = userId?.takeIf { !token.isNullOrBlank() && it.toLongOrNull()?.let { value -> value > 0 } == true }
            return authenticatedId?.let { "$CACHE_PREFIX_USER$it" } ?: CACHE_KEY_ANONYMOUS
        }

        private fun KugouError.toProblem(): HomeRefreshProblem =
            when (this) {
                is KugouError.Network -> {
                    when (kind) {
                        KugouError.Network.Kind.Offline -> HomeRefreshProblem.Offline
                        KugouError.Network.Kind.Timeout -> HomeRefreshProblem.Timeout
                        KugouError.Network.Kind.Connection -> HomeRefreshProblem.Connection
                    }
                }

                is KugouError.Http -> {
                    HomeRefreshProblem.ServiceUnavailable
                }

                is KugouError.Risk -> {
                    HomeRefreshProblem.Rejected
                }

                is KugouError.Protocol -> {
                    if (reason == KugouError.Protocol.Reason.ServiceRejected) HomeRefreshProblem.Rejected else HomeRefreshProblem.Protocol
                }
            }

        private fun <T> KugouApiResult<T>.successValueOrNull(): T? = (this as? KugouApiResult.Success)?.value

        private data class RefreshFlight(
            val generation: Long,
            val sessionGeneration: Long,
            val cacheGeneration: RegenerableContentCacheGeneration,
            val result: CompletableDeferred<HomeRefreshResult>,
        )

        private data class SessionScope(
            val session: KugouSessionSnapshot,
            val generation: Long,
        )

        private data class Responses(
            val banners: KugouApiResult<List<KugouHomeBannerDto>>,
            val recommendations: KugouApiResult<List<KugouHomeSongDto>>,
            val playlists: KugouApiResult<List<KugouHomePlaylistDto>>,
        )

        private sealed interface CacheRead {
            data class Available(
                val content: HomeContent,
            ) : CacheRead

            data object Missing : CacheRead

            data object StorageFailure : CacheRead
        }

        private enum class CommitResult { Persisted, StorageFailure, Superseded }

        private companion object {
            const val CACHE_FRESH_MS = 15 * 60 * 1_000L
            const val CACHE_KEY_ANONYMOUS = "home:v1:anonymous"
            const val CACHE_PREFIX_USER = "home:v1:user:"
        }
    }

@Module
@InstallIn(SingletonComponent::class)
object HomeDataModule {
    @Provides
    fun provideHomeContentSnapshotDao(database: MoeKoeDatabase): HomeContentSnapshotDao = database.homeContentSnapshotDao()

    @Provides
    fun provideKugouHomeService(client: KugouOnlineClient): KugouHomeService = client

    @Provides
    fun provideHomeTimeProvider(): HomeTimeProvider = HomeTimeProvider(System::currentTimeMillis)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeRepositoryBindings {
    @Binds
    abstract fun bindHomeRepository(implementation: KugouHomeRepository): HomeRepository
}
