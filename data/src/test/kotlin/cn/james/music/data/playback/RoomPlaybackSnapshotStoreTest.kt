package cn.james.music.data.playback

import cn.james.music.core.database.playback.PlaybackQueueItemEntity
import cn.james.music.core.database.playback.PlaybackSnapshotDao
import cn.james.music.core.database.playback.PlaybackSnapshotEntity
import cn.james.music.core.database.playback.PlaybackSnapshotWithQueue
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPlaybackSnapshotStoreTest {
    private val dao = FakePlaybackSnapshotDao()
    private val store = RoomPlaybackSnapshotStore(dao)

    @Test
    fun saveAndLoadPreservesOrderedQueuePositionAndMode() =
        runBlocking {
            store.save(
                PlaybackSnapshot(
                    queue = listOf(demoItem("one"), demoItem("two")),
                    currentIndex = 1,
                    positionMs = 7_500,
                    mode = PlaybackMode.Shuffle,
                    updatedAtEpochMs = 123,
                ),
            )

            val restored = requireNotNull(store.load())
            assertEquals(listOf("one", "two"), restored.queue.map(PlaybackItem::id))
            assertEquals(1, restored.currentIndex)
            assertEquals(7_500, restored.positionMs)
            assertEquals(PlaybackMode.Shuffle, restored.mode)
        }

    @Test
    fun loadSkipsUnknownSourceAndClampsInvalidValues() =
        runBlocking {
            dao.stored =
                PlaybackSnapshotWithQueue(
                    snapshot = snapshot(currentIndex = 8, positionMs = -1),
                    queue = listOf(entity(1, "unknown", "future_source"), entity(0, "valid", "foundation_demo")),
                )

            val restored = requireNotNull(store.load())
            assertEquals(listOf("valid"), restored.queue.map(PlaybackItem::id))
            assertEquals(0, restored.currentIndex)
            assertEquals(0, restored.positionMs)
        }

    @Test
    fun invalidModeClearsCorruptSnapshot() =
        runBlocking {
            dao.stored =
                PlaybackSnapshotWithQueue(
                    snapshot = snapshot(mode = "FutureMode"),
                    queue = listOf(entity(0, "demo", "foundation_demo")),
                )

            assertNull(store.load())
            assertTrue(dao.wasCleared)
        }

    @Test
    fun noValidQueueEntriesClearsSnapshot() =
        runBlocking {
            dao.stored =
                PlaybackSnapshotWithQueue(
                    snapshot = snapshot(),
                    queue = listOf(entity(0, "unknown", "future_source")),
                )

            assertNull(store.load())
            assertTrue(dao.wasCleared)
        }

    private fun demoItem(id: String) =
        PlaybackItem(
            id = id,
            title = id,
            artist = "MoeKoe Test Lab",
            albumTitle = null,
            source = PlaybackSource.FoundationDemo,
        )

    private fun snapshot(
        currentIndex: Int = 0,
        positionMs: Long = 10,
        mode: String = PlaybackMode.RepeatAll.name,
    ) = PlaybackSnapshotEntity(
        currentIndex = currentIndex,
        positionMs = positionMs,
        mode = mode,
        updatedAtEpochMs = 1,
    )

    private fun entity(
        index: Int,
        id: String,
        sourceType: String,
    ) = PlaybackQueueItemEntity(
        queueIndex = index,
        mediaId = id,
        title = id,
        artist = "MoeKoe Test Lab",
        albumTitle = null,
        sourceType = sourceType,
        sourceValue = null,
    )
}

private class FakePlaybackSnapshotDao : PlaybackSnapshotDao {
    var stored: PlaybackSnapshotWithQueue? = null
    var wasCleared = false

    override suspend fun getSnapshot() = stored

    override suspend fun upsertSnapshot(snapshot: PlaybackSnapshotEntity) {
        stored = PlaybackSnapshotWithQueue(snapshot, stored?.queue.orEmpty())
    }

    override suspend fun insertQueue(items: List<PlaybackQueueItemEntity>) {
        stored = stored?.copy(queue = items)
    }

    override suspend fun deleteQueue() {
        stored = stored?.copy(queue = emptyList())
    }

    override suspend fun deleteSnapshot() {
        stored = null
    }

    override suspend fun replace(
        snapshot: PlaybackSnapshotEntity,
        items: List<PlaybackQueueItemEntity>,
    ) {
        stored = PlaybackSnapshotWithQueue(snapshot, items)
    }

    override suspend fun clear() {
        wasCleared = true
        stored = null
    }
}
