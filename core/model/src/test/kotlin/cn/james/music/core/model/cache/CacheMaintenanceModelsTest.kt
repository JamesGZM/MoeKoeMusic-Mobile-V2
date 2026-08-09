package cn.james.music.core.model.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheMaintenanceModelsTest {
    @Test
    fun maintenanceScopesAreLimitedToRegenerableContentAndCoilCaches() {
        assertEquals(
            setOf(
                CacheMaintenanceScope.HomeContentSnapshots,
                CacheMaintenanceScope.Lyrics,
                CacheMaintenanceScope.ImageMemory,
                CacheMaintenanceScope.ImageDisk,
            ),
            CacheMaintenanceScope.entries.toSet(),
        )
    }

    @Test
    fun partialResultRetainsBothClearedAndFailedScopes() {
        val result =
            CacheMaintenanceResult.PartiallyCleared(
                clearedScopes = setOf(CacheMaintenanceScope.HomeContentSnapshots),
                failedScopes = setOf(CacheMaintenanceScope.ImageDisk),
            )

        assertTrue(CacheMaintenanceScope.HomeContentSnapshots in result.clearedScopes)
        assertTrue(CacheMaintenanceScope.ImageDisk in result.failedScopes)
    }
}
