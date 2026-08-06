package cn.james.music.data.lyrics

import cn.james.music.core.database.lyrics.LyricsCacheDao
import cn.james.music.core.database.lyrics.LyricsCacheEntity
import cn.james.music.core.model.lyrics.LyricsError
import cn.james.music.core.model.lyrics.LyricsRepository
import cn.james.music.core.model.lyrics.LyricsResult
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouLyricsFetchResult
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.transport.KugouError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KugouLyricsRepository
    @Inject
    constructor(
        private val cacheDao: LyricsCacheDao,
        private val onlineClient: KugouOnlineClient,
        @LyricsParserDispatcher parserDispatcher: CoroutineDispatcher,
    ) : LyricsRepository {
        private val parser = KugouLyricsParser(parserDispatcher)
        private val inFlightMutex = Mutex()
        private val inFlight = mutableMapOf<String, CompletableDeferred<LyricsResult>>()

        override suspend fun getLyrics(source: PlaybackSource): LyricsResult {
            val kugou = source as? PlaybackSource.Kugou ?: return LyricsResult.Failure(LyricsError.UnsupportedSource)
            val hash = kugou.songHash.trim().lowercase()
            if (!KUGOU_HASH.matches(hash)) return LyricsResult.Failure(LyricsError.Protocol)
            val sourceKey = "$KUGOU_SOURCE_PREFIX$hash"
            return singleFlight(sourceKey) { load(sourceKey, hash) }
        }

        private suspend fun singleFlight(
            sourceKey: String,
            loader: suspend () -> LyricsResult,
        ): LyricsResult {
            var ownsFlight = false
            val deferred =
                inFlightMutex.withLock {
                    inFlight[sourceKey]
                        ?: CompletableDeferred<LyricsResult>().also { created ->
                            inFlight[sourceKey] = created
                            ownsFlight = true
                        }
                }
            if (!ownsFlight) return deferred.await()

            return try {
                val result = loader()
                deferred.complete(result)
                result
            } catch (cancellation: CancellationException) {
                deferred.completeExceptionally(cancellation)
                throw cancellation
            } catch (_: Exception) {
                val failure = LyricsResult.Failure(LyricsError.Protocol)
                deferred.complete(failure)
                failure
            } finally {
                inFlightMutex.withLock {
                    if (inFlight[sourceKey] === deferred) inFlight.remove(sourceKey)
                }
            }
        }

        private suspend fun load(
            sourceKey: String,
            hash: String,
        ): LyricsResult {
            storageOrNull { cacheDao.find(sourceKey) }?.let { cached ->
                when (val parsed = parser.parse(cached.krcText)) {
                    KugouLyricsParseResult.Failure -> {
                        storageOrNull { cacheDao.delete(sourceKey) }
                    }

                    is KugouLyricsParseResult.Success -> {
                        if (cached.parserVersion != PARSER_VERSION) {
                            storageOrNull { cacheDao.upsert(cached.currentParserVersion()) }
                        }
                        return LyricsResult.Success(parsed.document)
                    }
                }
            }

            return when (val response = onlineClient.fetchLyrics(hash)) {
                is KugouApiResult.Failure -> LyricsResult.Failure(response.error.toDomain())
                is KugouApiResult.Success -> response.value.toDomain(sourceKey)
            }
        }

        private suspend fun KugouLyricsFetchResult.toDomain(sourceKey: String): LyricsResult =
            when (this) {
                KugouLyricsFetchResult.NotFound -> {
                    LyricsResult.NotFound
                }

                is KugouLyricsFetchResult.Available -> {
                    when (val parsed = parser.parse(source.krcText)) {
                        KugouLyricsParseResult.Failure -> {
                            LyricsResult.Failure(LyricsError.Protocol)
                        }

                        is KugouLyricsParseResult.Success -> {
                            storageOrNull {
                                cacheDao.upsert(
                                    LyricsCacheEntity(
                                        sourceKey = sourceKey,
                                        krcText = source.krcText,
                                        parserVersion = PARSER_VERSION,
                                        updatedAtEpochMs = System.currentTimeMillis().coerceAtLeast(0),
                                    ),
                                )
                            }
                            LyricsResult.Success(parsed.document)
                        }
                    }
                }
            }

        private fun LyricsCacheEntity.currentParserVersion() =
            copy(
                parserVersion = PARSER_VERSION,
                updatedAtEpochMs = System.currentTimeMillis().coerceAtLeast(0),
            )

        private suspend fun <T> storageOrNull(block: suspend () -> T): T? =
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }

        private fun KugouError.toDomain(): LyricsError =
            when (this) {
                is KugouError.Network -> {
                    when (kind) {
                        KugouError.Network.Kind.Offline -> LyricsError.Offline
                        KugouError.Network.Kind.Timeout -> LyricsError.Timeout
                        KugouError.Network.Kind.Connection -> LyricsError.Connection
                    }
                }

                is KugouError.Http -> {
                    LyricsError.ServiceUnavailable
                }

                is KugouError.Protocol,
                is KugouError.Risk,
                -> {
                    LyricsError.Protocol
                }
            }

        private companion object {
            val KUGOU_HASH = Regex("^[0-9a-f]{32}$")
            const val KUGOU_SOURCE_PREFIX = "kugou:"
            const val PARSER_VERSION = 1
        }
    }
