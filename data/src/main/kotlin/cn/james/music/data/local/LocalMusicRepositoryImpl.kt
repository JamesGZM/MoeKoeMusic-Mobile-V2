@file:Suppress("ktlint:standard:max-line-length")

package cn.james.music.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import cn.james.music.core.database.MoeKoeDatabase
import cn.james.music.core.database.local.LocalImportBatchEntity
import cn.james.music.core.database.local.LocalImportEntryEntity
import cn.james.music.core.database.local.LocalMusicDao
import cn.james.music.core.database.local.LocalMusicEntity
import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportEntryState
import cn.james.music.core.model.local.LocalImportError
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalImportSource
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.playback.ImportedLocalSourceResolver
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLocalMusicRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val dao: LocalMusicDao,
        private val workManager: WorkManager,
    ) : LocalMusicRepository,
        LocalImportGateway,
        ImportedLocalSourceResolver {
        override fun observeMusic(): Flow<List<LocalMusic>> = dao.observeMusic().map { rows -> rows.map { it.toModel() } }

        override fun observeImports(): Flow<List<LocalImportProgress>> =
            combine(dao.observeBatches(), dao.observeEntries()) { batches, entries ->
                batches.map { batch ->
                    val current =
                        entries.firstOrNull {
                            it.batchId == batch.id && it.state == LocalImportEntryState.Copying.name
                        }
                    LocalImportProgress(
                        batchId = batch.id,
                        state = enumValueOrDefault(batch.state.substringBefore('|'), LocalImportBatchState.Failed),
                        totalCount = batch.totalCount,
                        completedCount = batch.completedCount,
                        failedCount = batch.failedCount,
                        currentDisplayName = current?.displayName,
                        copiedBytes = current?.copiedBytes ?: 0,
                        totalBytes = current?.expectedSizeBytes,
                        error =
                            batch.state
                                .substringAfter('|', "")
                                .ifBlank { null }
                                .toLocalImportError(),
                    )
                }
            }

        override suspend fun enqueue(
            uris: List<Uri>,
            source: LocalImportSource,
            completionAction: ImportCompletionAction,
        ): String {
            require(uris.isNotEmpty())
            val batchId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            dao.createBatch(
                LocalImportBatchEntity(
                    id = batchId,
                    source = source.name,
                    completionAction = completionAction.name,
                    state = LocalImportBatchState.Queued.name,
                    totalCount = uris.size,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                ),
                uris.mapIndexed { index, uri ->
                    LocalImportEntryEntity(
                        id = UUID.randomUUID().toString(),
                        batchId = batchId,
                        orderIndex = index,
                        sourceUri = uri.toString(),
                        displayName = null,
                        expectedSizeBytes = null,
                        state = "Queued",
                    )
                },
            )
            val request =
                OneTimeWorkRequestBuilder<LocalImportWorker>()
                    .setInputData(workDataOf(LocalImportWorker.KEY_BATCH_ID to batchId))
                    .addTag(batchId)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            workManager.enqueueUniqueWork(IMPORT_QUEUE, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            return batchId
        }

        override suspend fun enqueueMediaStore(mediaStoreIds: List<Long>): String =
            enqueue(
                uris =
                    mediaStoreIds.map { id ->
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    },
                source = LocalImportSource.MediaStore,
                completionAction = ImportCompletionAction.OpenLocalLibrary,
            )

        override fun scanDevice(): Flow<DeviceAudioCandidate> =
            flow {
                val projection =
                    arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.SIZE,
                    )
                context.contentResolver
                    .query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                        null,
                        "${MediaStore.Audio.Media.DATE_ADDED} DESC",
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                        while (cursor.moveToNext()) {
                            emit(
                                DeviceAudioCandidate(
                                    mediaStoreId = cursor.getLong(idColumn),
                                    displayName = cursor.getString(nameColumn) ?: "未知文件",
                                    artist = cursor.getString(artistColumn),
                                    durationMs = cursor.getLong(durationColumn),
                                    sizeBytes = cursor.getLong(sizeColumn),
                                ),
                            )
                        }
                    }
            }.flowOn(Dispatchers.IO)

        override suspend fun cancelImport(batchId: String) {
            val batch = dao.findBatch(batchId) ?: return
            dao.cancelPendingEntries(batchId)
            dao.updateBatch(
                batchId,
                LocalImportBatchState.Cancelled.name,
                batch.completedCount,
                batch.failedCount,
                System.currentTimeMillis(),
            )
        }

        override suspend fun delete(localMusicId: String) {
            val row = dao.findById(localMusicId) ?: return
            dao.markDeleting(localMusicId)
            File(requireNotNull(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)), row.storageKey).delete()
            row.artworkKey?.let { File(context.filesDir, it).delete() }
            dao.deleteMusic(localMusicId)
        }

        override suspend fun resolve(localMusicId: String): Uri? {
            val row = dao.findById(localMusicId)?.takeUnless(LocalMusicEntity::pendingDeletion) ?: return null
            val root = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC) ?: return null
            return File(root, row.storageKey).takeIf(File::isFile)?.let(Uri::fromFile)
        }

        private fun LocalMusicEntity.toModel() =
            LocalMusic(
                id = id,
                title = title,
                artist = artist,
                albumTitle = albumTitle,
                durationMs = durationMs,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                artworkKey = artworkKey,
                importedAtEpochMs = importedAtEpochMs,
            )

        private inline fun <reified T : Enum<T>> enumValueOrDefault(
            value: String,
            default: T,
        ): T = runCatching { enumValueOf<T>(value) }.getOrDefault(default)

        private fun String?.toLocalImportError(): LocalImportError? =
            when (this?.substringBefore(':')) {
                null -> null
                "SourceUnavailable" -> LocalImportError.SourceUnavailable
                "UnsupportedFormat" -> LocalImportError.UnsupportedFormat
                "InsufficientSpace" -> LocalImportError.InsufficientSpace
                "StorageUnavailable" -> LocalImportError.StorageUnavailable
                "PermissionDenied" -> LocalImportError.PermissionDenied
                "Cancelled" -> LocalImportError.Cancelled
                "CorruptFile" -> LocalImportError.CorruptFile
                else -> LocalImportError.Unknown
            }

        companion object {
            const val IMPORT_QUEUE = "local-music-import-queue"
        }
    }

@Module
@InstallIn(SingletonComponent::class)
object LocalMusicDataModule {
    @Provides fun provideLocalMusicDao(database: MoeKoeDatabase): LocalMusicDao = database.localMusicDao()

    @Provides @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalMusicBindings {
    @Binds abstract fun bindRepository(implementation: AndroidLocalMusicRepository): LocalMusicRepository

    @Binds abstract fun bindGateway(implementation: AndroidLocalMusicRepository): LocalImportGateway

    @Binds abstract fun bindLocalResolver(implementation: AndroidLocalMusicRepository): ImportedLocalSourceResolver
}
