package cn.james.music.core.database.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_music",
    indices = [Index(value = ["sizeBytes", "sha256"], unique = true)],
)
data class LocalMusicEntity(
    @PrimaryKey val id: String,
    val storageKey: String,
    val displayName: String,
    val title: String,
    val artist: String,
    val albumTitle: String?,
    val durationMs: Long,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val artworkKey: String?,
    val source: String,
    val importedAtEpochMs: Long,
    val pendingDeletion: Boolean = false,
)

@Entity(tableName = "local_import_batch")
data class LocalImportBatchEntity(
    @PrimaryKey val id: String,
    val source: String,
    val completionAction: String,
    val state: String,
    val totalCount: Int,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "local_import_entry",
    indices = [Index("batchId")],
)
data class LocalImportEntryEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val orderIndex: Int,
    val sourceUri: String,
    val displayName: String?,
    val expectedSizeBytes: Long?,
    val state: String,
    val copiedBytes: Long = 0,
    val localMusicId: String? = null,
    val errorCode: String? = null,
)
