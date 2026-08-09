package cn.james.music.feature.player

import androidx.compose.runtime.State
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PlayerDestination

fun NavGraphBuilder.playerDestination(
    dynamicCoverColors: Boolean,
    showLyricsSupplementalText: Boolean,
    state: State<PlayerUiState>,
    progress: State<PlayerProgressUiState>,
    lyricsState: State<PlayerLyricsUiState>,
    lyricsProgress: State<PlayerLyricsProgressUiState>,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onRetryLyrics: () -> Unit,
    onLyricClick: (Long) -> Unit,
    onLyricsPageVisibilityChanged: (Boolean) -> Unit,
) {
    composable<PlayerDestination> {
        PlayerScreen(
            dynamicCoverColors = dynamicCoverColors,
            showLyricsSupplementalText = showLyricsSupplementalText,
            state = state.value,
            progress = progress,
            lyricsState = lyricsState.value,
            lyricsProgress = lyricsProgress,
            onBack = onBack,
            onTogglePlayback = onTogglePlayback,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onChangeMode = onChangeMode,
            onOpenQueue = onOpenQueue,
            onRetryLyrics = onRetryLyrics,
            onLyricClick = onLyricClick,
            onLyricsPageVisibilityChanged = onLyricsPageVisibilityChanged,
        )
    }
}
