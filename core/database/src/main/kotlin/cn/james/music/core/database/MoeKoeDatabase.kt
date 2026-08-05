package cn.james.music.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.james.music.core.database.playback.PlaybackQueueItemEntity
import cn.james.music.core.database.playback.PlaybackSnapshotDao
import cn.james.music.core.database.playback.PlaybackSnapshotEntity

@Database(
    entities = [PlaybackSnapshotEntity::class, PlaybackQueueItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MoeKoeDatabase : RoomDatabase() {
    abstract fun playbackSnapshotDao(): PlaybackSnapshotDao
}
