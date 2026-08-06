package cn.james.music.core.database.lyrics

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.james.music.core.database.MoeKoeDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LyricsCacheDaoTest {
    private lateinit var database: MoeKoeDatabase
    private lateinit var dao: LyricsCacheDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    MoeKoeDatabase::class.java,
                ).build()
        dao = database.lyricsCacheDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun upsertReplacesContentAndDeleteRemovesTheEntry() =
        runBlocking {
            dao.upsert(entity(krcText = "first", parserVersion = 1, updatedAt = 10))
            dao.upsert(entity(krcText = "second", parserVersion = 2, updatedAt = 20))

            val cached = requireNotNull(dao.find(SOURCE_KEY))
            assertEquals("second", cached.krcText)
            assertEquals(2, cached.parserVersion)
            assertEquals(20L, cached.updatedAtEpochMs)
            assertFalse(cached.toString().contains("second"))

            dao.delete(SOURCE_KEY)

            assertNull(dao.find(SOURCE_KEY))
        }

    private fun entity(
        krcText: String,
        parserVersion: Int,
        updatedAt: Long,
    ) = LyricsCacheEntity(
        sourceKey = SOURCE_KEY,
        krcText = krcText,
        parserVersion = parserVersion,
        updatedAtEpochMs = updatedAt,
    )

    private companion object {
        const val SOURCE_KEY = "kugou:fixture-hash"
    }
}
