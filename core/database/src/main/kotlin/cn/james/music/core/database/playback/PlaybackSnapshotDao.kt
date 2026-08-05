package cn.james.music.core.database.playback

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PlaybackSnapshotDao {
    @Transaction
    @Query("SELECT * FROM playback_snapshot WHERE id = 1")
    suspend fun getSnapshot(): PlaybackSnapshotWithQueue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: PlaybackSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(items: List<PlaybackQueueItemEntity>)

    @Query("DELETE FROM playback_queue_item")
    suspend fun deleteQueue()

    @Query("DELETE FROM playback_snapshot")
    suspend fun deleteSnapshot()

    @Transaction
    suspend fun replace(
        snapshot: PlaybackSnapshotEntity,
        items: List<PlaybackQueueItemEntity>,
    ) {
        deleteQueue()
        upsertSnapshot(snapshot)
        if (items.isNotEmpty()) insertQueue(items)
    }

    @Transaction
    suspend fun clear() {
        deleteQueue()
        deleteSnapshot()
    }
}
