package cn.james.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cn.james.music.core.model.playback.PlaybackItem

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
    Column(
        modifier =
            modifier
                .background(QueueContainer)
                .playerLayoutProbe(PLAYER_PROBE_QUEUE_SHEET, horizontalFraction = 0.25f),
    ) {
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
                        modifier = if (index == 0) Modifier.playerLayoutProbe(PLAYER_PROBE_QUEUE_FIRST_ITEM) else Modifier,
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
