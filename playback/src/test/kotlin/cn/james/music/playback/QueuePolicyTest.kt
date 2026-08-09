package cn.james.music.playback

import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuePolicyTest {
    @Test
    fun deduplicateKeepsFirstOccurrence() {
        assertEquals(listOf("one", "two"), QueuePolicy.deduplicate(listOf(item("one"), item("two"), item("one"))).map { it.id })
    }

    @Test
    fun appendOnlyAddsFreshItems() {
        val result = QueuePolicy.append(listOf(item("one"), item("two")), listOf(item("two"), item("three")))
        assertEquals(listOf("one", "two", "three"), result.map { it.id })
    }

    @Test
    fun playNextMovesExistingItemAfterCurrent() {
        val result = QueuePolicy.playNext(listOf(item("one"), item("two"), item("three")), 0, item("three"))
        assertEquals(listOf("one", "three", "two"), result.map { it.id })
    }

    @Test
    fun playNextRefreshesTheMovedItemWithThePassedItem() {
        val stale = item("three", title = "Stale title", source = PlaybackSource.FoundationDemo)
        val refreshed = item("three", title = "Refreshed title", source = PlaybackSource.Kugou("updated-hash"))

        val result = QueuePolicy.playNext(listOf(item("one"), item("two"), stale), 0, refreshed)

        assertEquals(listOf("one", "three", "two"), result.map { it.id })
        assertEquals("Refreshed title", result[1].title)
        assertEquals(PlaybackSource.Kugou("updated-hash"), result[1].source)
    }

    @Test
    fun playNowPlacementMovesExistingItemBeforeCurrentAndTargetsItForReplaceAndSeek() {
        val queue = listOf(item("one"), item("two"), item("three"), item("four"))

        val placement = QueuePolicy.placeAfterCurrent(queue, currentIndex = 2, itemId = "one")
        val plannedQueue = QueuePolicy.playNext(queue, currentIndex = 2, item("one"))

        assertEquals(0, placement.existingIndex)
        assertEquals(2, placement.targetIndex)
        assertEquals(listOf("two", "three", "one", "four"), plannedQueue.map { it.id })
        assertEquals("one", plannedQueue[placement.targetIndex].id)
    }

    @Test
    fun playNowPlacementKeepsCurrentItemInPlaceAndTargetsItForReplaceAndSeek() {
        val queue = listOf(item("one"), item("two"), item("three"))

        val placement = QueuePolicy.placeAfterCurrent(queue, currentIndex = 1, itemId = "two")
        val plannedQueue = QueuePolicy.playNext(queue, currentIndex = 1, item("two"))

        assertEquals(1, placement.existingIndex)
        assertEquals(1, placement.targetIndex)
        assertEquals(false, placement.requiresMove)
        assertEquals(listOf("one", "two", "three"), plannedQueue.map { it.id })
        assertEquals("two", plannedQueue[placement.targetIndex].id)
    }

    @Test
    fun playNowPlacementKeepsImmediatelyFollowingItemAtItsTarget() {
        val queue = listOf(item("one"), item("two"), item("three"), item("four"))

        val placement = QueuePolicy.placeAfterCurrent(queue, currentIndex = 1, itemId = "three")
        val plannedQueue = QueuePolicy.playNext(queue, currentIndex = 1, item("three"))

        assertEquals(2, placement.existingIndex)
        assertEquals(2, placement.targetIndex)
        assertEquals(false, placement.requiresMove)
        assertEquals("three", plannedQueue[placement.targetIndex].id)
    }

    @Test
    fun playNowPlacementMovesLaterExistingItemAfterCurrent() {
        val queue = listOf(item("one"), item("two"), item("three"), item("four"))

        val placement = QueuePolicy.placeAfterCurrent(queue, currentIndex = 1, itemId = "four")
        val plannedQueue = QueuePolicy.playNext(queue, currentIndex = 1, item("four"))

        assertEquals(3, placement.existingIndex)
        assertEquals(2, placement.targetIndex)
        assertEquals(listOf("one", "two", "four", "three"), plannedQueue.map { it.id })
        assertEquals("four", plannedQueue[placement.targetIndex].id)
    }

    @Test
    fun playNowPlacementMovesExistingItemAfterLastCurrentItem() {
        val queue = listOf(item("one"), item("two"), item("three"))

        val placement = QueuePolicy.placeAfterCurrent(queue, currentIndex = 2, itemId = "one")
        val plannedQueue = QueuePolicy.playNext(queue, currentIndex = 2, item("one"))

        assertEquals(0, placement.existingIndex)
        assertEquals(2, placement.targetIndex)
        assertEquals(listOf("two", "three", "one"), plannedQueue.map { it.id })
        assertEquals("one", plannedQueue[placement.targetIndex].id)
    }

    @Test
    fun playNowPlacementInsertsNewItemAfterCurrent() {
        val queue = listOf(item("one"), item("two"), item("three"))

        val placement = QueuePolicy.placeAfterCurrent(queue, currentIndex = 1, itemId = "new")
        val plannedQueue = QueuePolicy.playNext(queue, currentIndex = 1, item("new"))

        assertEquals(null, placement.existingIndex)
        assertEquals(2, placement.targetIndex)
        assertEquals(listOf("one", "two", "new", "three"), plannedQueue.map { it.id })
        assertEquals("new", plannedQueue[placement.targetIndex].id)
    }

    @Test
    fun removingCurrentChoosesNextOrPreviousAtEnd() {
        assertEquals(1, QueuePolicy.indexAfterRemoval(3, 1, 1))
        assertEquals(1, QueuePolicy.indexAfterRemoval(3, 2, 2))
        assertEquals(-1, QueuePolicy.indexAfterRemoval(1, 0, 0))
    }

    private fun item(
        id: String,
        title: String = "Title $id",
        source: PlaybackSource = PlaybackSource.FoundationDemo,
    ) = PlaybackItem(
        id = id,
        title = title,
        artist = "MoeKoe",
        source = source,
    )
}
