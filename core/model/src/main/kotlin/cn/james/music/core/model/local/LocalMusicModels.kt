package cn.james.music.core.model.local

import kotlinx.coroutines.flow.Flow

data class LocalMusic(
    val id: String,
    val title: String,
    val artist: String,
    val albumTitle: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val artworkKey: String?,
    val importedAtEpochMs: Long,
)

enum class LocalImportSource { DocumentPicker, MediaStore, ExternalView, ExternalShare }

enum class ImportCompletionAction { OpenLocalLibrary, PlayImportedTrack }

enum class LocalImportBatchState { Queued, Running, Completed, PartiallyCompleted, Failed, Cancelled }

enum class LocalImportEntryState { Queued, Copying, Imported, Duplicate, Failed, Cancelled }

sealed interface LocalImportError {
    data object SourceUnavailable : LocalImportError

    data object UnsupportedFormat : LocalImportError

    data object InsufficientSpace : LocalImportError

    data object StorageUnavailable : LocalImportError

    data object CorruptFile : LocalImportError

    data object PermissionDenied : LocalImportError

    data object Cancelled : LocalImportError

    data object Unknown : LocalImportError
}

data class LocalImportProgress(
    val batchId: String,
    val state: LocalImportBatchState,
    val totalCount: Int,
    val completedCount: Int,
    val failedCount: Int,
    val currentDisplayName: String?,
    val copiedBytes: Long,
    val totalBytes: Long?,
    val error: LocalImportError? = null,
)

data class DeviceAudioCandidate(
    val mediaStoreId: Long,
    val displayName: String,
    val artist: String?,
    val durationMs: Long,
    val sizeBytes: Long,
)

enum class LocalMusicSort { Newest, Title, Artist, Duration }

interface LocalMusicRepository {
    fun observeMusic(): Flow<List<LocalMusic>>

    fun observeImports(): Flow<List<LocalImportProgress>>

    suspend fun scanDevice(): List<DeviceAudioCandidate>

    suspend fun cancelImport(batchId: String)

    suspend fun delete(localMusicId: String)
}
