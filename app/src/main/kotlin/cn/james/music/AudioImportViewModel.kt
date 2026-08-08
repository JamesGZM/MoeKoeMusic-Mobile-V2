package cn.james.music

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportError
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalImportSource
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.data.local.LocalImportGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ExternalAudioImportRequest(
    val uris: List<Uri>,
    val source: LocalImportSource,
    val completionAction: ImportCompletionAction,
)

internal data class AudioImportUiState(
    val message: String = "正在准备导入…",
    val finished: Boolean = false,
)

@HiltViewModel
internal class AudioImportViewModel
    @Inject
    constructor(
        private val gateway: LocalImportGateway,
        private val repository: LocalMusicRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(AudioImportUiState())
        val state: StateFlow<AudioImportUiState> = mutableState.asStateFlow()

        private var observationJob: Job? = null

        fun resumeOrEnqueue(request: ExternalAudioImportRequest) {
            if (observationJob != null) return
            val restoredBatchId = savedStateHandle.get<String>(KEY_BATCH_ID)
            observationJob =
                viewModelScope.launch {
                    if (restoredBatchId == null) {
                        enqueueAndObserve(request)
                    } else {
                        observe(restoredBatchId)
                    }
                }
        }

        fun replaceWith(request: ExternalAudioImportRequest) {
            observationJob?.cancel()
            savedStateHandle[KEY_BATCH_ID] = null
            mutableState.value = AudioImportUiState()
            observationJob = viewModelScope.launch { enqueueAndObserve(request) }
        }

        fun rejectInvalidSource() {
            observationJob?.cancel()
            observationJob = null
            savedStateHandle[KEY_BATCH_ID] = null
            mutableState.value = AudioImportUiState(message = "无法读取这个音频来源", finished = true)
        }

        private suspend fun enqueueAndObserve(request: ExternalAudioImportRequest) {
            try {
                val batchId = gateway.enqueue(request.uris, request.source, request.completionAction)
                savedStateHandle[KEY_BATCH_ID] = batchId
                observe(batchId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = AudioImportUiState(message = "导入失败，请重新选择文件", finished = true)
            }
        }

        private suspend fun observe(batchId: String) {
            repository.observeImports().first { rows ->
                val progress = rows.firstOrNull { it.batchId == batchId } ?: return@first false
                mutableState.value =
                    AudioImportUiState(
                        message = progress.toUserMessage(),
                        finished = progress.state in TERMINAL_STATES,
                    )
                progress.state in TERMINAL_STATES
            }
        }

        private companion object {
            const val KEY_BATCH_ID = "audio_import_batch_id"
            val TERMINAL_STATES =
                setOf(
                    LocalImportBatchState.Completed,
                    LocalImportBatchState.PartiallyCompleted,
                    LocalImportBatchState.Failed,
                    LocalImportBatchState.Cancelled,
                )
        }
    }

private fun LocalImportProgress.toUserMessage(): String =
    when (state) {
        LocalImportBatchState.Queued -> {
            "已加入导入队列"
        }

        LocalImportBatchState.Running -> {
            currentDisplayName?.let { "正在导入 $it · $completedCount/$totalCount" }
                ?: "正在导入 · $completedCount/$totalCount"
        }

        LocalImportBatchState.Completed -> {
            "导入完成 · $completedCount/$totalCount"
        }

        LocalImportBatchState.PartiallyCompleted -> {
            "部分导入成功 · 成功 $completedCount · 失败 $failedCount"
        }

        LocalImportBatchState.Failed -> {
            error.toImportFailureMessage()
        }

        LocalImportBatchState.Cancelled -> {
            "导入已取消 · 已保留 $completedCount 首"
        }
    }

private fun LocalImportError?.toImportFailureMessage(): String =
    when (this) {
        LocalImportError.SourceUnavailable -> "无法读取这个音频来源"
        LocalImportError.UnsupportedFormat -> "文件不是受支持的音频格式"
        LocalImportError.InsufficientSpace -> "存储空间不足，无法完成导入"
        LocalImportError.StorageUnavailable -> "App 音乐目录暂时不可用"
        LocalImportError.PermissionDenied -> "没有读取这个文件的权限"
        LocalImportError.CorruptFile -> "音频文件已损坏或无法解析"
        LocalImportError.Cancelled -> "导入已取消"
        LocalImportError.Unknown, null -> "导入失败，请重新选择文件"
    }
