package cn.james.music.data.cache

import cn.james.music.core.model.cache.CacheMaintenanceResult
import cn.james.music.core.model.cache.CacheMaintenanceScope
import cn.james.music.core.model.cache.ImageCacheClearResult
import cn.james.music.core.model.cache.ImageCacheClearer
import cn.james.music.core.model.cache.ImageCacheScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.CancellationException

class AndroidCacheMaintenanceRepositoryTest {
    @Test
    fun clearsRoomAndBothImageScopes() =
        runBlocking {
            val imageClearer = FakeImageCacheClearer(ImageCacheClearResult.Cleared(IMAGE_SCOPES))
            val repository = repository(clearTransaction = {}, imageClearer = imageClearer)

            assertEquals(
                CacheMaintenanceResult.Cleared(ALL_SCOPES),
                repository.clearCaches(),
            )
            assertEquals(1, imageClearer.calls)
        }

    @Test
    fun roomFailureStillAttemptsAndReportsSuccessfulImageClear() =
        runBlocking {
            val imageClearer = FakeImageCacheClearer(ImageCacheClearResult.Cleared(IMAGE_SCOPES))
            val repository = repository(clearTransaction = { throw IllegalStateException("room") }, imageClearer = imageClearer)

            assertEquals(
                CacheMaintenanceResult.PartiallyCleared(
                    clearedScopes = setOf(CacheMaintenanceScope.ImageMemory, CacheMaintenanceScope.ImageDisk),
                    failedScopes = ROOM_SCOPES,
                ),
                repository.clearCaches(),
            )
            assertEquals(1, imageClearer.calls)
        }

    @Test
    fun roomSuccessAndImageMemoryFailureArePartial() =
        runBlocking {
            val repository =
                repository(
                    clearTransaction = {},
                    imageClearer =
                        FakeImageCacheClearer(
                            ImageCacheClearResult.Failed(
                                clearedScopes = setOf(ImageCacheScope.Disk),
                                failedScopes = setOf(ImageCacheScope.Memory),
                            ),
                        ),
                )

            assertEquals(
                CacheMaintenanceResult.PartiallyCleared(
                    clearedScopes = ROOM_SCOPES + CacheMaintenanceScope.ImageDisk,
                    failedScopes = setOf(CacheMaintenanceScope.ImageMemory),
                ),
                repository.clearCaches(),
            )
        }

    @Test
    fun roomSuccessAndImageDiskFailureArePartial() =
        runBlocking {
            val repository =
                repository(
                    clearTransaction = {},
                    imageClearer =
                        FakeImageCacheClearer(
                            ImageCacheClearResult.Failed(
                                clearedScopes = setOf(ImageCacheScope.Memory),
                                failedScopes = setOf(ImageCacheScope.Disk),
                            ),
                        ),
                )

            assertEquals(
                CacheMaintenanceResult.PartiallyCleared(
                    clearedScopes = ROOM_SCOPES + CacheMaintenanceScope.ImageMemory,
                    failedScopes = setOf(CacheMaintenanceScope.ImageDisk),
                ),
                repository.clearCaches(),
            )
        }

    @Test
    fun noSuccessfulScopeIsReportedAsFailureBeforeAnyClear() =
        runBlocking {
            val repository =
                repository(
                    clearTransaction = { throw IllegalStateException("room") },
                    imageClearer =
                        FakeImageCacheClearer(
                            ImageCacheClearResult.Failed(emptySet(), IMAGE_SCOPES),
                        ),
                )

            assertEquals(
                CacheMaintenanceResult.FailedBeforeAnyClear(ALL_SCOPES),
                repository.clearCaches(),
            )
        }

    @Test
    fun retryRepeatsTheWholeIdempotentClearAfterPartialFailure() =
        runBlocking {
            var roomCalls = 0
            val imageClearer =
                FakeImageCacheClearer(
                    ImageCacheClearResult.Failed(
                        clearedScopes = setOf(ImageCacheScope.Memory),
                        failedScopes = setOf(ImageCacheScope.Disk),
                    ),
                    ImageCacheClearResult.Cleared(IMAGE_SCOPES),
                )
            val repository =
                repository(
                    clearTransaction = { roomCalls++ },
                    imageClearer = imageClearer,
                )

            assertEquals(
                CacheMaintenanceResult.PartiallyCleared(
                    clearedScopes = ROOM_SCOPES + CacheMaintenanceScope.ImageMemory,
                    failedScopes = setOf(CacheMaintenanceScope.ImageDisk),
                ),
                repository.clearCaches(),
            )
            assertEquals(CacheMaintenanceResult.Cleared(ALL_SCOPES), repository.clearCaches())
            assertEquals(2, roomCalls)
            assertEquals(2, imageClearer.calls)
        }

    @Test
    fun concurrentRequestsShareOneRoomAndImageClear() =
        runBlocking {
            val roomStarted = CompletableDeferred<Unit>()
            val releaseRoom = CompletableDeferred<Unit>()
            var roomCalls = 0
            val imageClearer = FakeImageCacheClearer(ImageCacheClearResult.Cleared(IMAGE_SCOPES))
            val repository =
                repository(
                    clearTransaction = {
                        roomCalls++
                        roomStarted.complete(Unit)
                        releaseRoom.await()
                    },
                    imageClearer = imageClearer,
                )

            val first = async { repository.clearCaches() }
            roomStarted.await()
            val second = async { repository.clearCaches() }
            releaseRoom.complete(Unit)

            assertEquals(CacheMaintenanceResult.Cleared(ALL_SCOPES), first.await())
            assertEquals(CacheMaintenanceResult.Cleared(ALL_SCOPES), second.await())
            assertEquals(1, roomCalls)
            assertEquals(1, imageClearer.calls)
        }

    @Test
    fun cancellationPropagatesWithoutStartingImageClear() =
        runBlocking {
            val imageClearer = FakeImageCacheClearer(ImageCacheClearResult.Cleared(IMAGE_SCOPES))
            val repository =
                repository(
                    clearTransaction = { throw CancellationException("cancel") },
                    imageClearer = imageClearer,
                )

            assertThrows(CancellationException::class.java) { runBlocking { repository.clearCaches() } }
            assertEquals(0, imageClearer.calls)
        }

    private fun repository(
        clearTransaction: suspend () -> Unit,
        imageClearer: ImageCacheClearer,
    ): AndroidCacheMaintenanceRepository =
        AndroidCacheMaintenanceRepository(
            regenerableContentCache = RegenerableContentCache.forTesting(clearTransaction),
            imageCacheClearer = imageClearer,
        )

    private class FakeImageCacheClearer(
        private vararg val results: ImageCacheClearResult,
    ) : ImageCacheClearer {
        var calls = 0

        override suspend fun clearImageCaches(): ImageCacheClearResult {
            val result = results[calls.coerceAtMost(results.lastIndex)]
            calls++
            return result
        }
    }

    private companion object {
        val ROOM_SCOPES =
            setOf(
                CacheMaintenanceScope.HomeContentSnapshots,
                CacheMaintenanceScope.Lyrics,
            )
        val IMAGE_SCOPES = setOf(ImageCacheScope.Memory, ImageCacheScope.Disk)
        val ALL_SCOPES =
            setOf(
                CacheMaintenanceScope.HomeContentSnapshots,
                CacheMaintenanceScope.Lyrics,
                CacheMaintenanceScope.ImageMemory,
                CacheMaintenanceScope.ImageDisk,
            )
    }
}
