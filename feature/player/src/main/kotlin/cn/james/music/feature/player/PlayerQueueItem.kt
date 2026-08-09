package cn.james.music.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.designsystem.component.MoeSongRowStyle

@Composable
internal fun QueueItem(
    queueItem: PlayerQueueItemUi,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    reorderEnabled: Boolean,
    reorderDisabledDescription: String,
    onDragStart: () -> Boolean,
    onDragPosition: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MoeSongRow(
        title = queueItem.title,
        subtitle = queueItem.artist,
        metadata = queueItem.durationLabel,
        onClick = onClick,
        style = if (isCurrent) MoeSongRowStyle.QueueCurrent else MoeSongRowStyle.Queue,
        isPlaying = isCurrent,
        showPlayingIndicator = false,
        highlightTitleWhenPlaying = false,
        shape = RoundedCornerShape(8.dp),
        containerColor = if (isCurrent) QueueCurrentContainer else Color.Transparent,
        titleColor = QueueOnSurface,
        subtitleColor = QueueOnSurfaceVariant,
        metadataColor = QueueOnSurfaceVariant,
        playingColor = QueueAccent,
        titleFontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .then(modifier),
        artwork = { QueueArtwork(queueItem) },
        artworkTrailing = {
            if (isCurrent) {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = stringResource(R.string.player_queue_current),
                    modifier = Modifier.padding(start = 9.dp).size(24.dp),
                    tint = QueueAccent,
                )
            }
        },
        trailing = {
            QueueReorderHandle(
                queueItem = queueItem,
                enabled = reorderEnabled,
                disabledDescription = reorderDisabledDescription,
                onDragStart = onDragStart,
                onDragPosition = onDragPosition,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
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
                            contentDescription = stringResource(R.string.player_queue_remove, queueItem.title),
                            modifier = Modifier.size(18.dp),
                            tint = QueueOnSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun QueueReorderHandle(
    queueItem: PlayerQueueItemUi,
    enabled: Boolean,
    disabledDescription: String,
    onDragStart: () -> Boolean,
    onDragPosition: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragActive by remember { mutableStateOf(false) }
    val description =
        if (enabled) {
            stringResource(R.string.player_queue_reorder, queueItem.title)
        } else {
            disabledDescription
        }
    val moveUpLabel = stringResource(R.string.player_queue_move_up)
    val moveDownLabel = stringResource(R.string.player_queue_move_down)
    val actions =
        if (enabled) {
            listOfNotNull(
                onMoveUp?.let { moveUp -> CustomAccessibilityAction(moveUpLabel) { moveUp(); true } },
                onMoveDown?.let { moveDown -> CustomAccessibilityAction(moveDownLabel) { moveDown(); true } },
            )
        } else {
            emptyList()
        }
    Box(
        modifier =
            Modifier
                .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                .onGloballyPositioned { coordinates = it }
                .semantics {
                    contentDescription = description
                    customActions = actions
                    if (!enabled) disabled()
                }.then(
                    if (enabled) {
                        Modifier.pointerInput(queueItem.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragActive = onDragStart() },
                                onDragCancel = {
                                    if (dragActive) onDragCancel()
                                    dragActive = false
                                },
                                onDragEnd = {
                                    if (dragActive) onDragEnd()
                                    dragActive = false
                                },
                                onDrag = { change, _ ->
                                    if (dragActive) {
                                        change.consume()
                                        coordinates?.localToRoot(change.position)?.let(onDragPosition)
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            Icons.Default.DragIndicator,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = QueueOnSurfaceVariant,
        )
    }
}

@Composable
private fun QueueArtwork(
    item: PlayerQueueItemUi,
) {
    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)).background(QueueArtworkPlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = QueueOnSurfaceVariant)
        PlayerArtworkRenderer(model = item.artwork, contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}
