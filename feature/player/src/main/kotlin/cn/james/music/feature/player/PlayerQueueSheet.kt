package cn.james.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoeDividerEmphasis
import cn.james.music.core.designsystem.component.MoeHorizontalDivider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val QUEUE_AUTHORITATIVE_ORDER_TIMEOUT_MS = 2_000L

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlayerQueueSheet(
    model: PlayerQueueUiState,
    onEvent: suspend (PlayerQueueAction) -> PlayerQueueActionResult,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = { scope.launch { onEvent(PlayerQueueAction.Dismiss) } },
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
    onEvent: suspend (PlayerQueueAction) -> PlayerQueueActionResult,
    modifier: Modifier = Modifier,
) {
    val reorderCoordinator = remember { PlayerQueueReorderCoordinator() }
    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    val reorderEnabled = model.mode != PlayerPlaybackModeUi.Shuffle && model.items.size > 1
    val disabledDescriptionRes =
        if (model.mode == PlayerPlaybackModeUi.Shuffle) {
            R.string.player_queue_reorder_disabled_shuffle
        } else {
            R.string.player_queue_reorder_disabled_single_item
        }
    var visibleIds by remember { mutableStateOf(model.items.map(PlayerQueueItemUi::id)) }
    var activeDragId by remember { mutableStateOf<String?>(null) }
    var autoScrollDirection by remember { mutableIntStateOf(0) }
    var dragPointerRootY by remember { mutableStateOf<Float?>(null) }
    var listCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pendingTimeoutJob by remember { mutableStateOf<Job?>(null) }
    val edgeSizePx = with(LocalDensity.current) { 56.dp.toPx() }

    fun syncVisibleIds() {
        visibleIds = reorderCoordinator.visibleIds
    }
    fun submitMove(request: PlayerQueueMoveRequest) {
        scope.launch {
            val result =
                when (val eventResult = onEvent(PlayerQueueAction.MoveBefore(request))) {
                    is PlayerQueueActionResult.Move -> eventResult.result
                    PlayerQueueActionResult.Handled -> PlayerQueueMoveResult.Rejected
                }
            reorderCoordinator.onSubmitResult(result)
            syncVisibleIds()
            pendingTimeoutJob?.cancel()
            if (result == PlayerQueueMoveResult.Accepted && reorderCoordinator.isAwaitingAuthoritativeOrder) {
                pendingTimeoutJob =
                    launch {
                        delay(QUEUE_AUTHORITATIVE_ORDER_TIMEOUT_MS)
                        if (reorderCoordinator.isAwaitingAuthoritativeOrder) {
                            reorderCoordinator.cancel()
                            syncVisibleIds()
                        }
                    }
            }
        }
    }
    fun dispatchEvent(action: PlayerQueueAction) {
        scope.launch { onEvent(action) }
    }
    fun moveDraggedToPointer(rootY: Float) {
        val coordinates = listCoordinates ?: return
        val bounds = coordinates.boundsInRoot()
        dragPointerRootY = rootY
        autoScrollDirection =
            reorderCoordinator.edgeAutoScrollDirection(
                pointerY = rootY,
                viewportTop = bounds.top,
                viewportBottom = bounds.bottom,
                edgeSize = edgeSizePx,
            )
        val targetIndex =
            reorderCoordinator.targetIndexAt(
                pointerY = rootY - bounds.top,
                visibleItems =
                    listState.layoutInfo.visibleItemsInfo.map { item ->
                        QueueReorderVisibleItem(
                            index = item.index,
                            offset = item.offset,
                            size = item.size,
                        )
                    },
            ) ?: return
        if (reorderCoordinator.moveDraggedTo(targetIndex)) syncVisibleIds()
    }

    LaunchedEffect(model.items.map(PlayerQueueItemUi::id), reorderEnabled) {
        reorderCoordinator.updateAuthoritative(model.items.map(PlayerQueueItemUi::id), reorderEnabled)
        syncVisibleIds()
        if (!reorderCoordinator.isAwaitingAuthoritativeOrder) pendingTimeoutJob?.cancel()
    }
    LaunchedEffect(activeDragId, autoScrollDirection) {
        while (activeDragId != null && autoScrollDirection != 0) {
            listState.scrollBy(autoScrollDirection * 12f)
            dragPointerRootY?.let(::moveDraggedToPointer)
            withFrameNanos { }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            pendingTimeoutJob?.cancel()
            reorderCoordinator.cancel()
        }
    }

    val visibleItems = visibleIds.mapNotNull { id -> model.items.firstOrNull { it.id == id } }
    Column(
        modifier =
            modifier
                .background(QueueContainer)
                .playerLayoutProbe(PLAYER_PROBE_QUEUE_SHEET, horizontalFraction = 0.25f),
    ) {
        QueueDragHandle()
        QueueHeader(
            state = model,
            onChangeMode = { dispatchEvent(PlayerQueueAction.ChangeMode) },
            onClear = { dispatchEvent(PlayerQueueAction.Clear) },
        )
        model.sourceLabel?.let { QueueSource(it) }
        if (model.items.isEmpty()) {
            QueueEmptyState(Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).onGloballyPositioned { listCoordinates = it },
            ) {
                itemsIndexed(visibleItems, key = { _, item -> item.id }) { index, queueItem ->
                    QueueItem(
                        queueItem = queueItem,
                        isCurrent = queueItem.id == model.currentItemId,
                        onClick = { dispatchEvent(PlayerQueueAction.Play(queueItem.id)) },
                        onRemove = { dispatchEvent(PlayerQueueAction.Remove(queueItem.id)) },
                        reorderEnabled = reorderEnabled,
                        reorderDisabledDescription = stringResource(disabledDescriptionRes, queueItem.title),
                        onDragStart = {
                            reorderCoordinator.beginDrag(queueItem.id, reorderEnabled).also { started ->
                                if (started) {
                                    activeDragId = queueItem.id
                                    dragPointerRootY = null
                                }
                            }
                        },
                        onDragPosition = { rootPosition -> moveDraggedToPointer(rootPosition.y) },
                        onDragEnd = {
                            autoScrollDirection = 0
                            activeDragId = null
                            dragPointerRootY = null
                            reorderCoordinator.drop()?.let(::submitMove)
                            syncVisibleIds()
                        },
                        onDragCancel = {
                            autoScrollDirection = 0
                            activeDragId = null
                            dragPointerRootY = null
                            reorderCoordinator.cancel()
                            syncVisibleIds()
                        },
                        onMoveUp =
                            if (index > 0) {
                                {
                                    reorderCoordinator
                                        .submitAccessibilityMove(queueItem.id, index - 1, reorderEnabled)
                                        ?.let(::submitMove)
                                    syncVisibleIds()
                                }
                            } else {
                                null
                            },
                        onMoveDown =
                            if (index < visibleItems.lastIndex) {
                                {
                                    reorderCoordinator
                                        .submitAccessibilityMove(queueItem.id, index + 1, reorderEnabled)
                                        ?.let(::submitMove)
                                    syncVisibleIds()
                                }
                            } else {
                                null
                            },
                        modifier = if (index == 0) Modifier.playerLayoutProbe(PLAYER_PROBE_QUEUE_FIRST_ITEM) else Modifier,
                    )
                    if (index != visibleItems.lastIndex) {
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
        QueueDismissHint { dispatchEvent(PlayerQueueAction.Dismiss) }
    }
}
