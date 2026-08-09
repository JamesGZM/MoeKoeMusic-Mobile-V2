package cn.james.music.feature.player

/**
 * Feature-owned transient reorder state. It never owns the playback queue: authoritative item ids
 * replace local state whenever membership or order changes outside one accepted submission.
 */
internal class PlayerQueueReorderCoordinator {
    private var authoritativeIds: List<String> = emptyList()
    private var originalIds: List<String>? = null
    private var pendingIds: List<String>? = null

    var visibleIds: List<String> = emptyList()
        private set

    var draggedId: String? = null
        private set

    val isReorderInProgress: Boolean
        get() = draggedId != null || pendingIds != null

    val isAwaitingAuthoritativeOrder: Boolean
        get() = pendingIds != null

    fun updateAuthoritative(
        ids: List<String>,
        reorderEnabled: Boolean,
    ) {
        check(ids.distinct().size == ids.size) { "Queue item ids must be stable and unique" }
        when {
            !reorderEnabled -> {
                resetTo(ids)
            }

            pendingIds == ids -> {
                authoritativeIds = ids
                originalIds = null
                pendingIds = null
                visibleIds = ids
            }

            pendingIds != null && ids == originalIds -> {
                authoritativeIds = ids
            }

            originalIds != null && originalIds != ids -> {
                resetTo(ids)
            }

            pendingIds != null && pendingIds != ids -> {
                resetTo(ids)
            }

            else -> {
                authoritativeIds = ids
                if (draggedId == null && pendingIds == null) visibleIds = ids
            }
        }
    }

    fun beginDrag(
        id: String,
        reorderEnabled: Boolean,
    ): Boolean {
        if (!reorderEnabled || isReorderInProgress || id !in authoritativeIds) return false
        originalIds = authoritativeIds
        visibleIds = authoritativeIds
        draggedId = id
        return true
    }

    fun moveDraggedTo(targetIndex: Int): Boolean {
        val dragged = draggedId ?: return false
        val fromIndex = visibleIds.indexOf(dragged)
        if (fromIndex < 0) return false
        val destination = targetIndex.coerceIn(0, visibleIds.lastIndex)
        if (fromIndex == destination) return false
        visibleIds =
            visibleIds.toMutableList().also { ids ->
                ids.removeAt(fromIndex)
                ids.add(destination, dragged)
            }
        return true
    }

    fun drop(): PlayerQueueMoveRequest? {
        val dragged = draggedId ?: return null
        draggedId = null
        if (visibleIds == originalIds) {
            resetTo(authoritativeIds)
            return null
        }
        val index = visibleIds.indexOf(dragged)
        if (index < 0) {
            resetTo(authoritativeIds)
            return null
        }
        pendingIds = visibleIds
        return PlayerQueueMoveRequest(
            draggedId = dragged,
            beforeId = visibleIds.getOrNull(index + 1),
        )
    }

    fun submitAccessibilityMove(
        id: String,
        targetIndex: Int,
        reorderEnabled: Boolean,
    ): PlayerQueueMoveRequest? {
        if (!beginDrag(id, reorderEnabled)) return null
        moveDraggedTo(targetIndex)
        return drop()
    }

    fun onSubmitResult(result: PlayerQueueMoveResult) {
        if (result != PlayerQueueMoveResult.Accepted) resetTo(authoritativeIds)
    }

    fun cancel() = resetTo(authoritativeIds)

    internal fun edgeAutoScrollDirection(
        pointerY: Float,
        viewportTop: Float,
        viewportBottom: Float,
        edgeSize: Float,
    ): Int =
        when {
            pointerY < viewportTop + edgeSize -> -1
            pointerY > viewportBottom - edgeSize -> 1
            else -> 0
        }

    /** Maps the current pointer position to a visible row using its measured LazyList geometry. */
    internal fun targetIndexAt(
        pointerY: Float,
        visibleItems: List<QueueReorderVisibleItem>,
    ): Int? =
        visibleItems
            .minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2).toFloat() - pointerY)
            }?.index

    private fun resetTo(ids: List<String>) {
        authoritativeIds = ids
        originalIds = null
        pendingIds = null
        draggedId = null
        visibleIds = ids
    }
}

internal data class QueueReorderVisibleItem(
    val index: Int,
    val offset: Int,
    val size: Int,
)
