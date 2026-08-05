package cn.james.music.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import cn.james.music.core.database.local.LocalImportEntryEntity
import cn.james.music.core.database.local.LocalMusicDao
import cn.james.music.core.database.local.LocalMusicEntity
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportEntryState
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackCommandResult
import cn.james.music.playback.PlaybackController
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@HiltWorker
class LocalImportWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val dao: LocalMusicDao,
        private val playbackController: PlaybackController,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo("正在准备导入", 0, 0)

        override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                val batchId = inputData.getString(KEY_BATCH_ID) ?: return@withContext Result.failure()
                val batch = dao.findBatch(batchId) ?: return@withContext Result.failure()
                if (batch.state.substringBefore('|') == LocalImportBatchState.Cancelled.name) {
                    return@withContext Result.success()
                }
                cleanupStalePartialFiles()
                setForeground(createForegroundInfo("正在准备导入", 0, batch.totalCount))
                dao.updateBatch(batchId, LocalImportBatchState.Running.name, 0, 0, now())
                var completed = 0
                var failed = 0
                var lastFailureCode: String? = null
                var lastImported: LocalMusicEntity? = null
                try {
                    dao.entries(batchId).forEachIndexed { index, entry ->
                        currentCoroutineContext().ensureActive()
                        ensureBatchActive(batchId)
                        setForeground(createForegroundInfo(entry.displayName ?: "正在导入音乐", index, batch.totalCount))
                        when (val outcome = import(entry, batch.source)) {
                            is ImportOutcome.Imported -> {
                                completed++
                                lastImported = outcome.music
                                dao.updateEntry(
                                    entry.id,
                                    LocalImportEntryState.Imported.name,
                                    outcome.music.sizeBytes,
                                    outcome.music.id,
                                    null,
                                )
                            }

                            is ImportOutcome.Duplicate -> {
                                completed++
                                lastImported = outcome.music
                                dao.updateEntry(
                                    entry.id,
                                    LocalImportEntryState.Duplicate.name,
                                    outcome.music.sizeBytes,
                                    outcome.music.id,
                                    null,
                                )
                            }

                            is ImportOutcome.Failed -> {
                                failed++
                                lastFailureCode = outcome.code
                                android.util.Log.w("MoeKoeImport", "Import entry failed: ${outcome.code}")
                                dao.updateEntry(entry.id, LocalImportEntryState.Failed.name, 0, null, outcome.code)
                            }
                        }
                        dao.updateBatch(batchId, LocalImportBatchState.Running.name, completed, failed, now())
                    }
                } catch (_: BatchCancelledException) {
                    dao.cancelPendingEntries(batchId)
                    dao.updateBatch(batchId, LocalImportBatchState.Cancelled.name, completed, failed, now())
                    return@withContext Result.success()
                } catch (cancelled: CancellationException) {
                    dao.cancelPendingEntries(batchId)
                    dao.updateBatch(batchId, LocalImportBatchState.Cancelled.name, completed, failed, now())
                    throw cancelled
                }
                val finalState =
                    when {
                        failed == 0 -> LocalImportBatchState.Completed
                        completed > 0 -> LocalImportBatchState.PartiallyCompleted
                        else -> LocalImportBatchState.Failed
                    }
                val persistedState =
                    if (finalState == LocalImportBatchState.Failed && lastFailureCode != null) {
                        "${finalState.name}|$lastFailureCode"
                    } else {
                        finalState.name
                    }
                dao.updateBatch(batchId, persistedState, completed, failed, now())
                if (batch.completionAction == ImportCompletionAction.PlayImportedTrack.name && batch.totalCount == 1) {
                    val playbackResult = lastImported?.let { playbackController.playNow(it.toPlaybackItem()) }
                    if (playbackResult is PlaybackCommandResult.Rejected) {
                        android.util.Log.w("MoeKoeImport", "Imported track playback command was rejected")
                    }
                }
                Result.success()
            }

        private suspend fun import(
            entry: LocalImportEntryEntity,
            source: String,
        ): ImportOutcome {
            val uri =
                runCatching { Uri.parse(entry.sourceUri) }.getOrNull()
                    ?: return ImportOutcome.Failed("SourceUnavailable")
            if (uri.scheme != "content") return ImportOutcome.Failed("SourceUnavailable")
            val root =
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    ?: return ImportOutcome.Failed("StorageUnavailable")
            root.mkdirs()
            val info = queryInfo(uri)
            dao.updateEntryProgress(
                entry.id,
                LocalImportEntryState.Copying.name,
                info.displayName,
                info.size,
                0,
            )
            if (info.size != null && StatFs(root.absolutePath).availableBytes < info.size + SPACE_RESERVE_BYTES) {
                return ImportOutcome.Failed("InsufficientSpace")
            }
            val id = UUID.randomUUID().toString()
            val partial = File(root, "$id.partial")
            val finalFile = File(root, "$id.audio")
            var stage = "source"
            return try {
                val digest = Sha256Digest()
                var copied = 0L
                var lastProgressUpdate = SystemClock.elapsedRealtime()
                val sourceStream =
                    applicationContext.contentResolver.openInputStream(uri)
                        ?: return ImportOutcome.Failed("SourceUnavailable")
                stage = "destination"
                sourceStream.use { input ->
                    FileOutputStream(partial).buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            copied += count
                            val elapsed = SystemClock.elapsedRealtime()
                            if (elapsed - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                                ensureBatchActive(entry.batchId)
                                dao.updateEntryProgress(
                                    entry.id,
                                    LocalImportEntryState.Copying.name,
                                    info.displayName,
                                    info.size,
                                    copied,
                                )
                                lastProgressUpdate = elapsed
                            }
                        }
                    }
                }
                ensureBatchActive(entry.batchId)
                stage = "probe"
                if (copied == 0L || !containsAudio(partial)) {
                    partial.delete()
                    return ImportOutcome.Failed("UnsupportedFormat")
                }
                val hash = digest.hex()
                dao.findDuplicate(copied, hash)?.let { duplicate ->
                    partial.delete()
                    return ImportOutcome.Duplicate(duplicate)
                }
                stage = "metadata"
                val metadata = readMetadata(partial, info.displayName)
                stage = "commit"
                if (!partial.renameTo(finalFile)) {
                    partial.delete()
                    return ImportOutcome.Failed("StorageUnavailable")
                }
                val artworkKey = metadata.artwork?.let { saveArtwork(id, it) }
                val row =
                    LocalMusicEntity(
                        id = id,
                        storageKey = finalFile.name,
                        displayName = info.displayName,
                        title = metadata.title,
                        artist = metadata.artist,
                        albumTitle = metadata.album,
                        durationMs = metadata.durationMs,
                        mimeType = metadata.mimeType,
                        sizeBytes = copied,
                        sha256 = hash,
                        artworkKey = artworkKey,
                        source = source,
                        importedAtEpochMs = now(),
                    )
                try {
                    dao.insertMusic(row)
                } catch (error: Exception) {
                    finalFile.delete()
                    artworkKey?.let { File(applicationContext.filesDir, it).delete() }
                    throw error
                }
                ImportOutcome.Imported(row)
            } catch (cancelled: CancellationException) {
                partial.delete()
                finalFile.delete()
                File(applicationContext.filesDir, "local-artwork/$id.jpg").delete()
                throw cancelled
            } catch (cancelled: BatchCancelledException) {
                partial.delete()
                finalFile.delete()
                File(applicationContext.filesDir, "local-artwork/$id.jpg").delete()
                throw cancelled
            } catch (_: SecurityException) {
                partial.delete()
                ImportOutcome.Failed("PermissionDenied")
            } catch (error: Exception) {
                partial.delete()
                finalFile.delete()
                ImportOutcome.Failed("CorruptFile:$stage:${error.javaClass.simpleName}")
            }
        }

        private fun queryInfo(uri: Uri): SourceInfo {
            var name = "导入的音乐"
            var size: Long? = null
            applicationContext.contentResolver
                .query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor
                            .getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            .takeIf { it >= 0 }
                            ?.let { name = cursor.getString(it) ?: name }
                        cursor
                            .getColumnIndex(OpenableColumns.SIZE)
                            .takeIf { it >= 0 && !cursor.isNull(it) }
                            ?.let { size = cursor.getLong(it) }
                    }
                }
            return SourceInfo(name, size)
        }

        private fun containsAudio(file: File): Boolean =
            runCatching {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(file.absolutePath)
                    (0 until extractor.trackCount).any {
                        extractor
                            .getTrackFormat(it)
                            .getString(android.media.MediaFormat.KEY_MIME)
                            ?.startsWith("audio/") == true
                    }
                } finally {
                    extractor.release()
                }
            }.getOrDefault(false)

        private fun readMetadata(
            file: File,
            displayName: String,
        ): Metadata {
            val retriever = MediaMetadataRetriever()
            return runCatching {
                retriever.setDataSource(file.absolutePath)
                Metadata(
                    title =
                        retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?.takeIf(String::isNotBlank)
                            ?: displayName.substringBeforeLast('.'),
                    artist =
                        retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?.takeIf(String::isNotBlank)
                            ?: "未知艺术家",
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                    durationMs =
                        retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull()
                            ?: 0,
                    mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/*",
                    artwork = retriever.embeddedPicture,
                )
            }.getOrElse {
                Metadata(
                    title = displayName.substringBeforeLast('.'),
                    artist = "未知艺术家",
                    album = null,
                    durationMs = 0,
                    mimeType = "audio/*",
                    artwork = null,
                )
            }.also { retriever.release() }
        }

        private fun saveArtwork(
            id: String,
            bytes: ByteArray,
        ): String? =
            runCatching {
                val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                val scale = minOf(1f, MAX_ARTWORK_SIZE.toFloat() / maxOf(original.width, original.height))
                val bitmap =
                    if (scale < 1f) {
                        Bitmap.createScaledBitmap(
                            original,
                            (original.width * scale).toInt(),
                            (original.height * scale).toInt(),
                            true,
                        )
                    } else {
                        original
                    }
                val relative = "local-artwork/$id.jpg"
                File(applicationContext.filesDir, relative).also { it.parentFile?.mkdirs() }.outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                }
                if (bitmap !== original) bitmap.recycle()
                original.recycle()
                relative
            }.getOrNull()

        private fun createForegroundInfo(
            text: String,
            current: Int,
            total: Int,
        ): ForegroundInfo {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "音乐导入", NotificationManager.IMPORTANCE_LOW),
            )
            val notification =
                NotificationCompat
                    .Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("正在导入本地音乐")
                    .setContentText(text)
                    .setProgress(total, current, total == 0)
                    .setOngoing(true)
                    .build()
            return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }

        private fun LocalMusicEntity.toPlaybackItem() = PlaybackItem(id, title, artist, albumTitle, PlaybackSource.ImportedLocal(id))

        private suspend fun ensureBatchActive(batchId: String) {
            if (dao.findBatch(batchId)?.state?.substringBefore('|') == LocalImportBatchState.Cancelled.name) {
                throw BatchCancelledException()
            }
        }

        private fun cleanupStalePartialFiles() {
            applicationContext
                .getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?.listFiles { file -> file.isFile && file.name.endsWith(".partial") }
                ?.forEach(File::delete)
        }

        private fun now() = System.currentTimeMillis()

        private data class SourceInfo(
            val displayName: String,
            val size: Long?,
        )

        private data class Metadata(
            val title: String,
            val artist: String,
            val album: String?,
            val durationMs: Long,
            val mimeType: String,
            val artwork: ByteArray?,
        )

        private sealed interface ImportOutcome {
            data class Imported(
                val music: LocalMusicEntity,
            ) : ImportOutcome

            data class Duplicate(
                val music: LocalMusicEntity,
            ) : ImportOutcome

            data class Failed(
                val code: String,
            ) : ImportOutcome
        }

        private class BatchCancelledException : RuntimeException()

        companion object {
            const val KEY_BATCH_ID = "batch_id"
            private const val CHANNEL_ID = "local_music_import"
            private const val NOTIFICATION_ID = 2103
            private const val MAX_ARTWORK_SIZE = 1024
            private const val SPACE_RESERVE_BYTES = 16L * 1024 * 1024
            private const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        }
    }
