package cn.james.music.core.database.lyrics

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM lyrics_cache WHERE source_key = :sourceKey")
    suspend fun find(sourceKey: String): LyricsCacheEntity?

    @Upsert
    suspend fun upsert(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE source_key = :sourceKey")
    suspend fun delete(sourceKey: String)
}
