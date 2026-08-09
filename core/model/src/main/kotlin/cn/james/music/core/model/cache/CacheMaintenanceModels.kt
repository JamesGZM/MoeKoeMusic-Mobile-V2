package cn.james.music.core.model.cache

/**
 * The only cache scopes that Settings may clear. User content and playback state are deliberately
 * not represented here.
 */
enum class CacheMaintenanceScope {
    HomeContentSnapshots,
    Lyrics,
    ImageMemory,
    ImageDisk,
}

/** Image-cache-only scopes exposed by the app-owned image loader adapter. */
enum class ImageCacheScope {
    Memory,
    Disk,
}

/**
 * A typed outcome from the app-owned image cache adapter. Cancellation is deliberately thrown
 * rather than represented as a normal result.
 */
sealed interface ImageCacheClearResult {
    data class Cleared(
        val scopes: Set<ImageCacheScope>,
    ) : ImageCacheClearResult

    data class Failed(
        val clearedScopes: Set<ImageCacheScope>,
        val failedScopes: Set<ImageCacheScope>,
    ) : ImageCacheClearResult
}

/** Port implemented by :app because Coil's singleton belongs to the Android composition root. */
interface ImageCacheClearer {
    suspend fun clearImageCaches(): ImageCacheClearResult
}

/**
 * The user-facing outcome of clearing all whitelisted caches. It intentionally carries no size
 * measurement: Room and Coil cannot provide one coherent atomic total.
 */
sealed interface CacheMaintenanceResult {
    data class Cleared(
        val scopes: Set<CacheMaintenanceScope>,
    ) : CacheMaintenanceResult

    data class PartiallyCleared(
        val clearedScopes: Set<CacheMaintenanceScope>,
        val failedScopes: Set<CacheMaintenanceScope>,
    ) : CacheMaintenanceResult

    data class FailedBeforeAnyClear(
        val failedScopes: Set<CacheMaintenanceScope>,
    ) : CacheMaintenanceResult
}

/** Public Settings-facing port. Its implementation coordinates Room and the app image adapter. */
interface CacheMaintenanceRepository {
    suspend fun clearCaches(): CacheMaintenanceResult
}
