package cn.james.music.core.database.playback

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.james.music.core.database.MoeKoeDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackSnapshotDaoTest {
    private lateinit var database: MoeKoeDatabase
    private lateinit var dao: PlaybackSnapshotDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    MoeKoeDatabase::class.java,
                ).build()
        dao = database.playbackSnapshotDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replaceAtomicallyReplacesSnapshotAndQueue() =
        runBlocking {
            dao.replace(snapshot(currentIndex = 1), listOf(queueItem(0, "old-1"), queueItem(1, "old-2")))
            dao.replace(snapshot(currentIndex = 0), listOf(queueItem(0, "new")))

            val stored = requireNotNull(dao.getSnapshot())
            assertEquals(0, stored.snapshot.currentIndex)
            assertEquals(listOf("new"), stored.queue.sortedBy { it.queueIndex }.map { it.mediaId })
        }

    @Test
    fun clearRemovesSnapshotAndQueue() =
        runBlocking {
            dao.replace(snapshot(currentIndex = 0), listOf(queueItem(0, "demo")))

            dao.clear()

            assertNull(dao.getSnapshot())
        }

    private fun snapshot(currentIndex: Int) =
        PlaybackSnapshotEntity(
            currentIndex = currentIndex,
            positionMs = 4_200,
            mode = "RepeatAll",
            updatedAtEpochMs = 99,
        )

    private fun queueItem(
        index: Int,
        id: String,
    ) = PlaybackQueueItemEntity(
        queueIndex = index,
        mediaId = id,
        title = id,
        artist = "MoeKoe Test Lab",
        albumTitle = null,
        sourceType = "foundation_demo",
        sourceValue = null,
    )
}
