package cn.james.music.data.lyrics

import cn.james.music.core.database.lyrics.LyricsCacheDao
import cn.james.music.core.database.lyrics.LyricsCacheEntity
import cn.james.music.core.model.lyrics.LyricsError
import cn.james.music.core.model.lyrics.LyricsResult
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouRetryDelayer
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class KugouLyricsRepositoryTest {
    @Test
    fun validCacheReturnsWithoutNetworkAndRevalidatesParserVersion() =
        runBlocking {
            val dao = FakeLyricsCacheDao(entity(FIXTURE_KRC_TEXT, parserVersion = 0))
            val transport = QueueTransport()
            val result = repository(dao, transport).getLyrics(KUGOU_SOURCE)

            assertTrue(result is LyricsResult.Success)
            assertEquals(
                "MoeKoe",
                (result as LyricsResult.Success)
                    .document.lines
                    .single()
                    .text,
            )
            assertTrue(transport.requests.isEmpty())
            assertEquals(1, dao.upserts.single().parserVersion)
        }

    @Test
    fun corruptCacheIsDeletedThenSuccessfulNetworkResultIsCached() =
        runBlocking {
            val dao = FakeLyricsCacheDao(entity("not-krc"))
            val transport = QueueTransport(searchSuccess(), downloadSuccess())
            val result = repository(dao, transport).getLyrics(KUGOU_SOURCE)

            assertTrue(result is LyricsResult.Success)
            assertEquals(listOf(SOURCE_KEY), dao.deletedKeys)
            assertEquals(FIXTURE_KRC_TEXT, dao.upserts.single().krcText)
            assertEquals(listOf("/search", "/download"), transport.requests.map { it.path })
        }

    @Test
    fun notFoundAndNetworkFailureAreNotPersisted() =
        runBlocking {
            val notFoundDao = FakeLyricsCacheDao()
            val notFound = repository(notFoundDao, QueueTransport(searchNotFound())).getLyrics(KUGOU_SOURCE)
            val offlineDao = FakeLyricsCacheDao()
            val offline =
                repository(
                    offlineDao,
                    QueueTransport(KugouTransportResult.Failure(KugouError.Network(KugouError.Network.Kind.Offline))),
                ).getLyrics(KUGOU_SOURCE)

            assertEquals(LyricsResult.NotFound, notFound)
            assertEquals(LyricsResult.Failure(LyricsError.Offline), offline)
            assertTrue(notFoundDao.upserts.isEmpty())
            assertTrue(offlineDao.upserts.isEmpty())
        }

    @Test
    fun unsupportedAndMalformedSourcesDoNotTouchStorageOrNetwork() =
        runBlocking {
            val dao = FakeLyricsCacheDao()
            val transport = QueueTransport()
            val repository = repository(dao, transport)

            val unsupported = repository.getLyrics(PlaybackSource.FoundationDemo)
            val malformed = repository.getLyrics(PlaybackSource.Kugou("not-a-kugou-hash"))

            assertEquals(LyricsResult.Failure(LyricsError.UnsupportedSource), unsupported)
            assertEquals(LyricsResult.Failure(LyricsError.Protocol), malformed)
            assertTrue(dao.findKeys.isEmpty())
            assertTrue(transport.requests.isEmpty())
        }

    @Test
    fun concurrentRequestsForSameHashShareOneNetworkFlight() =
        runBlocking {
            val dao = FakeLyricsCacheDao()
            val transport = QueueTransport(searchSuccess(), downloadSuccess(), delayMs = 50)
            val repository = repository(dao, transport)

            val results =
                listOf(
                    async { repository.getLyrics(KUGOU_SOURCE) },
                    async { repository.getLyrics(KUGOU_SOURCE) },
                ).awaitAll()

            assertTrue(results.all { it is LyricsResult.Success })
            assertEquals(listOf("/search", "/download"), transport.requests.map { it.path })
            assertEquals(1, dao.upserts.size)
        }

    @Test
    fun cancellationPropagatesAndDoesNotWriteCache() {
        val dao = FakeLyricsCacheDao()
        val transport = KugouTransport { throw CancellationException("fixture cancellation") }

        assertThrows(CancellationException::class.java) {
            runBlocking { repository(dao, transport).getLyrics(KUGOU_SOURCE) }
        }
        assertTrue(dao.upserts.isEmpty())
    }

    private fun repository(
        dao: LyricsCacheDao,
        transport: KugouTransport,
    ) = KugouLyricsRepository(
        cacheDao = dao,
        onlineClient =
            KugouOnlineClient(
                KugouCallExecutor(
                    requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L }),
                    transport = transport,
                    retryDelayer = KugouRetryDelayer {},
                ),
            ),
        parserDispatcher = Dispatchers.Unconfined,
    )

    private fun entity(
        krcText: String,
        parserVersion: Int = 1,
    ) = LyricsCacheEntity(
        sourceKey = SOURCE_KEY,
        krcText = krcText,
        parserVersion = parserVersion,
        updatedAtEpochMs = 1,
    )

    private fun searchSuccess() = response("""{"status":200,"candidates":[{"id":42,"accesskey":"fixture-key"}]}""")

    private fun searchNotFound() = response("""{"status":200,"candidates":[]}""")

    private fun downloadSuccess() = response("""{"status":200,"contenttype":0,"content":"$NODE_KRC_VECTOR"}""")

    private fun response(body: String) =
        KugouTransportResult.Success(
            KugouRawResponse(statusCode = 200, headers = emptyMap(), body = body.encodeToByteArray()),
        )

    private class FakeLyricsCacheDao(
        initial: LyricsCacheEntity? = null,
    ) : LyricsCacheDao {
        private val entries = ConcurrentHashMap<String, LyricsCacheEntity>()
        val findKeys = CopyOnWriteArrayList<String>()
        val upserts = CopyOnWriteArrayList<LyricsCacheEntity>()
        val deletedKeys = CopyOnWriteArrayList<String>()

        init {
            initial?.let { entries[it.sourceKey] = it }
        }

        override suspend fun find(sourceKey: String): LyricsCacheEntity? {
            findKeys += sourceKey
            return entries[sourceKey]
        }

        override suspend fun upsert(entity: LyricsCacheEntity) {
            upserts += entity
            entries[entity.sourceKey] = entity
        }

        override suspend fun delete(sourceKey: String) {
            deletedKeys += sourceKey
            entries.remove(sourceKey)
        }
    }

    private class QueueTransport(
        vararg responses: KugouTransportResult,
        private val delayMs: Long = 0,
    ) : KugouTransport {
        private val responses = ArrayDeque(responses.toList())
        val requests = CopyOnWriteArrayList<KugouPreparedRequest>()

        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult {
            requests += request
            if (delayMs > 0) delay(delayMs)
            return responses.removeFirst()
        }
    }

    private companion object {
        const val HASH = "abcdef0123456789abcdef0123456789"
        const val SOURCE_KEY = "kugou:$HASH"
        val KUGOU_SOURCE = PlaybackSource.Kugou(HASH.uppercase())
        const val FIXTURE_KRC_TEXT = "[0,1200]<0,600,0>Moe<600,600,0>Koe\n[language:W10=]"
        const val NODE_KRC_VECTOR = "a3JjMTjb6kGOA0B1Yb6EHB7jXVmQdtGEk33BRuAWDcIyhsCB3IPdg4z2gBP6RnIuFYpuXO5KIg=="
    }
}
