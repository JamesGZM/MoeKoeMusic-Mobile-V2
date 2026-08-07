package cn.james.music.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import java.io.File

data class PlayerQueueItemUi(
    val item: PlaybackItem,
    val durationLabel: String? = null,
)

data class PlayerQueueUiState(
    val items: List<PlayerQueueItemUi>,
    val currentIndex: Int,
    val mode: PlaybackMode,
    val sourceLabel: String? = null,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlayerQueueSheet(
    state: PlayerQueueUiState,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onChangeMode: () -> Unit,
    artworkContent: (@Composable (PlaybackItem) -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        containerColor = QueueContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val widthScale = (maxWidth / QueueDesignWidth).coerceIn(0.85f, 1.35f)
            val largeTextExpansion = if (LocalDensity.current.fontScale >= 1.3f) 64.dp else 0.dp
            val contentHeight =
                ((QueueContentHeight + largeTextExpansion) * widthScale)
                    .coerceAtLeast(320.dp)
                    .coerceAtMost(maxHeight * 0.68f)
            PlayerQueueSheetContent(
                state = state,
                onDismiss = onDismiss,
                onPlayAt = onPlayAt,
                onRemove = onRemove,
                onClear = onClear,
                onChangeMode = onChangeMode,
                artworkContent = artworkContent,
                modifier = Modifier.fillMaxWidth().height(contentHeight),
            )
        }
    }
}

@Composable
internal fun PlayerQueueSheetContent(
    state: PlayerQueueUiState,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onChangeMode: () -> Unit,
    modifier: Modifier = Modifier,
    artworkContent: (@Composable (PlaybackItem) -> Unit)? = null,
) {
    Column(modifier = modifier.background(QueueContainer)) {
        QueueDragHandle()
        QueueHeader(state = state, onChangeMode = onChangeMode, onClear = onClear)
        state.sourceLabel?.let { QueueSource(it) }
        if (state.items.isEmpty()) {
            QueueEmptyState(Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                itemsIndexed(state.items, key = { _, queueItem -> queueItem.item.id }) { index, queueItem ->
                    QueueItem(
                        queueItem = queueItem,
                        isCurrent = index == state.currentIndex,
                        onClick = { onPlayAt(index) },
                        onRemove = { onRemove(index) },
                        artworkContent = artworkContent,
                    )
                    if (index != state.items.lastIndex) {
                        if (index == state.currentIndex) {
                            Spacer(Modifier.height(2.dp))
                        } else {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp, end = 21.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                            )
                        }
                    }
                }
            }
        }
        if (LocalDensity.current.fontScale >= 1.3f) {
            Spacer(Modifier.height(20.dp))
        }
        QueueDismissHint(onDismiss)
    }
}

@Composable
private fun QueueHeader(
    state: PlayerQueueUiState,
    onChangeMode: () -> Unit,
    onClear: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(if (largeText) 86.dp else 40.dp)
                .padding(start = 21.dp, end = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(if (largeText) 38.dp else 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueTitle(state.items.size)
            Spacer(Modifier.weight(1f))
            if (!largeText) {
                QueueModeButton(state.mode, expanded = false, onClick = onChangeMode)
                QueueClearButton(enabled = state.items.isNotEmpty(), expanded = false, onClick = onClear)
            }
        }
        if (largeText) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QueueModeButton(state.mode, expanded = true, onClick = onChangeMode)
                QueueClearButton(enabled = state.items.isNotEmpty(), expanded = true, onClick = onClear)
            }
        }
    }
}

