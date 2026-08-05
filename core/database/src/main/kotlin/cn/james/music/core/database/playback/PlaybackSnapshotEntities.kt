package cn.james.music.core.database.playback

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "playback_snapshot")
data class PlaybackSnapshotEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "current_index") val currentIndex: Int,
    @ColumnInfo(name = "position_ms") val positionMs: Long,
    val mode: String,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(
    tableName = "playback_queue_item",
    primaryKeys = ["snapshot_id", "queue_index"],
    foreignKeys = [
        ForeignKey(
            entity = PlaybackSnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshot_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("snapshot_id")],
)
data class PlaybackQueueItemEntity(
    @ColumnInfo(name = "snapshot_id") val snapshotId: Int = PlaybackSnapshotEntity.SINGLETON_ID,
    @ColumnInfo(name = "queue_index") val queueIndex: Int,
    @ColumnInfo(name = "media_id") val mediaId: String,
    val title: String,
    val artist: String,
    @ColumnInfo(name = "album_title") val albumTitle: String?,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "source_value") val sourceValue: String?,
)

data class PlaybackSnapshotWithQueue(
    @androidx.room.Embedded val snapshot: PlaybackSnapshotEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "snapshot_id",
    )
    val queue: List<PlaybackQueueItemEntity>,
)
