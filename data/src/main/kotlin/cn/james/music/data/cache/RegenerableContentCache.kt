package cn.james.music.data.cache

import androidx.room.withTransaction
import cn.james.music.core.database.MoeKoeDatabase
import cn.james.music.core.database.home.HomeContentSnapshotDao
import cn.james.music.core.database.lyrics.LyricsCacheDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates only durable content that can safely be regenerated: Home snapshots and KRC lyrics.
 * Playback snapshots, imported music, DataStore, and every other database table are intentionally
 * outside this boundary.
 */
@Singleton
class RegenerableContentCache private constructor(
    private val clearTransaction: suspend () -> Unit,
) {
    private val mutex = Mutex()
    private var generation = 0L

    @Inject
    constructor(
        database: MoeKoeDatabase,
        homeContentSnapshotDao: HomeContentSnapshotDao,
        lyricsCacheDao: LyricsCacheDao,
    ) : this(
        clearTransaction = {
            database.withTransaction {
                homeContentSnapshotDao.deleteAll()
                lyricsCacheDao.deleteAll()
            }
        },
    )

    internal constructor(
        clearTransaction: suspend () -> Unit,
        @Suppress("UNUSED_PARAMETER") testOnly: Unit = Unit,
    ) : this(clearTransaction)

    companion object {
        internal fun forTesting(clearTransaction: suspend () -> Unit): RegenerableContentCache =
            RegenerableContentCache(clearTransaction, Unit)
    }

    internal suspend fun snapshot(): RegenerableContentCacheGeneration = mutex.withLock { RegenerableContentCacheGeneration(generation) }

    internal suspend fun writeIfCurrent(
        snapshot: RegenerableContentCacheGeneration,
        write: suspend () -> Unit,
    ): RegenerableContentCacheWriteResult =
        mutex.withLock {
            if (snapshot.value != generation) return@withLock RegenerableContentCacheWriteResult.Superseded
            try {
                write()
                RegenerableContentCacheWriteResult.Written
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                RegenerableContentCacheWriteResult.StorageFailure
            }
        }

    /**
     * Deletes both whitelisted Room caches in one transaction. A successful clear advances the
     * generation while the mutex is held, so any earlier refresh/load cannot repopulate storage.
     */
    internal suspend fun clearAllRegenerableContent(): RegenerableContentCacheClearResult =
        mutex.withLock {
            try {
                clearTransaction()
                generation++
                RegenerableContentCacheClearResult.Cleared(RegenerableContentCacheScope.entries.toSet())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                RegenerableContentCacheClearResult.RoomFailure
            }
        }
}

@JvmInline
internal value class RegenerableContentCacheGeneration internal constructor(
    internal val value: Long,
)

internal enum class RegenerableContentCacheWriteResult {
    Written,
    Superseded,
    StorageFailure,
}

/** The only durable scopes this coordinator is allowed to delete. */
internal enum class RegenerableContentCacheScope {
    HomeContentSnapshots,
    Lyrics,
}

/** Typed Room-scope result for a later cache-maintenance orchestrator to combine with Coil. */
internal sealed interface RegenerableContentCacheClearResult {
    data class Cleared(
        val scopes: Set<RegenerableContentCacheScope>,
    ) : RegenerableContentCacheClearResult

    data object RoomFailure : RegenerableContentCacheClearResult
}