@Composable
private fun QueueTitle(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.player_queue_title),
            color = QueueOnSurface,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.player_queue_count, count),
            modifier = Modifier.padding(start = 10.dp),
            color = QueueOnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun QueueModeButton(
    mode: PlaybackMode,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(if (expanded) 132.dp else 96.dp)
                .height(48.dp)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(if (expanded) 124.dp else 88.dp).height(if (expanded) 38.dp else 30.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            contentColor = QueueAccent,
            border = BorderStroke(1.dp, QueueAccent.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(queueModeIcon(mode), contentDescription = null, modifier = Modifier.size(14.dp))
                Text(
                    text = stringResource(queueModeLabel(mode)),
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun QueueDragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().height(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(39.dp).height(5.dp),
            shape = CircleShape,
            color = QueueHandle,
        ) {}
    }
}

@Composable
private fun QueueClearButton(
    enabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(if (expanded) 70.dp else 55.dp)
                .height(48.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.player_queue_clear),
            color = if (enabled) QueueAccent else QueueOnSurfaceVariant.copy(alpha = 0.55f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun QueueSource(sourceLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp).padding(start = 21.dp, end = 27.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.player_queue_source, sourceLabel),
            modifier = Modifier.weight(1f),
            color = QueueOnSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = QueueOnSurfaceVariant,
        )
    }
}

@Composable
private fun QueueItem(
    queueItem: PlayerQueueItemUi,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .height(if (isCurrent) 58.dp else 56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrent) QueueCurrentContainer else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(start = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QueueArtwork(queueItem.item, artworkContent)
        if (isCurrent) {
            Icon(
                Icons.Default.Equalizer,
                contentDescription = stringResource(R.string.player_queue_current),
                modifier = Modifier.padding(start = 9.dp).size(24.dp),
                tint = QueueAccent,
            )
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = if (isCurrent) 9.dp else 15.dp, end = 8.dp),
        ) {
            Text(
                text = queueItem.item.title,
                color = QueueOnSurface,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = queueItem.item.artist,
                color = QueueOnSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        queueItem.durationLabel?.let {
            Text(it, color = QueueOnSurfaceVariant, fontSize = 12.sp)
        }
        Icon(
            Icons.Default.DragIndicator,
            contentDescription = stringResource(R.string.player_queue_reorder_unavailable),
            modifier = Modifier.padding(start = 9.dp).size(22.dp),
            tint = QueueOnSurfaceVariant,
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, QueueOutline),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.player_queue_remove, queueItem.item.title),
                        modifier = Modifier.size(18.dp),
                        tint = QueueOnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueArtwork(
    item: PlaybackItem,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
) {
    val context = LocalContext.current
    val model =
        when (val artwork = item.artwork) {
            is PlaybackArtwork.Remote -> artwork.value
            is PlaybackArtwork.AppFile -> File(context.filesDir, artwork.value)
            null -> null
        }
    Box(
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(7.dp)).background(QueueArtworkPlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = QueueOnSurfaceVariant)
        AsyncImage(model = model, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        artworkContent?.invoke(item)
    }
}

@Composable
private fun QueueEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp), tint = QueueAccent)
        Text(
            stringResource(R.string.player_queue_empty),
            modifier = Modifier.padding(top = 12.dp),
            color = QueueOnSurfaceVariant,
        )
    }
}

@Composable
private fun QueueDismissHint(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(29.dp).clickable(role = Role.Button, onClick = onDismiss),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = QueueOnSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.player_queue_swipe_to_close),
            modifier = Modifier.padding(start = 8.dp),
            color = QueueOnSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

private fun queueModeIcon(mode: PlaybackMode): ImageVector =
    when (mode) {
        PlaybackMode.Shuffle -> Icons.Default.Shuffle
        PlaybackMode.RepeatOne -> Icons.Default.RepeatOne
        PlaybackMode.Sequential, PlaybackMode.RepeatAll -> Icons.Default.Repeat
    }

private fun queueModeLabel(mode: PlaybackMode): Int =
    when (mode) {
        PlaybackMode.Sequential -> R.string.player_mode_sequence
        PlaybackMode.RepeatAll -> R.string.player_mode_repeat_all
        PlaybackMode.RepeatOne -> R.string.player_mode_repeat_one
        PlaybackMode.Shuffle -> R.string.player_mode_shuffle
    }

private val QueueContainer = Color(0xFF1A2138)
private val QueueCurrentContainer = Color(0xFF2A3157)
private val QueueAccent = Color(0xFFA49BFF)
private val QueueHandle = Color(0xFFAAA9CE)
private val QueueOnSurface = Color(0xFFF5F2FF)
private val QueueOnSurfaceVariant = Color(0xFFB8B8CF)
private val QueueOutline = Color(0xFF505872)
private val QueueArtworkPlaceholder = Color(0xFF252D48)
private val QueueDesignWidth = 390.dp
private val QueueContentHeight = 469.dp
