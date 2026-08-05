package cn.james.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.online.Song
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackController
import cn.james.music.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppPlaybackViewModel
    @Inject
    constructor(
        private val playbackController: PlaybackController,
    ) : ViewModel() {
        val state: StateFlow<PlaybackState> =
            playbackController.state.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                PlaybackState(),
            )

        fun play(song: Song) {
            viewModelScope.launch {
                playbackController.playNow(
                    PlaybackItem(
                        id = "kugou:${song.hash}",
                        title = song.title,
                        artist = song.artistName.ifBlank { "未知艺术家" },
                        albumTitle = song.albumTitle,
                        source = PlaybackSource.Kugou(song.hash),
                    ),
                )
            }
        }

        fun togglePlayback() {
            viewModelScope.launch {
                if (state.value.isPlaying) playbackController.pause() else playbackController.play()
            }
        }

        fun playAt(index: Int) {
            viewModelScope.launch { playbackController.playAt(index) }
        }

        fun removeAt(index: Int) {
            viewModelScope.launch { playbackController.remove(index) }
        }

        fun clearQueue() {
            viewModelScope.launch { playbackController.clear() }
        }
    }
