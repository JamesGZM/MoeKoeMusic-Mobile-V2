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
        val withoutItem = queue.filterNot { it.id == item.id }.toMutableList()
        val currentId = queue.getOrNull(currentIndex)?.id
        val adjustedCurrent = withoutItem.indexOfFirst { it.id == currentId }
        val insertionIndex = (adjustedCurrent + 1).coerceIn(0, withoutItem.size)
        withoutItem.add(insertionIndex, item)
        return withoutItem
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
