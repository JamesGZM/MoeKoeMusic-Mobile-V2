package cn.james.music.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.james.music.core.database.local.LocalImportBatchEntity
import cn.james.music.core.database.local.LocalImportEntryEntity
import cn.james.music.core.database.local.LocalMusicDao
import cn.james.music.core.database.local.LocalMusicEntity
import cn.james.music.core.database.playback.PlaybackQueueItemEntity
import cn.james.music.core.database.playback.PlaybackSnapshotDao
import cn.james.music.core.database.playback.PlaybackSnapshotEntity

@Database(
    entities = [
        PlaybackSnapshotEntity::class,
        PlaybackQueueItemEntity::class,
        LocalMusicEntity::class,
        LocalImportBatchEntity::class,
        LocalImportEntryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class MoeKoeDatabase : RoomDatabase() {
    abstract fun playbackSnapshotDao(): PlaybackSnapshotDao

    abstract fun localMusicDao(): LocalMusicDao
}
