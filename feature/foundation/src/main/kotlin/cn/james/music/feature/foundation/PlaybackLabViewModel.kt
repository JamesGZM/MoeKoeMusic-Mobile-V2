package cn.james.music.feature.foundation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackController
import cn.james.music.playback.PlaybackProgress
import cn.james.music.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaybackLabUiState(
    val playback: PlaybackState = PlaybackState(),
    val progress: PlaybackProgress = PlaybackProgress(),
)

sealed interface PlaybackLabAction {
    data object LoadDemo : PlaybackLabAction

    data object TogglePlayback : PlaybackLabAction

    data object Previous : PlaybackLabAction

    data object Next : PlaybackLabAction

    data object Clear : PlaybackLabAction

    data class SeekTo(
        val positionMs: Long,
    ) : PlaybackLabAction

    data class SetMode(
        val mode: PlaybackMode,
    ) : PlaybackLabAction

    data class Remove(
        val index: Int,
    ) : PlaybackLabAction

    data class Move(
        val fromIndex: Int,
        val toIndex: Int,
    ) : PlaybackLabAction
}

@HiltViewModel
class PlaybackLabViewModel
    @Inject
    constructor(
        private val controller: PlaybackController,
    ) : ViewModel() {
        val uiState: StateFlow<PlaybackLabUiState> =
            combine(controller.state, controller.progress, ::PlaybackLabUiState)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = PlaybackLabUiState(),
                )

        fun onAction(action: PlaybackLabAction) {
            viewModelScope.launch {
                when (action) {
                    PlaybackLabAction.LoadDemo -> {
                        controller.replaceQueue(DemoQueue, play = false)
                    }

                    PlaybackLabAction.TogglePlayback -> {
                        if (controller.state.value.isPlaying) controller.pause() else controller.play()
                    }

                    PlaybackLabAction.Previous -> {
                        controller.skipPrevious()
                    }

                    PlaybackLabAction.Next -> {
                        controller.skipNext()
                    }

                    PlaybackLabAction.Clear -> {
                        controller.clear()
                    }

                    is PlaybackLabAction.SeekTo -> {
                        controller.seekTo(action.positionMs)
                    }

                    is PlaybackLabAction.SetMode -> {
                        controller.setMode(action.mode)
                    }

                    is PlaybackLabAction.Remove -> {
                        controller.remove(action.index)
                    }

                    is PlaybackLabAction.Move -> {
                        controller.move(action.fromIndex, action.toIndex)
                    }
                }
            }
        }

        private companion object {
            val DemoQueue =
                listOf(
                    demoItem("foundation-sky", "Skyline Signal"),
                    demoItem("foundation-bloom", "Blue Bloom"),
                    demoItem("foundation-night", "MoeKoe Night"),
                )

            fun demoItem(
                id: String,
                title: String,
            ) = PlaybackItem(
                id = id,
                title = title,
                artist = "MoeKoe Foundation Lab",
                albumTitle = "Stage 2",
                source = PlaybackSource.FoundationDemo,
            )
        }
    }
