package cn.james.music.cache

import cn.james.music.core.model.cache.ImageCacheClearResult
import cn.james.music.core.model.cache.ImageCacheScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.CancellationException

class CoilImageCacheClearerTest {
    @Test
    fun clearsMemoryThenDiskThroughOneOperationsInstance() =
        runBlocking {
            val calls = mutableListOf<String>()
            var providerCalls = 0
            val clearer =
                CoilImageCacheClearer.forTesting {
                    providerCalls++
                    FakeCoilCacheOperations(calls)
                }

            assertEquals(
                ImageCacheClearResult.Cleared(setOf(ImageCacheScope.Memory, ImageCacheScope.Disk)),
                clearer.clearImageCaches(),
            )
            assertEquals(1, providerCalls)
            assertEquals(listOf("memory", "disk"), calls)
        }

    @Test
    fun memoryFailureStillAttemptsDisk() =
        runBlocking {
            val calls = mutableListOf<String>()
            val clearer =
                CoilImageCacheClearer.forTesting {
                    FakeCoilCacheOperations(calls, memoryFailure = IllegalStateException("memory"))
                }

            assertEquals(
                ImageCacheClearResult.Failed(
                    clearedScopes = setOf(ImageCacheScope.Disk),
                    failedScopes = setOf(ImageCacheScope.Memory),
                ),
                clearer.clearImageCaches(),
            )
            assertEquals(listOf("memory", "disk"), calls)
        }

    @Test
    fun diskFailureRetainsCompletedMemoryScope() =
        runBlocking {
            val clearer =
                CoilImageCacheClearer.forTesting {
                    FakeCoilCacheOperations(mutableListOf(), diskFailure = IllegalStateException("disk"))
                }

            assertEquals(
                ImageCacheClearResult.Failed(
                    clearedScopes = setOf(ImageCacheScope.Memory),
                    failedScopes = setOf(ImageCacheScope.Disk),
                ),
                clearer.clearImageCaches(),
            )
        }

    @Test
    fun providerFailureReportsBothImageScopesWithoutStartingEitherClear() =
        runBlocking {
            val clearer =
                CoilImageCacheClearer.forTesting {
                    throw IllegalStateException("singleton unavailable")
                }

            assertEquals(
                ImageCacheClearResult.Failed(
                    clearedScopes = emptySet(),
                    failedScopes = setOf(ImageCacheScope.Memory, ImageCacheScope.Disk),
                ),
                clearer.clearImageCaches(),
            )
        }

    @Test
    fun providerCancellationPropagates() {
        runBlocking {
            val clearer =
                CoilImageCacheClearer.forTesting {
                    throw CancellationException("cancel")
                }

            assertThrows(CancellationException::class.java) { runBlocking { clearer.clearImageCaches() } }
        }
    }

    @Test
    fun cancellationPropagatesAndDoesNotContinueToDisk() =
        runBlocking {
            val calls = mutableListOf<String>()
            val clearer =
                CoilImageCacheClearer.forTesting {
                    FakeCoilCacheOperations(calls, memoryFailure = CancellationException("cancel"))
                }

            assertThrows(CancellationException::class.java) { runBlocking { clearer.clearImageCaches() } }
            assertEquals(listOf("memory"), calls)
        }

    private class FakeCoilCacheOperations(
        private val calls: MutableList<String>,
        private val memoryFailure: Throwable? = null,
        private val diskFailure: Throwable? = null,
    ) : CoilCacheOperations {
        override fun clearMemory() {
            calls += "memory"
            memoryFailure?.let { throw it }
        }

        override fun clearDisk() {
            calls += "disk"
            diskFailure?.let { throw it }
        }
    }
}
