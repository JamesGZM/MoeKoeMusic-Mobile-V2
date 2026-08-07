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
import cn.james.music.feature.player.PlayerQueueItemUi
import cn.james.music.feature.player.PlayerQueueSheet
import cn.james.music.feature.player.PlayerQueueUiState
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
    state: PlaybackState,
    currentDurationMs: Long,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onChangeMode: () -> Unit,
) {
    PlayerQueueSheet(
        state =
            PlayerQueueUiState(
                items =
                    state.queue.mapIndexed { index, item ->
                        PlayerQueueItemUi(
                            item = item,
                            durationLabel =
                                currentDurationMs
                                    .takeIf { index == state.currentIndex && it > 0 }
                                    ?.let(::formatMiniPlayerTime),
                        )
                    },
                currentIndex = state.currentIndex,
                mode = state.mode,
                sourceLabel = state.currentItem?.albumTitle?.takeIf(String::isNotBlank),
            ),
        onDismiss = onDismiss,
        onPlayAt = onPlayAt,
        onRemove = onRemove,
        onClear = onClear,
        onChangeMode = onChangeMode,
    )
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
