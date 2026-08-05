package cn.james.music.data.local

import cn.james.music.core.database.local.LocalMusicDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalImportDebugProbe
    @Inject
    internal constructor(
        private val dao: LocalMusicDao,
    ) {
        suspend fun findBatch(batchId: String): DebugImportBatch? =
            dao.findBatch(batchId)?.let {
                DebugImportBatch(
                    state = it.state,
                    completedCount = it.completedCount,
                    failedCount = it.failedCount,
                )
            }

        suspend fun entries(batchId: String): List<DebugImportEntry> =
            dao.entries(batchId).map {
                DebugImportEntry(
                    state = it.state,
                    localMusicId = it.localMusicId,
                    errorCode = it.errorCode,
                )
            }

        suspend fun musicCount(): Int = dao.observeMusic().first().size

        suspend fun musicExists(localMusicId: String): Boolean = dao.findById(localMusicId) != null
    }

data class DebugImportBatch(
    val state: String,
    val completedCount: Int,
    val failedCount: Int,
)

data class DebugImportEntry(
    val state: String,
    val localMusicId: String?,
    val errorCode: String?,
)
