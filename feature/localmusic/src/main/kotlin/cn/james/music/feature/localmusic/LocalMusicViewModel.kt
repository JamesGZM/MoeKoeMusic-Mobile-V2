package cn.james.music.feature.localmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackController
import cn.james.music.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class LocalMusicViewModel
    @Inject
    constructor(
        private val repository: LocalMusicRepository,
        private val playbackController: PlaybackController,
    ) : ViewModel() {
        private val controls = MutableStateFlow(LocalMusicControls())
        private val deviceSnapshot = MutableStateFlow(DeviceImportSnapshot())
        private val localSnapshot = MutableStateFlow(LocalMusicSnapshot())
        private var scanJob: Job? = null

        val state: StateFlow<LocalMusicUiState> =
            localSnapshot
                .map { snapshot -> snapshot.uiState }
                .stateIn(viewModelScope, SharingStarted.Eagerly, LocalMusicUiState())

        init {
            viewModelScope.launch {
                combine(
                    repository.observeMusic(),
                    repository.observeImports(),
                    deviceSnapshot,
                    playbackController.state,
                    controls,
                    ::buildLocalMusicSnapshot,
                ).collect { snapshot -> localSnapshot.value = snapshot }
            }
        }

        fun onAction(action: LocalMusicAction) {
            when (action) {
                LocalMusicAction.Back,
                LocalMusicAction.OpenImporter,
                -> {
                    Unit
                }

                is LocalMusicAction.QueryChanged -> {
                    controls.update { current -> current.copy(query = action.value) }
                }

                is LocalMusicAction.SelectSort -> {
                    controls.update { current -> current.copy(sort = action.sort) }
                }

                is LocalMusicAction.PlaySong -> {
                    play(action.id)
                }

                is LocalMusicAction.RequestDeleteSong -> {
                    if (action.id in localSnapshot.value.musicById) {
                        controls.update { current -> current.copy(pendingDeleteSongId = action.id) }
                    }
                }

                is LocalMusicAction.ConfirmDeleteSong -> {
                    controls.update { current ->
                        current.copy(
                            pendingDeleteSongId = current.pendingDeleteSongId.takeUnless { it == action.id },
                        )
                    }
                    delete(action.id)
                }

                LocalMusicAction.DismissDeleteSong -> {
                    controls.update { current -> current.copy(pendingDeleteSongId = null) }
                }

                is LocalMusicAction.CancelImport -> {
                    cancelImport(action.id)
                }
            }
        }

        fun onDeviceImportAction(action: DeviceImportAction) {
            when (action) {
                is DeviceImportAction.Open -> openImporter(action.hasPermission)

                DeviceImportAction.Back,
                DeviceImportAction.Cancel,
                -> cancelDeviceScan()

                DeviceImportAction.RequestPermission,
                is DeviceImportAction.Import,
                -> Unit

                is DeviceImportAction.PermissionResult -> onPermissionResult(action.granted)

                DeviceImportAction.Retry -> scanDevice()

                is DeviceImportAction.ToggleCandidate -> toggleCandidate(action.id)

                DeviceImportAction.ToggleAll -> toggleAllCandidates()
            }
        }

        private fun openImporter(hasPermission: Boolean) {
            if (hasPermission) {
                scanDevice()
            } else {
                cancelDeviceScan()
                deviceSnapshot.value = DeviceImportSnapshot()
            }
        }

        private fun onPermissionResult(granted: Boolean) {
            if (granted) {
                scanDevice()
            } else {
                cancelDeviceScan()
                deviceSnapshot.value = DeviceImportSnapshot()
            }
        }

        private fun toggleCandidate(id: Long) {
            deviceSnapshot.update { current ->
                val selection = current.uiState as? DeviceImportUiState.Selection ?: return@update current
                if (id !in current.candidatesById) return@update current
                current.copy(
                    uiState =
                        selection.copy(
                            selectedIds =
                                if (id in selection.selectedIds) {
                                    selection.selectedIds - id
                                } else {
                                    selection.selectedIds + id
                                },
                        ),
                )
            }
        }

        private fun toggleAllCandidates() {
            deviceSnapshot.update { current ->
                val selection = current.uiState as? DeviceImportUiState.Selection ?: return@update current
                val candidateIds = selection.candidates.mapTo(linkedSetOf(), DeviceCandidateRowUiModel::id)
                current.copy(
                    uiState =
                        selection.copy(
                            selectedIds =
                                if (selection.selectedIds.size == candidateIds.size) {
                                    emptySet()
                                } else {
                                    candidateIds
                                },
                        ),
                )
            }
        }

        private fun cancelDeviceScan() {
            scanJob?.cancel()
            scanJob = null
        }

        private fun scanDevice() {
            cancelDeviceScan()
            scanJob =
                viewModelScope.launch {
                    deviceSnapshot.value =
                        DeviceImportSnapshot(
                            uiState = DeviceImportUiState.Scanning(emptyList()),
                        )
                    try {
                        repository.scanDevice().collect { candidate ->
                            deviceSnapshot.update { current -> current.append(candidate) }
                        }
                        deviceSnapshot.update { current ->
                            val scanning = current.uiState as? DeviceImportUiState.Scanning ?: return@update current
                            current.copy(uiState = DeviceImportUiState.Selection(scanning.candidates))
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        deviceSnapshot.update { current -> current.copy(uiState = DeviceImportUiState.Failed) }
                    }
                }
        }

        private fun play(id: String) {
            val current = localSnapshot.value
            if (id !in current.musicById) return
            val queue = current.visibleMusic.map(LocalMusic::toPlaybackItem)
            val startIndex = queue.indexOfFirst { item -> item.id == id }
            if (startIndex < 0) return
            viewModelScope.launch {
                playbackController.replaceQueue(queue, startIndex, true)
            }
        }

        private fun cancelImport(id: String) {
            viewModelScope.launch { repository.cancelImport(id) }
        }

        private fun delete(id: String) {
            if (id !in localSnapshot.value.musicById) return
            viewModelScope.launch {
                playbackController.state.value.queue
                    .mapIndexedNotNull { index, item -> index.takeIf { item.id == id } }
                    .asReversed()
                    .forEach { index -> playbackController.remove(index) }
                repository.delete(id)
            }
        }
    }

private data class LocalMusicControls(
    val query: String = "",
    val sort: LocalMusicSortUi = LocalMusicSortUi.Newest,
    val pendingDeleteSongId: String? = null,
)

private data class LocalMusicSnapshot(
    val uiState: LocalMusicUiState = LocalMusicUiState(),
    val musicById: Map<String, LocalMusic> = emptyMap(),
    val visibleMusic: List<LocalMusic> = emptyList(),
)

private data class DeviceImportSnapshot(
    val uiState: DeviceImportUiState = DeviceImportUiState.PermissionRequired,
    val candidatesById: Map<Long, DeviceAudioCandidate> = emptyMap(),
) {
    fun append(candidate: DeviceAudioCandidate): DeviceImportSnapshot {
        val scanning = uiState as? DeviceImportUiState.Scanning ?: return this
        if (candidate.mediaStoreId in candidatesById) return this
        val updatedCandidates = candidatesById + (candidate.mediaStoreId to candidate)
        return copy(
            uiState = DeviceImportUiState.Scanning(updatedCandidates.values.map { candidate -> candidate.toUiModel() }),
            candidatesById = updatedCandidates,
        )
    }
}

private fun buildLocalMusicSnapshot(
    music: List<LocalMusic>,
    imports: List<LocalImportProgress>,
    deviceImport: DeviceImportSnapshot,
    playback: PlaybackState,
    controls: LocalMusicControls,
): LocalMusicSnapshot {
    val visible = music.filterAndSort(controls.query, controls.sort)
    val playingSongId = playback.currentItem?.id
    return LocalMusicSnapshot(
        uiState =
            LocalMusicUiState(
                songs = visible.map { item -> item.toUiModel(isPlaying = item.id == playingSongId) },
                content =
                    when {
                        music.isEmpty() -> LocalMusicContentUi.EmptyLibrary
                        visible.isEmpty() -> LocalMusicContentUi.NoResults
                        else -> LocalMusicContentUi.Songs
                    },
                activeImport = imports.firstNotNullOfOrNull(LocalImportProgress::toActiveUiModel),
                deviceImport = deviceImport.uiState,
                query = controls.query,
                sort = controls.sort,
                playingSongId = playingSongId,
                pendingDeleteSongId = controls.pendingDeleteSongId?.takeIf { id -> music.any { it.id == id } },
            ),
        musicById = music.associateBy(LocalMusic::id),
        visibleMusic = visible,
    )
}

private fun List<LocalMusic>.filterAndSort(
    query: String,
    sort: LocalMusicSortUi,
): List<LocalMusic> =
    filter { item ->
        query.isBlank() ||
            item.title.contains(query, ignoreCase = true) ||
            item.artist.contains(query, ignoreCase = true)
    }.let { filtered ->
        when (sort) {
            LocalMusicSortUi.Newest -> filtered.sortedByDescending(LocalMusic::importedAtEpochMs)
            LocalMusicSortUi.Title -> filtered.sortedBy(LocalMusic::title)
            LocalMusicSortUi.Artist -> filtered.sortedBy(LocalMusic::artist)
            LocalMusicSortUi.Duration -> filtered.sortedByDescending(LocalMusic::durationMs)
        }
    }

private fun LocalMusic.toUiModel(isPlaying: Boolean): LocalSongRowUiModel =
    LocalSongRowUiModel(
        id = id,
        title = title,
        artist = artist,
        durationLabel = formatDurationLabel(durationMs),
        artwork =
            artworkKey
                ?.let { key -> runCatching { LocalArtworkStorageRef(key) }.getOrNull() }
                ?.let(LocalArtworkUiModel::AppStorage)
                ?: LocalArtworkUiModel.None,
        isPlaying = isPlaying,
    )

private fun LocalImportProgress.toActiveUiModel(): LocalImportProgressUiModel? =
    takeIf { progress ->
        progress.state == LocalImportBatchState.Running || progress.state == LocalImportBatchState.Queued
    }?.let { progress ->
        LocalImportProgressUiModel(
            id = progress.batchId,
            completedCount = progress.completedCount,
            totalCount = progress.totalCount,
        )
    }

private fun DeviceAudioCandidate.toUiModel(): DeviceCandidateRowUiModel =
    DeviceCandidateRowUiModel(
        id = mediaStoreId,
        title = displayName.substringBeforeLast('.').ifBlank { displayName },
        artist = artist?.takeIf(String::isNotBlank)?.let(DeviceCandidateArtistUi::Known) ?: DeviceCandidateArtistUi.Unknown,
        durationLabel = formatDurationLabel(durationMs),
    )

private fun formatDurationLabel(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1_000
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "${totalSeconds / 60}:$seconds"
}

private fun LocalMusic.toPlaybackItem() =
    PlaybackItem(
        id = id,
        title = title,
        artist = artist,
        albumTitle = albumTitle,
        source = PlaybackSource.ImportedLocal(id),
        artwork = artworkKey?.let { key -> runCatching { PlaybackArtwork.AppFile(key) }.getOrNull() },
    )
