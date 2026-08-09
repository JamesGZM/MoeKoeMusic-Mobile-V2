package cn.james.music

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.LocalMoeMiniPlayerArtworkRenderer
import cn.james.music.core.designsystem.component.MoeMiniPlayer
import cn.james.music.core.designsystem.component.MoeMiniPlayerArtworkRenderer
import cn.james.music.core.designsystem.component.MoeMiniPlayerEvent
import cn.james.music.core.designsystem.component.MoeMiniPlayerSemanticsUi
import cn.james.music.core.designsystem.component.MoeMiniPlayerUiModel
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.feature.player.PlayerArtworkUiModel
import cn.james.music.feature.player.PlayerArtworkStorageRef
import cn.james.music.feature.player.PlayerItemUiModel
import cn.james.music.feature.player.PlayerPlaybackModeUi
import cn.james.music.feature.player.PlayerQueueAction
import cn.james.music.feature.player.PlayerQueueActionResult
import cn.james.music.feature.player.PlayerQueueItemUi
import cn.james.music.feature.player.PlayerQueueMoveRequest
import cn.james.music.feature.player.PlayerQueueMoveResult
import cn.james.music.feature.player.PlayerQueueSheet
import cn.james.music.feature.player.PlayerQueueUiState
import cn.james.music.feature.player.PlayerUiState
import cn.james.music.playback.PlaybackState
import java.io.File

@Composable
internal fun MoeKoeMiniPlayer(
    model: MoeMiniPlayerUiModel,
    item: PlaybackItem,
    onEvent: (MoeMiniPlayerEvent) -> Unit,
) {
    CompositionLocalProvider(
        LocalMoeMiniPlayerArtworkRenderer provides
            CurrentPlaybackArtworkRenderer(
                itemId = item.id,
                artwork = item.artwork,
            ),
    ) {
        MoeMiniPlayer(
            model = model,
            onEvent = onEvent,
        )
    }
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
    onEvent: suspend (PlayerQueueAction) -> PlayerQueueActionResult,
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

internal fun PlaybackState.toMoeMiniPlayerUiModel(
    positionMs: Long,
    durationMs: Long,
    badgeLabel: String,
    semantics: MoeMiniPlayerSemanticsUi,
): MoeMiniPlayerUiModel? {
    val item = currentItem ?: return null
    return MoeMiniPlayerUiModel(
        title = item.title,
        artist = item.artist,
        artworkKey = item.id,
        artworkContentDescription = "${item.title} 封面",
        isPlaying = isPlaying,
        progressFraction =
            if (durationMs > 0) {
                positionMs.toFloat() / durationMs
            } else {
                0f
            },
        positionLabel = formatMiniPlayerTime(positionMs),
        durationLabel = formatMiniPlayerTime(durationMs),
        badgeLabel = badgeLabel,
        canOpenPlayer = true,
        semantics = semantics,
    )
}

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
        is PlayerQueueAction.MoveBefore -> null
    }

internal data class ResolvedPlayerQueueMove(
    val fromIndex: Int,
    val toIndex: Int,
)

internal fun PlaybackState.resolveStableQueueMove(request: PlayerQueueMoveRequest): ResolvedPlayerQueueMove? {
    val fromIndex = queue.indexOfFirst { it.id == request.draggedId }
    if (fromIndex < 0) return null
    val remaining = queue.filterIndexed { index, _ -> index != fromIndex }
    val toIndex =
        when (val beforeId = request.beforeId) {
            null -> remaining.size
            else -> remaining.indexOfFirst { it.id == beforeId }.takeIf { it >= 0 } ?: return null
        }
    if (fromIndex == toIndex) return null
    return ResolvedPlayerQueueMove(fromIndex, toIndex)
}

internal fun cn.james.music.playback.PlaybackCommandResult.toPlayerQueueMoveResult(): PlayerQueueMoveResult =
    when (this) {
        cn.james.music.playback.PlaybackCommandResult.Accepted -> PlayerQueueMoveResult.Accepted
        is cn.james.music.playback.PlaybackCommandResult.Rejected -> PlayerQueueMoveResult.Rejected
    }

private class CurrentPlaybackArtworkRenderer(
    private val itemId: String,
    private val artwork: PlaybackArtwork?,
) : MoeMiniPlayerArtworkRenderer {
    @Composable
    override fun Render(
        artworkKey: String?,
        contentDescription: String?,
    ) {
        if (artworkKey != itemId) return
        val context = LocalContext.current
        val model =
            when (artwork) {
                is PlaybackArtwork.Remote -> artwork.value
                is PlaybackArtwork.AppFile -> File(context.filesDir, artwork.value)
                null -> null
            }
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
