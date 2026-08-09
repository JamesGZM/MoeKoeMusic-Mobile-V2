package cn.james.music.cache

import android.content.Context
import cn.james.music.core.model.cache.ImageCacheClearResult
import cn.james.music.core.model.cache.ImageCacheClearer
import cn.james.music.core.model.cache.ImageCacheScope
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Clears the already configured Coil singleton without replacing it or touching its directory. */
@Singleton
class CoilImageCacheClearer private constructor(
    private val ioDispatcher: CoroutineDispatcher,
    private val operationsProvider: () -> CoilCacheOperations,
) : ImageCacheClearer {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        ioDispatcher = Dispatchers.IO,
        operationsProvider = {
            ImageLoaderCoilCacheOperations(
                SingletonImageLoader.get(context.applicationContext),
            )
        },
    )

    override suspend fun clearImageCaches(): ImageCacheClearResult =
        withContext(ioDispatcher) {
            val operations =
                try {
                    operationsProvider()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    return@withContext ImageCacheClearResult.Failed(
                        clearedScopes = emptySet(),
                        failedScopes = ImageCacheScope.entries.toSet(),
                    )
                }
            val clearedScopes = linkedSetOf<ImageCacheScope>()
            val failedScopes = linkedSetOf<ImageCacheScope>()

            clearScope(
                scope = ImageCacheScope.Memory,
                clear = operations::clearMemory,
                clearedScopes = clearedScopes,
                failedScopes = failedScopes,
            )
            clearScope(
                scope = ImageCacheScope.Disk,
                clear = operations::clearDisk,
                clearedScopes = clearedScopes,
                failedScopes = failedScopes,
            )

            if (failedScopes.isEmpty()) {
                ImageCacheClearResult.Cleared(clearedScopes)
            } else {
                ImageCacheClearResult.Failed(clearedScopes, failedScopes)
            }
        }

    private fun clearScope(
        scope: ImageCacheScope,
        clear: () -> Unit,
        clearedScopes: MutableSet<ImageCacheScope>,
        failedScopes: MutableSet<ImageCacheScope>,
    ) {
        try {
            clear()
            clearedScopes += scope
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            failedScopes += scope
        }
    }

    internal companion object {
        fun forTesting(
            ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
            operationsProvider: () -> CoilCacheOperations,
        ): CoilImageCacheClearer = CoilImageCacheClearer(ioDispatcher, operationsProvider)
    }
}

internal interface CoilCacheOperations {
    fun clearMemory()

    fun clearDisk()
}

private class ImageLoaderCoilCacheOperations(
    private val imageLoader: ImageLoader,
) : CoilCacheOperations {
    override fun clearMemory() {
        imageLoader.memoryCache?.clear()
    }

    override fun clearDisk() {
        imageLoader.diskCache?.clear()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CoilImageCacheClearerModule {
    @Binds
    abstract fun bindImageCacheClearer(implementation: CoilImageCacheClearer): ImageCacheClearer
}
