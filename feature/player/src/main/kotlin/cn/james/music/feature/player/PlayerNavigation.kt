package cn.james.music.feature.player

import androidx.compose.runtime.State
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PlayerDestination

fun NavGraphBuilder.playerDestination(
    dynamicCoverColors: Boolean,
    state: State<PlayerUiState>,
    progress: State<PlayerProgressUiState>,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    composable<PlayerDestination> {
        PlayerScreen(
            dynamicCoverColors = dynamicCoverColors,
            state = state.value,
            progress = progress,
            onBack = onBack,
            onTogglePlayback = onTogglePlayback,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onChangeMode = onChangeMode,
            onOpenQueue = onOpenQueue,
        )
    }
}
