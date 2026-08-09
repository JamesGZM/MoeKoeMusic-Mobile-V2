package cn.james.music.core.database.home

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.james.music.core.database.MoeKoeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContentSnapshotDaoTest {
    private lateinit var database: MoeKoeDatabase
    private lateinit var dao: HomeContentSnapshotDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    MoeKoeDatabase::class.java,
                ).build()
        dao = database.homeContentSnapshotDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun upsertReplacesOnlyMatchingPartitionAndDeleteRemovesOnlyTarget() =
        runBlocking {
            dao.upsert(entity(ANONYMOUS_KEY, "first", 10))
            dao.upsert(entity(SIGNED_IN_KEY, "signed-in", 20))
            dao.upsert(entity(ANONYMOUS_KEY, "replacement", 30))

            val anonymous = requireNotNull(dao.observe(ANONYMOUS_KEY).first())
            assertEquals("replacement", anonymous.payloadJson)
            assertEquals(30L, anonymous.updatedAtEpochMs)
            assertEquals("signed-in", dao.find(SIGNED_IN_KEY)?.payloadJson)
            assertFalse(anonymous.toString().contains(ANONYMOUS_KEY))
            assertFalse(anonymous.toString().contains("replacement"))

            dao.delete(ANONYMOUS_KEY)

            assertNull(dao.find(ANONYMOUS_KEY))
            assertEquals("signed-in", dao.find(SIGNED_IN_KEY)?.payloadJson)
        }

    @Test
    fun deleteAllRemovesEveryIdentityPartition() =
        runBlocking {
            dao.upsert(entity(ANONYMOUS_KEY, "anonymous", 10))
            dao.upsert(entity(SIGNED_IN_KEY, "signed-in", 20))

            dao.deleteAll()

            assertNull(dao.find(ANONYMOUS_KEY))
            assertNull(dao.find(SIGNED_IN_KEY))
        }

    private fun entity(
        cacheKey: String,
        payloadJson: String,
        updatedAtEpochMs: Long,
    ) = HomeContentSnapshotEntity(
        cacheKey = cacheKey,
        payloadJson = payloadJson,
        schemaVersion = 1,
        updatedAtEpochMs = updatedAtEpochMs,
    )

    private companion object {
        const val ANONYMOUS_KEY = "home:v1:anonymous"
        const val SIGNED_IN_KEY = "home:v1:user:12345"
    }
}
