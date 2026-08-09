package cn.james.music.data.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException

class RegenerableContentCacheTest {
    @Test
    fun onlyRegenerableContentScopesAreModeled() {
        assertEquals(
            setOf(
                RegenerableContentCacheScope.HomeContentSnapshots,
                RegenerableContentCacheScope.Lyrics,
            ),
            RegenerableContentCacheScope.entries.toSet(),
        )
    }

    @Test
    fun clearSupersedesEarlierSnapshotAndPreventsItsWrite() =
        runBlocking {
            val cache = RegenerableContentCache.forTesting { }
            val beforeClear = cache.snapshot()
            var writes = 0

            assertEquals(
                RegenerableContentCacheClearResult.Cleared(RegenerableContentCacheScope.entries.toSet()),
                cache.clearAllRegenerableContent(),
            )

            assertEquals(
                RegenerableContentCacheWriteResult.Superseded,
                cache.writeIfCurrent(beforeClear) { writes++ },
            )
            assertEquals(0, writes)
        }

    @Test
    fun roomFailureIsTypedAndKeepsEarlierSnapshotUsable() =
        runBlocking {
            val cache = RegenerableContentCache.forTesting { throw IllegalStateException("fixture") }
            val snapshot = cache.snapshot()
            var writes = 0

            assertEquals(RegenerableContentCacheClearResult.RoomFailure, cache.clearAllRegenerableContent())
            assertEquals(
                RegenerableContentCacheWriteResult.Written,
                cache.writeIfCurrent(snapshot) { writes++ },
            )
            assertEquals(1, writes)
        }

    @Test
    fun cancellationPropagatesWithoutAdvancingGeneration() =
        runBlocking {
            val cache = RegenerableContentCache.forTesting { throw CancellationException("fixture") }
            val snapshot = cache.snapshot()

            assertThrows(CancellationException::class.java) { runBlocking { cache.clearAllRegenerableContent() } }
            assertEquals(RegenerableContentCacheWriteResult.Written, cache.writeIfCurrent(snapshot) { })
        }

    @Test
    fun writeWaitsForClearAndSeesItsCompletedGeneration() =
        runBlocking {
            val clearStarted = CompletableDeferred<Unit>()
            val releaseClear = CompletableDeferred<Unit>()
            val cache =
                RegenerableContentCache.forTesting {
                    clearStarted.complete(Unit)
                    releaseClear.await()
                }
            val snapshot = cache.snapshot()
            val clear = async { cache.clearAllRegenerableContent() }
            clearStarted.await()
            val write = async { cache.writeIfCurrent(snapshot) { error("stale write must not execute") } }

            assertTrue(!write.isCompleted)
            releaseClear.complete(Unit)

            assertEquals(
                RegenerableContentCacheClearResult.Cleared(RegenerableContentCacheScope.entries.toSet()),
                clear.await(),
            )
            assertEquals(RegenerableContentCacheWriteResult.Superseded, write.await())
        }
}
