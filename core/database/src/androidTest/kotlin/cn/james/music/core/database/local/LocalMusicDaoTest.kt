package cn.james.music.core.database.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.james.music.core.database.MoeKoeDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalMusicDaoTest {
    private lateinit var database: MoeKoeDatabase
    private lateinit var dao: LocalMusicDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    MoeKoeDatabase::class.java,
                ).build()
        dao = database.localMusicDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun sizeAndHashRejectDuplicateContent() =
        runBlocking {
            dao.insertMusic(music("first"))

            try {
                dao.insertMusic(music("second"))
                fail("Duplicate content must violate the unique index")
            } catch (_: SQLiteConstraintException) {
                assertEquals("first", requireNotNull(dao.findDuplicate(512, "fixed-hash")).id)
            }
        }

    @Test
    fun cancelMarksQueuedAndCopyingEntriesButKeepsCompletedEntry() =
        runBlocking {
            dao.createBatch(
                LocalImportBatchEntity(
                    id = "batch",
                    source = "DocumentPicker",
                    completionAction = "OpenLocalLibrary",
                    state = "Running",
                    totalCount = 3,
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 1,
                ),
                listOf(
                    entry("queued", "Queued", 0),
                    entry("copying", "Copying", 1),
                    entry("imported", "Imported", 2),
                ),
            )

            dao.cancelPendingEntries("batch")

            assertEquals(
                listOf("Cancelled", "Cancelled", "Imported"),
                dao.entries("batch").map(LocalImportEntryEntity::state),
            )
        }

    private fun music(id: String) =
        LocalMusicEntity(
            id = id,
            storageKey = "$id.audio",
            displayName = "$id.wav",
            title = id,
            artist = "MoeKoe Test Lab",
            albumTitle = null,
            durationMs = 1_000,
            mimeType = "audio/wav",
            sizeBytes = 512,
            sha256 = "fixed-hash",
            artworkKey = null,
            source = "DocumentPicker",
            importedAtEpochMs = 99,
        )

    private fun entry(
        id: String,
        state: String,
        index: Int,
    ) = LocalImportEntryEntity(
        id = id,
        batchId = "batch",
        orderIndex = index,
        sourceUri = "content://test/$id",
        displayName = "$id.wav",
        expectedSizeBytes = 512,
        state = state,
    )
}
