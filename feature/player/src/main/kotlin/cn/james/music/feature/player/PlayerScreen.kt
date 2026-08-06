package cn.james.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import java.io.File
import kotlin.math.roundToLong

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

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    artworkContent: (@Composable (PlaybackItem) -> Unit)? = null,
) {
    MoeKoeTheme(themeMode = ThemeMode.Dark) {
        val item = state.item
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .background(playerBackground()),
            ) {
                if (item == null) {
                    PlayerEmptyState(onBack = onBack)
                } else {
                    PlayerContent(
                        state = state,
                        progress = progress,
                        item = item,
                        onBack = onBack,
                        onTogglePlayback = onTogglePlayback,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onChangeMode = onChangeMode,
                        onOpenQueue = onOpenQueue,
                        artworkContent = artworkContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerContent(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlaybackItem,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState()),
    ) {
        PlayerTopBar(onBack = onBack)
        Spacer(Modifier.height(MoeKoeTheme.spacing.large))
        PlayerArtwork(
            item = item,
            artworkContent = artworkContent,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MoeKoeTheme.spacing.extraLarge))
        PlayerControls(
            state = state,
            progress = progress,
            item = item,
            onTogglePlayback = onTogglePlayback,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onChangeMode = onChangeMode,
            onOpenQueue = onOpenQueue,
        )
        Spacer(Modifier.height(MoeKoeTheme.spacing.large))
    }
}

@Composable
private fun PlayerTopBar(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(MoeKoeTheme.dimensions.toolbarHeight)
                .padding(horizontal = MoeKoeTheme.spacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.player_collapse))
        }
        Text(
            text = stringResource(R.string.player_title),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget))
    }
}

@Composable
private fun PlayerArtwork(
    item: PlaybackItem,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val model =
        when (val artwork = item.artwork) {
            is PlaybackArtwork.Remote -> artwork.value
            is PlaybackArtwork.AppFile -> File(context.filesDir, artwork.value)
            null -> null
        }
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = MoeKoeTheme.spacing.extraLarge),
        contentAlignment = Alignment.TopCenter,
    ) {
        val artworkSize = maxWidth.coerceAtMost(326.dp)
        Box(
            modifier =
                Modifier
                    .size(artworkSize)
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.player_artwork, item.title),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            artworkContent?.invoke(item)
        }
    }
}

@Composable
private fun PlayerControls(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlaybackItem,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MoeKoeTheme.spacing.large)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.artist,
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.small),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PlayerProgress(
            progress = progress,
            itemId = item.id,
            enabled = state.controlsEnabled,
            onSeek = onSeek,
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(88.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerModeButton(state.mode, state.controlsEnabled, onChangeMode)
            PlayerIconButton(Icons.Default.SkipPrevious, R.string.player_previous, state.controlsEnabled, onPrevious)
            Surface(
                onClick = onTogglePlayback,
                enabled = state.controlsEnabled,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color =
                    if (state.controlsEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                contentColor =
                    if (state.controlsEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
            PlayerIconButton(Icons.Default.SkipNext, R.string.player_next, state.controlsEnabled, onNext)
            PlayerIconButton(Icons.AutoMirrored.Filled.QueueMusic, R.string.player_queue, state.controlsEnabled, onOpenQueue)
        }
        Spacer(Modifier.height(MoeKoeTheme.spacing.small))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerProgress(
    progress: State<PlayerProgressUiState>,
    itemId: String,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
) {
    var draggedPosition by remember(itemId) { mutableStateOf<Long?>(null) }
    val current = progress.value
    val duration = current.durationMs.coerceAtLeast(0)
    val shownPosition = (draggedPosition ?: current.positionMs).coerceIn(0, duration)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    Slider(
        value = shownPosition.toFloat(),
        onValueChange = { draggedPosition = it.roundToLong() },
        onValueChangeFinished = {
            draggedPosition?.let(onSeek)
            draggedPosition = null
        },
        valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
        enabled = enabled && duration > 0,
        modifier = Modifier.fillMaxWidth().padding(top = MoeKoeTheme.spacing.medium),
        thumb = {
            Box(
                Modifier
                    .size(16.dp)
                    .background(
                        color = if (enabled && duration > 0) activeColor else activeColor.copy(alpha = 0.38f),
                        shape = CircleShape,
                    ),
            )
        },
        track = { sliderState ->
            PlayerProgressTrack(
                progress = sliderState.value / duration.coerceAtLeast(1).toFloat(),
                activeColor = if (enabled && duration > 0) activeColor else activeColor.copy(alpha = 0.38f),
                inactiveColor = if (enabled && duration > 0) inactiveColor else inactiveColor.copy(alpha = 0.38f),
            )
        },
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PlayerTime(shownPosition, valid = duration > 0)
        PlayerTime((duration - shownPosition).coerceAtLeast(0), valid = duration > 0, remaining = true)
    }
}

@Composable
private fun PlayerProgressTrack(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val centerY = size.height / 2f
        val strokeWidth = 4.dp.toPx()
        drawLine(
            color = inactiveColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = activeColor,
            start = Offset(0f, centerY),
            end = Offset(size.width * progress.coerceIn(0f, 1f), centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PlayerModeButton(
    mode: PlaybackMode,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val icon =
        when (mode) {
            PlaybackMode.Sequential, PlaybackMode.RepeatAll -> Icons.Default.Repeat
            PlaybackMode.RepeatOne -> Icons.Default.RepeatOne
            PlaybackMode.Shuffle -> Icons.Default.Shuffle
        }
    val label =
        when (mode) {
            PlaybackMode.Sequential -> R.string.player_mode_sequence
            PlaybackMode.RepeatAll -> R.string.player_mode_repeat_all
            PlaybackMode.RepeatOne -> R.string.player_mode_repeat_one
            PlaybackMode.Shuffle -> R.string.player_mode_shuffle
        }
    PlayerIconButton(icon, label, enabled, onClick)
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget),
    ) {
        Icon(icon, stringResource(label), modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard))
    }
}

@Composable
private fun PlayerTime(
    millis: Long,
    valid: Boolean,
    remaining: Boolean = false,
) {
    Text(
        text =
            if (valid) {
                (if (remaining) "−" else "") + formatPlayerTime(millis)
            } else {
                stringResource(R.string.player_unknown_time)
            },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PlayerEmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.player_no_current),
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = MoeKoeTheme.spacing.large)) {
            Text(stringResource(R.string.player_back))
        }
    }
}

@Composable
private fun playerBackground(): Brush {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val background = MaterialTheme.colorScheme.background
    return Brush.radialGradient(
        colorStops =
            arrayOf(
                0f to lerp(background, tertiary, 0.28f),
                0.45f to lerp(background, primary, 0.14f),
                1f to background,
            ),
        center = Offset(180f, 220f),
        radius = 1_100f,
    )
}

internal fun formatPlayerTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
