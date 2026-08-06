package cn.james.music.core.database.home

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeContentSnapshotDao {
    @Query("SELECT * FROM home_content_snapshots WHERE cache_key = :cacheKey")
    fun observe(cacheKey: String): Flow<HomeContentSnapshotEntity?>

    @Query("SELECT * FROM home_content_snapshots WHERE cache_key = :cacheKey")
    suspend fun find(cacheKey: String): HomeContentSnapshotEntity?

    @Upsert
    suspend fun upsert(entity: HomeContentSnapshotEntity)

    @Query("DELETE FROM home_content_snapshots WHERE cache_key = :cacheKey")
    suspend fun delete(cacheKey: String)
}
