package cn.james.music

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.MoeMiniPlayer
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.feature.player.PlayerArtworkUiModel
import cn.james.music.feature.player.PlayerArtworkStorageRef
import cn.james.music.feature.player.PlayerItemUiModel
import cn.james.music.feature.player.PlayerPlaybackModeUi
import cn.james.music.feature.player.PlayerQueueAction
import cn.james.music.feature.player.PlayerQueueItemUi
import cn.james.music.feature.player.PlayerQueueSheet
import cn.james.music.feature.player.PlayerQueueUiState
import cn.james.music.feature.player.PlayerUiState
import cn.james.music.playback.PlaybackState
import java.io.File

@Composable
internal fun MoeKoeMiniPlayer(
    item: PlaybackItem,
    isPlaying: Boolean,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onQueue: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    MoeMiniPlayer(
        title = item.title,
        artist = item.artist,
        isPlaying = isPlaying,
        progress = progress,
        positionLabel = formatMiniPlayerTime(positionMs),
        durationLabel = formatMiniPlayerTime(durationMs),
        badgeLabel = stringResource(R.string.mini_player_quality_standard),
        playContentDescription = stringResource(R.string.mini_player_play),
        pauseContentDescription = stringResource(R.string.mini_player_pause),
        previousContentDescription = stringResource(R.string.mini_player_previous),
        nextContentDescription = stringResource(R.string.mini_player_next),
        queueContentDescription = stringResource(R.string.mini_player_queue),
        onTogglePlayback = onToggle,
        onPrevious = onPrevious,
        onNext = onNext,
        onOpenQueue = onQueue,
        onOpenPlayer = onOpenPlayer,
        artwork = { PlaybackArtworkImage(item) },
    )
}

internal fun formatMiniPlayerTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
internal fun MoeKoeQueueSheet(
    model: PlayerQueueUiState,
    onEvent: (PlayerQueueAction) -> Unit,
) {
    PlayerQueueSheet(
        model = model,
        onEvent = onEvent,
    )
}

internal fun PlaybackState.toPlayerUiState(): PlayerUiState =
    PlayerUiState(
        item = currentItem?.toPlayerItemUiModel(),
        isPlaying = isPlaying,
        isBuffering = status == cn.james.music.playback.PlaybackStatus.Buffering,
        controlsEnabled = connection == cn.james.music.playback.PlaybackConnectionState.Connected,
        mode = mode.toPlayerPlaybackModeUi(),
    )

internal fun PlaybackState.toPlayerQueueUiState(currentDurationMs: Long): PlayerQueueUiState =
    PlayerQueueUiState(
        items =
            queue.map { item ->
                PlayerQueueItemUi(
                    id = item.id,
                    title = item.title,
                    artist = item.artist,
                    durationLabel =
                        currentDurationMs
                            .takeIf { item.id == currentItem?.id && it > 0 }
                            ?.let(::formatMiniPlayerTime),
                    artwork = item.artwork.toPlayerArtworkUiModel(),
                )
            },
        currentItemId = currentItem?.id,
        mode = mode.toPlayerPlaybackModeUi(),
        sourceLabel = currentItem?.albumTitle?.takeIf(String::isNotBlank),
    )

internal fun PlaybackItem.toPlayerItemUiModel(): PlayerItemUiModel =
    PlayerItemUiModel(
        id = id,
        title = title,
        artist = artist,
        artwork = artwork.toPlayerArtworkUiModel(),
    )

internal fun PlaybackMode.toPlayerPlaybackModeUi(): PlayerPlaybackModeUi =
    when (this) {
        PlaybackMode.Sequential -> PlayerPlaybackModeUi.Sequential
        PlaybackMode.RepeatAll -> PlayerPlaybackModeUi.RepeatAll
        PlaybackMode.RepeatOne -> PlayerPlaybackModeUi.RepeatOne
        PlaybackMode.Shuffle -> PlayerPlaybackModeUi.Shuffle
    }

private fun PlaybackArtwork?.toPlayerArtworkUiModel(): PlayerArtworkUiModel =
    when (this) {
        null -> PlayerArtworkUiModel.None
        is PlaybackArtwork.Remote -> PlayerArtworkUiModel.Remote(value)
        is PlaybackArtwork.AppFile -> PlayerArtworkUiModel.AppStorage(PlayerArtworkStorageRef(value))
    }

internal sealed interface ResolvedPlayerQueueAction {
    data object Dismiss : ResolvedPlayerQueueAction

    data class PlayAt(
        val index: Int,
    ) : ResolvedPlayerQueueAction

    data class RemoveAt(
        val index: Int,
    ) : ResolvedPlayerQueueAction

    data object Clear : ResolvedPlayerQueueAction

    data object ChangeMode : ResolvedPlayerQueueAction
}

internal fun PlaybackState.resolvePlayerQueueAction(action: PlayerQueueAction): ResolvedPlayerQueueAction? =
    when (action) {
        PlayerQueueAction.Dismiss -> ResolvedPlayerQueueAction.Dismiss
        is PlayerQueueAction.Play ->
            queue.indexOfFirst { item -> item.id == action.id }
                .takeIf { it >= 0 }
                ?.let(ResolvedPlayerQueueAction::PlayAt)

        is PlayerQueueAction.Remove ->
            queue.indexOfFirst { item -> item.id == action.id }
                .takeIf { it >= 0 }
                ?.let(ResolvedPlayerQueueAction::RemoveAt)

        PlayerQueueAction.Clear -> ResolvedPlayerQueueAction.Clear
        PlayerQueueAction.ChangeMode -> ResolvedPlayerQueueAction.ChangeMode
    }

@Composable
private fun PlaybackArtworkImage(item: PlaybackItem) {
    val context = LocalContext.current
    val model =
        when (val artwork = item.artwork) {
            is PlaybackArtwork.Remote -> artwork.value
            is PlaybackArtwork.AppFile -> File(context.filesDir, artwork.value)
            null -> null
        }
    AsyncImage(
        model = model,
        contentDescription = "${item.title} 封面",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
