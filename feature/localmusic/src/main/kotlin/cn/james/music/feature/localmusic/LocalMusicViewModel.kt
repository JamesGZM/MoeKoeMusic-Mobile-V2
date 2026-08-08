package cn.james.music.feature.localmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface DeviceImportUiState {
    data object PermissionRequired : DeviceImportUiState

    data class Scanning(
        val candidates: List<DeviceAudioCandidate>,
    ) : DeviceImportUiState

    data class Selection(
        val candidates: List<DeviceAudioCandidate>,
        val selectedIds: Set<Long> = emptySet(),
    ) : DeviceImportUiState

    data object Failed : DeviceImportUiState
}

internal data class LocalMusicUiState(
    val music: List<LocalMusic> = emptyList(),
    val imports: List<LocalImportProgress> = emptyList(),
    val deviceImport: DeviceImportUiState = DeviceImportUiState.PermissionRequired,
)

@HiltViewModel
internal class LocalMusicViewModel
    @Inject
    constructor(
        private val repository: LocalMusicRepository,
        private val playbackController: PlaybackController,
    ) : ViewModel() {
        private val deviceImport = MutableStateFlow<DeviceImportUiState>(DeviceImportUiState.PermissionRequired)
        private var scanJob: Job? = null
        val state: StateFlow<LocalMusicUiState> =
            combine(repository.observeMusic(), repository.observeImports(), deviceImport, ::LocalMusicUiState)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalMusicUiState())

        fun openImporter(hasPermission: Boolean) {
            if (hasPermission) {
                scanDevice()
            } else {
                cancelDeviceScan()
                deviceImport.value = DeviceImportUiState.PermissionRequired
            }
        }

        fun onPermissionResult(granted: Boolean) {
            if (granted) scanDevice() else deviceImport.value = DeviceImportUiState.PermissionRequired
        }

        fun retryDeviceScan() = scanDevice()

        fun toggleCandidate(mediaStoreId: Long) {
            deviceImport.update { current ->
                if (current !is DeviceImportUiState.Selection) return@update current
                current.copy(
                    selectedIds =
                        if (mediaStoreId in current.selectedIds) {
                            current.selectedIds - mediaStoreId
                        } else {
                            current.selectedIds + mediaStoreId
                        },
                )
            }
        }

        fun toggleAllCandidates() {
            deviceImport.update { current ->
                if (current !is DeviceImportUiState.Selection) return@update current
                current.copy(
                    selectedIds =
                        if (current.selectedIds.size == current.candidates.size) {
                            emptySet()
                        } else {
                            current.candidates.mapTo(linkedSetOf(), DeviceAudioCandidate::mediaStoreId)
                        },
                )
            }
        }

        fun cancelDeviceScan() {
            scanJob?.cancel()
            scanJob = null
        }

        private fun scanDevice() {
            scanJob?.cancel()
            scanJob =
                viewModelScope.launch {
                    deviceImport.value = DeviceImportUiState.Scanning(emptyList())
                    try {
                        repository.scanDevice().collect { candidate ->
                            deviceImport.update { current ->
                                val candidates = (current as? DeviceImportUiState.Scanning)?.candidates.orEmpty()
                                DeviceImportUiState.Scanning(
                                    candidates =
                                        if (candidates.any { it.mediaStoreId == candidate.mediaStoreId }) {
                                            candidates
                                        } else {
                                            candidates + candidate
                                        },
                                )
                            }
                        }
                        val candidates =
                            (deviceImport.value as? DeviceImportUiState.Scanning)?.candidates
                                ?: return@launch
                        deviceImport.value = DeviceImportUiState.Selection(candidates)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        deviceImport.value = DeviceImportUiState.Failed
                    }
                }
        }

        fun play(
            music: LocalMusic,
            visible: List<LocalMusic>,
        ) {
            viewModelScope.launch {
                val queue = visible.map(LocalMusic::toPlaybackItem)
                playbackController.replaceQueue(queue, queue.indexOfFirst { it.id == music.id }, true)
            }
        }

        fun cancelImport(batchId: String) {
            viewModelScope.launch { repository.cancelImport(batchId) }
        }

        fun delete(id: String) {
            viewModelScope.launch {
                playbackController.state.value.queue
                    .mapIndexedNotNull { index, item -> index.takeIf { item.id == id } }
                    .asReversed()
                    .forEach { playbackController.remove(it) }
                repository.delete(id)
            }
        }
    }

private fun LocalMusic.toPlaybackItem() =
    PlaybackItem(
        id = id,
        title = title,
        artist = artist,
        albumTitle = albumTitle,
        source = PlaybackSource.ImportedLocal(id),
        artwork = artworkKey?.let { runCatching { PlaybackArtwork.AppFile(it) }.getOrNull() },
    )
