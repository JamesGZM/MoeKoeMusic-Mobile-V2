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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoeDividerEmphasis
import cn.james.music.core.designsystem.component.MoeHorizontalDivider

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlayerQueueSheet(
    model: PlayerQueueUiState,
    onEvent: (PlayerQueueAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(PlayerQueueAction.Dismiss) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier.fillMaxWidth(),
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
                model = model,
                onEvent = onEvent,
                modifier = Modifier.fillMaxWidth().height(contentHeight),
            )
        }
    }
}

@Composable
internal fun PlayerQueueSheetContent(
    model: PlayerQueueUiState,
    onEvent: (PlayerQueueAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(QueueContainer)
                .playerLayoutProbe(PLAYER_PROBE_QUEUE_SHEET, horizontalFraction = 0.25f),
    ) {
        QueueDragHandle()
        QueueHeader(
            state = model,
            onChangeMode = { onEvent(PlayerQueueAction.ChangeMode) },
            onClear = { onEvent(PlayerQueueAction.Clear) },
        )
        model.sourceLabel?.let { QueueSource(it) }
        if (model.items.isEmpty()) {
            QueueEmptyState(Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                itemsIndexed(model.items, key = { _, queueItem -> queueItem.id }) { index, queueItem ->
                    QueueItem(
                        queueItem = queueItem,
                        isCurrent = queueItem.id == model.currentItemId,
                        onClick = { onEvent(PlayerQueueAction.Play(queueItem.id)) },
                        onRemove = { onEvent(PlayerQueueAction.Remove(queueItem.id)) },
                        modifier = if (index == 0) Modifier.playerLayoutProbe(PLAYER_PROBE_QUEUE_FIRST_ITEM) else Modifier,
                    )
                    if (index != model.items.lastIndex) {
                        if (queueItem.id == model.currentItemId) {
                            Spacer(Modifier.height(2.dp))
                        } else {
                            MoeHorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp, end = 21.dp),
                                emphasis = MoeDividerEmphasis.Muted,
                            )
                        }
                    }
                }
            }
        }
        if (LocalDensity.current.fontScale >= 1.3f) {
            Spacer(Modifier.height(20.dp))
        }
        QueueDismissHint { onEvent(PlayerQueueAction.Dismiss) }
    }
}
