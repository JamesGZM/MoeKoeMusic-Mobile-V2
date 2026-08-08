package cn.james.music.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import kotlin.math.roundToLong

@Composable
internal fun PlayerTopBar(
    onBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
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
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onMore, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.player_more))
        }
    }
}

@Composable
internal fun PlayerPageIndicator(
    activePage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PlayerPage.entries.size) { page ->
            if (page > 0) Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .size(if (page == activePage) 8.dp else 7.dp)
                    .background(
                        if (page == activePage) PlayerAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
internal fun PlayerControls(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlaybackItem,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    showSecondaryActions: Boolean,
    compactLayout: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compactLayout) 26.dp else 22.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = if (compactLayout) 0.dp else 8.dp)
                    .playerLayoutProbe(PLAYER_PROBE_INFO),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = if (compactLayout) 18.sp else 20.sp,
                    lineHeight = if (compactLayout) 22.sp else 25.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artist,
                    modifier = Modifier.padding(top = 3.dp),
                    style = if (compactLayout) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PlayerQualityBadge(compact = compactLayout)
            }
            IconButton(
                onClick = onFavorite,
                modifier =
                    Modifier
                        .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                        .offset(x = if (compactLayout) 8.dp else 14.dp, y = (-8).dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.player_favorite),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .padding(horizontal = if (compactLayout) 0.dp else 8.dp)
                    .playerLayoutProbe(PLAYER_PROBE_PROGRESS),
        ) {
            PlayerProgress(
                progress = progress,
                itemId = item.id,
                enabled = state.controlsEnabled,
                onSeek = onSeek,
                compact = compactLayout,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(if (compactLayout) 76.dp else 82.dp)
                    .playerLayoutProbe(PLAYER_PROBE_CORE_CONTROLS),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(Icons.Default.Shuffle, R.string.player_mode_shuffle, state.controlsEnabled, onChangeMode)
            PlayerIconButton(Icons.Default.SkipPrevious, R.string.player_previous, state.controlsEnabled, onPrevious)
            Surface(
                onClick = onTogglePlayback,
                enabled = state.controlsEnabled,
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color =
                    if (state.controlsEnabled) {
                        PlayerAccent
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
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            PlayerIconButton(Icons.Default.SkipNext, R.string.player_next, state.controlsEnabled, onNext)
            PlayerModeButton(state.mode, state.controlsEnabled, onChangeMode)
        }
        if (showSecondaryActions) {
            PlayerSecondaryActions(
                onDownload = onDownload,
                onAddToPlaylist = onAddToPlaylist,
                onShare = onShare,
                onOpenQueue = onOpenQueue,
                modifier = Modifier.playerLayoutProbe(PLAYER_PROBE_SECONDARY),
            )
        }
    }
}

@Composable
private fun PlayerQualityBadge(compact: Boolean) {
    Surface(
        modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = PlayerSecondaryContainer,
        contentColor = PlayerSecondaryContent,
    ) {
        Text(
            text = stringResource(R.string.player_quality_standard),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerSecondaryActions(
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 18.dp).height(72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerSecondaryAction(Icons.Default.Download, R.string.player_download, onDownload)
        PlayerSecondaryAction(Icons.AutoMirrored.Filled.PlaylistAdd, R.string.player_add_to_playlist, onAddToPlaylist)
        PlayerSecondaryAction(Icons.Default.Share, R.string.player_share, onShare)
        PlayerSecondaryAction(Icons.AutoMirrored.Filled.QueueMusic, R.string.player_queue, onOpenQueue)
    }
}

@Composable
private fun PlayerSecondaryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = PlayerSecondaryContainer,
            contentColor = PlayerSecondaryContent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, stringResource(label), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerProgress(
    progress: State<PlayerProgressUiState>,
    itemId: String,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    compact: Boolean,
) {
    var draggedPosition by remember(itemId) { mutableStateOf<Long?>(null) }
    val current = progress.value
    val duration = current.durationMs.coerceAtLeast(0)
    val shownPosition = (draggedPosition ?: current.positionMs).coerceIn(0, duration)
    val activeColor = PlayerAccent
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
        modifier =
            Modifier
                .fillMaxWidth()
                .height(if (compact) 34.dp else 40.dp)
                .then(if (compact) Modifier else Modifier.offset(y = 2.dp)),
        thumb = {
            Box(
                Modifier
                    .size(12.dp)
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
    Row(
        modifier = Modifier.fillMaxWidth().then(if (compact) Modifier else Modifier.offset(y = (-6).dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
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
    val trackHeight = 2.dp
    Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
        val centerY = size.height / 2f
        val strokeWidth = trackHeight.toPx()
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
            PlaybackMode.Sequential, PlaybackMode.RepeatAll, PlaybackMode.Shuffle -> Icons.Default.Repeat
            PlaybackMode.RepeatOne -> Icons.Default.RepeatOne
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
