package cn.james.music.feature.localmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class LocalMusicUiState(
    val music: List<LocalMusic> = emptyList(),
    val imports: List<LocalImportProgress> = emptyList(),
    val candidates: List<DeviceAudioCandidate> = emptyList(),
)

@HiltViewModel
internal class LocalMusicViewModel
    @Inject
    constructor(
        private val repository: LocalMusicRepository,
        private val playbackController: PlaybackController,
    ) : ViewModel() {
        private val candidates = MutableStateFlow<List<DeviceAudioCandidate>>(emptyList())
        val state: StateFlow<LocalMusicUiState> =
            combine(repository.observeMusic(), repository.observeImports(), candidates, ::LocalMusicUiState)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalMusicUiState())

        fun scanDevice() {
            viewModelScope.launch { candidates.value = repository.scanDevice() }
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
    )
