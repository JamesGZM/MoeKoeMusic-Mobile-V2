package cn.james.music.feature.player

import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode

data class PlayerUiState(
    val item: PlaybackItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val controlsEnabled: Boolean = true,
    val mode: PlaybackMode = PlaybackMode.RepeatAll,
)

data class PlayerProgressUiState(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

enum class PlayerPage {
    Cover,
    Lyrics,
}

data class PlayerLyricLineUi(
    val original: String,
    val secondary: String? = null,
    val highlightedCharacterCount: Int = 0,
    val startTimeMs: Long = 0,
)

enum class PlayerLyricsTextSize {
    Standard,
    Large,
    Largest,
}

sealed interface PlayerLyricsUiState {
    data object Loading : PlayerLyricsUiState

    data object Empty : PlayerLyricsUiState

    data object Offline : PlayerLyricsUiState

    data object Error : PlayerLyricsUiState

    data class Content(
        val lines: List<PlayerLyricLineUi>,
        val activeLineIndex: Int,
        val textSize: PlayerLyricsTextSize = PlayerLyricsTextSize.Standard,
    ) : PlayerLyricsUiState
}
