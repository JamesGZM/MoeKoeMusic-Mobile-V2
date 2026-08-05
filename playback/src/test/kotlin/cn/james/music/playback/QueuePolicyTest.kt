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
    fun removingCurrentChoosesNextOrPreviousAtEnd() {
        assertEquals(1, QueuePolicy.indexAfterRemoval(3, 1, 1))
        assertEquals(1, QueuePolicy.indexAfterRemoval(3, 2, 2))
        assertEquals(-1, QueuePolicy.indexAfterRemoval(1, 0, 0))
    }

    private fun item(id: String) =
        PlaybackItem(
            id = id,
            title = "Title $id",
            artist = "MoeKoe",
            source = PlaybackSource.FoundationDemo,
        )
}
