package cn.james.music.data.playback

import android.content.Context
import androidx.room.Room
import cn.james.music.core.database.MoeKoeDatabase
import cn.james.music.core.database.playback.PlaybackQueueItemEntity
import cn.james.music.core.database.playback.PlaybackSnapshotDao
import cn.james.music.core.database.playback.PlaybackSnapshotEntity
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackSnapshot
import cn.james.music.playback.PlaybackSnapshotStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPlaybackSnapshotStore
    @Inject
    constructor(
        private val dao: PlaybackSnapshotDao,
    ) : PlaybackSnapshotStore {
        override suspend fun load(): PlaybackSnapshot? {
            val stored = dao.getSnapshot() ?: return null
            val mode = runCatching { PlaybackMode.valueOf(stored.snapshot.mode) }.getOrNull()
            if (mode == null) {
                dao.clear()
                return null
            }
            val queue = stored.queue.sortedBy(PlaybackQueueItemEntity::queueIndex).mapNotNull { it.toModel() }
            if (queue.isEmpty()) {
                dao.clear()
                return null
            }
            return PlaybackSnapshot(
                queue = queue,
                currentIndex = stored.snapshot.currentIndex.coerceIn(queue.indices),
                positionMs = stored.snapshot.positionMs.coerceAtLeast(0),
                mode = mode,
                updatedAtEpochMs = stored.snapshot.updatedAtEpochMs,
            )
        }

        override suspend fun save(snapshot: PlaybackSnapshot) {
            if (snapshot.queue.isEmpty()) {
                dao.clear()
                return
            }
            dao.replace(
                snapshot =
                    PlaybackSnapshotEntity(
                        currentIndex = snapshot.currentIndex.coerceIn(snapshot.queue.indices),
                        positionMs = snapshot.positionMs.coerceAtLeast(0),
                        mode = snapshot.mode.name,
                        updatedAtEpochMs = snapshot.updatedAtEpochMs,
                    ),
                items = snapshot.queue.mapIndexed { index, item -> item.toEntity(index) },
            )
        }

        override suspend fun clear() = dao.clear()
    }

private fun PlaybackQueueItemEntity.toModel(): PlaybackItem? {
    val source =
        when (sourceType) {
            SOURCE_DEMO -> PlaybackSource.FoundationDemo
            SOURCE_LOCAL -> sourceValue?.takeIf(String::isNotBlank)?.let(PlaybackSource::ImportedLocal)
            SOURCE_KUGOU -> sourceValue?.takeIf(String::isNotBlank)?.let(PlaybackSource::Kugou)
            else -> null
        } ?: return null
    return runCatching {
        PlaybackItem(
            id = mediaId,
            title = title,
            artist = artist,
            albumTitle = albumTitle,
            source = source,
        )
    }.getOrNull()
}

private fun PlaybackItem.toEntity(index: Int): PlaybackQueueItemEntity {
    val (type, value) =
        when (val itemSource = source) {
            PlaybackSource.FoundationDemo -> SOURCE_DEMO to null
            is PlaybackSource.ImportedLocal -> SOURCE_LOCAL to itemSource.localMusicId
            is PlaybackSource.Kugou -> SOURCE_KUGOU to itemSource.songHash
        }
    return PlaybackQueueItemEntity(
        queueIndex = index,
        mediaId = id,
        title = title,
        artist = artist,
        albumTitle = albumTitle,
        sourceType = type,
        sourceValue = value,
    )
}

private const val SOURCE_DEMO = "foundation_demo"
private const val SOURCE_LOCAL = "imported_local"
private const val SOURCE_KUGOU = "kugou"

@Module
@InstallIn(SingletonComponent::class)
object PlaybackDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MoeKoeDatabase = Room.databaseBuilder(context, MoeKoeDatabase::class.java, "moekoe.db").build()

    @Provides
    fun providePlaybackSnapshotDao(database: MoeKoeDatabase): PlaybackSnapshotDao = database.playbackSnapshotDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackSnapshotStoreModule {
    @Binds
    abstract fun bindPlaybackSnapshotStore(implementation: RoomPlaybackSnapshotStore): PlaybackSnapshotStore
}
