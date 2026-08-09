package cn.james.music.playback

import cn.james.music.core.model.playback.PlaybackItem

internal object QueuePolicy {
    fun deduplicate(items: List<PlaybackItem>): List<PlaybackItem> = items.distinctBy(PlaybackItem::id)

    fun append(
        queue: List<PlaybackItem>,
        items: List<PlaybackItem>,
    ): List<PlaybackItem> {
        val existingIds = queue.mapTo(mutableSetOf(), PlaybackItem::id)
        return queue + items.filter { existingIds.add(it.id) }
    }

    fun playNext(
        queue: List<PlaybackItem>,
        currentIndex: Int,
        item: PlaybackItem,
    ): List<PlaybackItem> {
        val placement = placeAfterCurrent(queue, currentIndex, item.id)
        return queue.toMutableList().apply {
            when (val existingIndex = placement.existingIndex) {
                null -> {
                    add(placement.targetIndex, item)
                }

                else -> {
                    if (placement.requiresMove) add(placement.targetIndex, removeAt(existingIndex))
                    this[placement.targetIndex] = item
                }
            }
        }
    }

    /**
     * Plans an insertion immediately after the current item. [targetIndex] is the final Media3
     * index after an existing item has been removed, so Media3 can use it without a
     * direction-dependent adjustment.
     */
    fun placeAfterCurrent(
        queue: List<PlaybackItem>,
        currentIndex: Int,
        itemId: String,
    ): QueuePlacement {
        val existingIndex = queue.indexOfFirst { it.id == itemId }.takeIf { it >= 0 }
        if (existingIndex == currentIndex) {
            return QueuePlacement(existingIndex = existingIndex, targetIndex = currentIndex)
        }

        val queueWithoutItem = queue.toMutableList().apply { existingIndex?.let(::removeAt) }
        val currentItemId = queue.getOrNull(currentIndex)?.id
        val targetIndex =
            currentItemId
                ?.let { currentId -> queueWithoutItem.indexOfFirst { it.id == currentId } }
                ?.takeIf { it >= 0 }
                ?.plus(1)
                ?: 0
        return QueuePlacement(
            existingIndex = existingIndex,
            targetIndex = targetIndex.coerceIn(0, queueWithoutItem.size),
        )
    }

    fun indexAfterRemoval(
        queueSizeBeforeRemoval: Int,
        currentIndex: Int,
        removedIndex: Int,
    ): Int {
        val remainingSize = queueSizeBeforeRemoval - 1
        if (remainingSize <= 0) return -1
        return when {
            removedIndex < currentIndex -> currentIndex - 1
            removedIndex == currentIndex -> removedIndex.coerceAtMost(remainingSize - 1)
            else -> currentIndex
        }
    }
}

internal data class QueuePlacement(
    val existingIndex: Int?,
    val targetIndex: Int,
) {
    val requiresMove: Boolean
        get() = existingIndex != null && existingIndex != targetIndex
}
