@file:Suppress("ktlint:standard:max-line-length")

package cn.james.music.core.database.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalMusicDao {
    @Query("SELECT * FROM local_music WHERE pendingDeletion = 0 ORDER BY importedAtEpochMs DESC")
    fun observeMusic(): Flow<List<LocalMusicEntity>>

    @Query("SELECT * FROM local_music WHERE id = :id")
    suspend fun findById(id: String): LocalMusicEntity?

    @Query("SELECT * FROM local_music WHERE sizeBytes = :size AND sha256 = :sha256 LIMIT 1")
    suspend fun findDuplicate(
        size: Long,
        sha256: String,
    ): LocalMusicEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMusic(entity: LocalMusicEntity)

    @Query("UPDATE local_music SET pendingDeletion = 1 WHERE id = :id")
    suspend fun markDeleting(id: String)

    @Query("DELETE FROM local_music WHERE id = :id")
    suspend fun deleteMusic(id: String)

    @Query("SELECT * FROM local_import_batch ORDER BY createdAtEpochMs DESC")
    fun observeBatches(): Flow<List<LocalImportBatchEntity>>

    @Query("SELECT * FROM local_import_batch WHERE id = :id")
    suspend fun findBatch(id: String): LocalImportBatchEntity?

    @Query("SELECT * FROM local_import_entry WHERE batchId = :batchId ORDER BY orderIndex")
    suspend fun entries(batchId: String): List<LocalImportEntryEntity>

    @Query("SELECT * FROM local_import_entry ORDER BY batchId, orderIndex")
    fun observeEntries(): Flow<List<LocalImportEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBatch(batch: LocalImportBatchEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(entries: List<LocalImportEntryEntity>)

    @Transaction
    suspend fun createBatch(
        batch: LocalImportBatchEntity,
        entries: List<LocalImportEntryEntity>,
    ) {
        insertBatch(batch)
        insertEntries(entries)
    }

    @Query(
        "UPDATE local_import_batch SET state = :state, completedCount = :completed, failedCount = :failed, updatedAtEpochMs = :updatedAt WHERE id = :id",
    )
    suspend fun updateBatch(
        id: String,
        state: String,
        completed: Int,
        failed: Int,
        updatedAt: Long,
    )

    @Query(
        "UPDATE local_import_entry SET state = :state, copiedBytes = :copiedBytes, localMusicId = :localMusicId, errorCode = :errorCode WHERE id = :id",
    )
    suspend fun updateEntry(
        id: String,
        state: String,
        copiedBytes: Long,
        localMusicId: String?,
        errorCode: String?,
    )

    @Query(
        "UPDATE local_import_entry SET state = :state, displayName = :displayName, expectedSizeBytes = :expectedSizeBytes, copiedBytes = :copiedBytes WHERE id = :id",
    )
    suspend fun updateEntryProgress(
        id: String,
        state: String,
        displayName: String,
        expectedSizeBytes: Long?,
        copiedBytes: Long,
    )

    @Query(
        "UPDATE local_import_entry SET state = 'Cancelled', errorCode = 'Cancelled' WHERE batchId = :batchId AND state IN ('Queued', 'Copying')",
    )
    suspend fun cancelPendingEntries(batchId: String)
}
