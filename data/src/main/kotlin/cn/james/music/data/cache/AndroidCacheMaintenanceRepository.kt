package cn.james.music.data.cache

import cn.james.music.core.model.cache.CacheMaintenanceRepository
import cn.james.music.core.model.cache.CacheMaintenanceResult
import cn.james.music.core.model.cache.CacheMaintenanceScope
import cn.james.music.core.model.cache.ImageCacheClearResult
import cn.james.music.core.model.cache.ImageCacheClearer
import cn.james.music.core.model.cache.ImageCacheScope
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composes the independently-owned Room and Coil clear operations. They are not a transaction:
 * each is attempted once unless cancellation stops the operation, and the combined result is
 * explicit about partial completion.
 */
@Singleton
class AndroidCacheMaintenanceRepository
    @Inject
    constructor(
        private val regenerableContentCache: RegenerableContentCache,
        private val imageCacheClearer: ImageCacheClearer,
    ) : CacheMaintenanceRepository {
        private val singleFlightMutex = Mutex()
        private var inFlight: CompletableDeferred<CacheMaintenanceResult>? = null

        override suspend fun clearCaches(): CacheMaintenanceResult {
            val (flight, ownsFlight) =
                singleFlightMutex.withLock {
                    inFlight?.let { existing -> existing to false }
                        ?: CompletableDeferred<CacheMaintenanceResult>().also { inFlight = it } to true
                }
            if (!ownsFlight) return flight.await()

            try {
                val result = clearOnce()
                flight.complete(result)
                return result
            } catch (cancellation: CancellationException) {
                flight.completeExceptionally(cancellation)
                throw cancellation
            } catch (failure: Exception) {
                flight.completeExceptionally(failure)
                throw failure
            } finally {
                singleFlightMutex.withLock {
                    if (inFlight === flight) inFlight = null
                }
            }
        }

        private suspend fun clearOnce(): CacheMaintenanceResult {
            val roomOutcome = regenerableContentCache.clearAllRegenerableContent().toMaintenanceOutcome()
            val imageOutcome = imageCacheClearer.clearImageCaches().toMaintenanceOutcome()
            val clearedScopes = roomOutcome.clearedScopes + imageOutcome.clearedScopes
            val failedScopes = roomOutcome.failedScopes + imageOutcome.failedScopes

            return when {
                failedScopes.isEmpty() -> CacheMaintenanceResult.Cleared(clearedScopes)
                clearedScopes.isEmpty() -> CacheMaintenanceResult.FailedBeforeAnyClear(failedScopes)
                else -> CacheMaintenanceResult.PartiallyCleared(clearedScopes, failedScopes)
            }
        }
    }

private data class CacheMaintenanceAttempt(
    val clearedScopes: Set<CacheMaintenanceScope>,
    val failedScopes: Set<CacheMaintenanceScope>,
)

private fun RegenerableContentCacheClearResult.toMaintenanceOutcome(): CacheMaintenanceAttempt =
    when (this) {
        is RegenerableContentCacheClearResult.Cleared -> {
            CacheMaintenanceAttempt(
                clearedScopes =
                    scopes.mapTo(linkedSetOf()) { scope ->
                        when (scope) {
                            RegenerableContentCacheScope.HomeContentSnapshots -> CacheMaintenanceScope.HomeContentSnapshots
                            RegenerableContentCacheScope.Lyrics -> CacheMaintenanceScope.Lyrics
                        }
                    },
                failedScopes = emptySet(),
            )
        }

        RegenerableContentCacheClearResult.RoomFailure -> {
            CacheMaintenanceAttempt(
                clearedScopes = emptySet(),
                failedScopes = ROOM_CACHE_SCOPES,
            )
        }
    }

private fun ImageCacheClearResult.toMaintenanceOutcome(): CacheMaintenanceAttempt =
    when (this) {
        is ImageCacheClearResult.Cleared -> {
            CacheMaintenanceAttempt(
                clearedScopes = scopes.mapTo(linkedSetOf(), ImageCacheScope::toMaintenanceScope),
                failedScopes = emptySet(),
            )
        }

        is ImageCacheClearResult.Failed -> {
            CacheMaintenanceAttempt(
                clearedScopes = clearedScopes.mapTo(linkedSetOf(), ImageCacheScope::toMaintenanceScope),
                failedScopes = failedScopes.mapTo(linkedSetOf(), ImageCacheScope::toMaintenanceScope),
            )
        }
    }

private fun ImageCacheScope.toMaintenanceScope(): CacheMaintenanceScope =
    when (this) {
        ImageCacheScope.Memory -> CacheMaintenanceScope.ImageMemory
        ImageCacheScope.Disk -> CacheMaintenanceScope.ImageDisk
    }

private val ROOM_CACHE_SCOPES =
    setOf(
        CacheMaintenanceScope.HomeContentSnapshots,
        CacheMaintenanceScope.Lyrics,
    )

@Module
@InstallIn(SingletonComponent::class)
abstract class CacheMaintenanceRepositoryModule {
    @Binds
    abstract fun bindCacheMaintenanceRepository(implementation: AndroidCacheMaintenanceRepository): CacheMaintenanceRepository
}
