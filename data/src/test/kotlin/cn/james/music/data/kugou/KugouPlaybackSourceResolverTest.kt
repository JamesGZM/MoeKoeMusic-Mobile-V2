package cn.james.music.data.kugou

import cn.james.music.core.model.settings.AppSettings
import cn.james.music.core.model.settings.AppSettingsProblem
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsSnapshot
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import cn.james.music.core.model.settings.LyricsTextSizePreference
import cn.james.music.core.model.settings.PlaybackQualityPreference
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouInitializationError
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouRetryDelayer
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import cn.james.music.playback.PlaybackSourceError
import cn.james.music.playback.RemotePlaybackSourceResult
import cn.james.music.playback.ResolvedPlaybackQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KugouPlaybackSourceResolverTest {
    @Test
    fun anonymousResolveUsesOnlyStandardFreePartAndNeverQueriesPrivilege() =
        runBlocking {
            val transport = RecordingTransport(success("""{"status":1,"url":["https://cdn.example/audio.mp3"]}"""))
            val resolver =
                KugouPlaybackSourceResolver(
                    sessionProvider = anonymousProvider(),
                    onlineClient = onlineClient(transport),
                    appSettingsRepository = throwingSettingsRepository(),
                )

            assertEquals(
                RemotePlaybackSourceResult.Resolved(
                    url = "https://cdn.example/audio.mp3",
                    quality = ResolvedPlaybackQuality.Standard,
                ),
                resolver.resolve(" ABCDEF "),
            )
            assertEquals(listOf("song_url"), transport.requests.map(KugouPreparedRequest::id))
            assertEquals("128", transport.requests.single().query["quality"])
            assertEquals("1", transport.requests.single().query["IsFreePart"])
        }

    @Test
    fun loggedInResolveUsesPreferenceOrderedPrivilegeCandidates() =
        runBlocking {
            val transport =
                RecordingTransport(
                    success(privilegeBody("high" to "HIGH", "128" to "STANDARD")),
                    success("""{"status":1,"url":["https://cdn.example/high.mp3"],"extName":"mp3"}"""),
                )
            val resolver = resolver(authenticatedProvider(), transport, PlaybackQualityPreference.HiRes)

            assertEquals(
                RemotePlaybackSourceResult.Resolved(
                    url = "https://cdn.example/high.mp3",
                    quality = ResolvedPlaybackQuality.HiRes,
                ),
                resolver.resolve("original"),
            )
            assertEquals(listOf("privilege_lite", "song_url"), transport.requests.map(KugouPreparedRequest::id))
            assertEquals("high", transport.requests.last().query["quality"])
            assertEquals("0", transport.requests.last().query["IsFreePart"])
            assertEquals("high", transport.requests.last().query["hash"])
        }

    @Test
    fun resolvedCandidateQualityMapsToPlaybackRuntimeQuality() =
        runBlocking {
            listOf(
                "128" to ResolvedPlaybackQuality.Standard,
                "320" to ResolvedPlaybackQuality.High,
                "flac" to ResolvedPlaybackQuality.Lossless,
                "high" to ResolvedPlaybackQuality.HiRes,
                "viper_atmos" to ResolvedPlaybackQuality.ViperAtmos,
                "viper_clear" to ResolvedPlaybackQuality.ViperClear,
                "viper_tape" to ResolvedPlaybackQuality.ViperTape,
            ).forEach { (wireQuality, expectedQuality) ->
                val transport =
                    RecordingTransport(
                        success(privilegeBody(wireQuality to "FIXTURE")),
                        success("""{"status":1,"url":["https://cdn.example/$wireQuality.mp3"]}"""),
                    )
                val resolver = resolver(authenticatedProvider(), transport, PlaybackQualityPreference.ViperTape)

                assertEquals(
                    RemotePlaybackSourceResult.Resolved(
                        url = "https://cdn.example/$wireQuality.mp3",
                        quality = expectedQuality,
                    ),
                    resolver.resolve("original"),
                )
            }
        }

    @Test
    fun sparsePrivilegeCandidatesUseOnlyAvailableFallbackValues() =
        runBlocking {
            val transport =
                RecordingTransport(
                    success(privilegeBody("flac" to "FLAC", "128" to "STANDARD")),
                    success("""{"status":1,"url":["https://cdn.example/flac.mp3"],"extName":"mp3"}"""),
                )
            val resolver = resolver(authenticatedProvider(), transport, PlaybackQualityPreference.ViperTape)

            assertEquals(
                RemotePlaybackSourceResult.Resolved(
                    url = "https://cdn.example/flac.mp3",
                    quality = ResolvedPlaybackQuality.Lossless,
                ),
                resolver.resolve("original"),
            )
            assertEquals("flac", transport.requests.last().query["quality"])
            assertEquals("flac", transport.requests.last().query["hash"])
        }

    @Test
    fun emptyPrivilegeCandidatesFallBackToOriginalHashQualityChain() =
        runBlocking {
            val transport =
                RecordingTransport(
                    success("""{"data":[]}"""),
                    success("""{"status":1,"url":[]}"""),
                    success("""{"status":1,"url":["https://cdn.example/standard.mp3"],"extName":"mp3"}"""),
                )
            val resolver = resolver(authenticatedProvider(), transport, PlaybackQualityPreference.High)

            assertEquals(
                RemotePlaybackSourceResult.Resolved(
                    url = "https://cdn.example/standard.mp3",
                    quality = ResolvedPlaybackQuality.Standard,
                ),
                resolver.resolve("original"),
            )
            assertEquals(listOf("320", "128"), transport.requests.drop(1).map { it.query.getValue("quality") })
            assertEquals(listOf("original", "original"), transport.requests.drop(1).map { it.query.getValue("hash") })
        }

    @Test
    fun vipAndMp4CandidatesContinueToNextQuality() =
        runBlocking {
            val transport =
                RecordingTransport(
                    success(privilegeBody("320" to "HIGH", "128" to "STANDARD")),
                    success("""{"status":1,"url":["https://cdn.example/video.mp4"],"extName":"mp4"}"""),
                    success("""{"status":1,"url":["https://cdn.example/audio.mp3"],"extName":"mp3"}"""),
                )
            val resolver = resolver(authenticatedProvider(), transport, PlaybackQualityPreference.High)

            assertEquals(
                RemotePlaybackSourceResult.Resolved(
                    url = "https://cdn.example/audio.mp3",
                    quality = ResolvedPlaybackQuality.Standard,
                ),
                resolver.resolve("original"),
            )
            assertEquals(listOf("320", "128"), transport.requests.drop(1).map { it.query.getValue("quality") })
        }

    @Test
    fun exhaustedMp4ReturnsProtocolAndExhaustedVipReturnsVipRequired() =
        runBlocking {
            val allMp4 =
                resolver(
                    authenticatedProvider(),
                    RecordingTransport(
                        success(privilegeBody("128" to "STANDARD")),
                        success("""{"status":1,"url":["https://cdn.example/video.mp4"],"extName":"mp4"}"""),
                    ),
                    PlaybackQualityPreference.Standard,
                )
            val allVip =
                resolver(
                    authenticatedProvider(),
                    RecordingTransport(
                        success(privilegeBody("128" to "STANDARD")),
                        success("""{"status":1,"url":[]}"""),
                    ),
                    PlaybackQualityPreference.Standard,
                )

            assertEquals(RemotePlaybackSourceResult.Unavailable(PlaybackSourceError.Protocol), allMp4.resolve("original"))
            assertEquals(RemotePlaybackSourceResult.Unavailable(PlaybackSourceError.VipRequired), allVip.resolve("original"))
        }

    @Test
    fun noCopyrightAndTypedFailuresStopWithoutTryingLowerQuality() =
        runBlocking {
            val noCopyright = assertStopsWith(success("""{"status":3,"url":[]}"""), PlaybackSourceError.NoCopyright)
            assertEquals(listOf("privilege_lite", "song_url"), noCopyright.requests.map(KugouPreparedRequest::id))

            listOf(
                failure(KugouError.Network(KugouError.Network.Kind.Offline)) to PlaybackSourceError.Offline,
                failure(KugouError.Network(KugouError.Network.Kind.Connection)) to PlaybackSourceError.Connection,
                failure(KugouError.Http(400)) to PlaybackSourceError.ServiceUnavailable,
                success("not-json") to PlaybackSourceError.Protocol,
                success("""{"status":0,"error_code":152}""") to PlaybackSourceError.AuthenticationRequired,
                success("""{"status":1}""", headers = mapOf("ssa-code" to listOf("fixture-risk"))) to
                    PlaybackSourceError.VerificationRequired,
            ).forEach { (result, expected) ->
                val transport = assertStopsWith(result, expected)
                assertEquals(listOf("privilege_lite", "song_url"), transport.requests.map(KugouPreparedRequest::id))
            }
        }

    @Test
    fun timeoutStopsAfterTheExistingBoundedEndpointRetryWithoutLowerQualityFallback() =
        runBlocking {
            val timeout = failure(KugouError.Network(KugouError.Network.Kind.Timeout))
            val transport =
                RecordingTransport(
                    success(privilegeBody("320" to "HIGH", "128" to "STANDARD")),
                    timeout,
                    timeout,
                    timeout,
                )
            val resolver = resolver(authenticatedProvider(), transport, PlaybackQualityPreference.High)

            assertEquals(RemotePlaybackSourceResult.Unavailable(PlaybackSourceError.Timeout), resolver.resolve("original"))
            assertEquals(
                listOf("privilege_lite", "song_url", "song_url", "song_url"),
                transport.requests.map(KugouPreparedRequest::id),
            )
            assertEquals(
                setOf("320"),
                transport.requests
                    .drop(1)
                    .map { it.query.getValue("quality") }
                    .toSet(),
            )
        }

    @Test
    fun privilegeRiskStopsBeforeAnySongUrlRequest() =
        runBlocking {
            val transport =
                RecordingTransport(
                    success("""{"status":1}""", headers = mapOf("ssa-code" to listOf("fixture-risk"))),
                )
            val resolver = resolver(authenticatedProvider(), transport, PlaybackQualityPreference.High)

            assertEquals(RemotePlaybackSourceResult.Unavailable(PlaybackSourceError.VerificationRequired), resolver.resolve("original"))
            assertEquals(listOf("privilege_lite"), transport.requests.map(KugouPreparedRequest::id))
        }

    @Test
    fun blankHashDoesNotInitializeOrReadSettingsOrCallTransport() =
        runBlocking {
            var initialized = false
            val transport = RecordingTransport(success("""{"status":1,"url":[]}"""))
            val resolver =
                KugouPlaybackSourceResolver(
                    sessionProvider =
                        object : KugouSessionProvider {
                            override suspend fun initialize(): KugouInitializationResult {
                                initialized = true
                                return anonymousProvider().initialize()
                            }
                        },
                    onlineClient = onlineClient(transport),
                    appSettingsRepository = throwingSettingsRepository(),
                )

            assertEquals(RemotePlaybackSourceResult.Unavailable(PlaybackSourceError.InvalidSource), resolver.resolve("   "))
            assertEquals(false, initialized)
            assertEquals(0, transport.requests.size)
        }

    @Test
    fun authenticatedResolveReadsOnePreferenceSnapshotAfterSessionInitializationAndCancellationPropagates() =
        runBlocking {
            var settingsReads = 0
            val settings =
                settingsRepository(
                    flow {
                        settingsReads += 1
                        emit(AppSettingsSnapshot(AppSettings(playbackQuality = PlaybackQualityPreference.High)))
                        emit(AppSettingsSnapshot(AppSettings(playbackQuality = PlaybackQualityPreference.ViperTape)))
                    },
                )
            val transport =
                RecordingTransport(
                    success(privilegeBody("320" to "HIGH")),
                    success("""{"status":1,"url":["https://cdn.example/high.mp3"]}"""),
                )
            val resolver = KugouPlaybackSourceResolver(authenticatedProvider(), onlineClient(transport), settings)

            assertEquals(
                RemotePlaybackSourceResult.Resolved(
                    url = "https://cdn.example/high.mp3",
                    quality = ResolvedPlaybackQuality.High,
                ),
                resolver.resolve("original"),
            )
            assertEquals(1, settingsReads)
            assertEquals("320", transport.requests.last().query["quality"])

            val cancellingResolver =
                resolver(
                    anonymousProvider(),
                    ThrowingTransport(CancellationException("fixture")),
                    PlaybackQualityPreference.Standard,
                )
            assertThrows(CancellationException::class.java) { runBlocking { cancellingResolver.resolve("original") } }
            Unit
        }

    private suspend fun assertStopsWith(
        response: KugouTransportResult,
        expected: PlaybackSourceError,
    ): RecordingTransport {
        val transport =
            RecordingTransport(
                success(privilegeBody("320" to "HIGH", "128" to "STANDARD")),
                response,
                success("""{"status":1,"url":["https://cdn.example/should-not-run.mp3"]}"""),
            )
        val resolver = resolver(authenticatedProvider(), transport, PlaybackQualityPreference.High)
        assertEquals(RemotePlaybackSourceResult.Unavailable(expected), resolver.resolve("original"))
        return transport
    }

    private fun resolver(
        provider: KugouSessionProvider,
        transport: KugouTransport,
        preference: PlaybackQualityPreference,
    ) = KugouPlaybackSourceResolver(
        provider,
        onlineClient(transport),
        settingsRepository(flowOf(AppSettingsSnapshot(AppSettings(playbackQuality = preference)))),
    )

    private fun onlineClient(transport: KugouTransport) =
        KugouOnlineClient(
            KugouCallExecutor(
                requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L }),
                transport = transport,
                retryDelayer = KugouRetryDelayer {},
            ),
        )

    private fun anonymousProvider() = sessionProvider()

    private fun authenticatedProvider() = sessionProvider(token = "fixture-token", userId = "7")

    private fun sessionProvider(
        token: String? = null,
        userId: String? = null,
    ) = object : KugouSessionProvider {
        override suspend fun initialize() =
            KugouInitializationResult.Ready(
                KugouSessionSnapshot(
                    identity = KugouDeviceIdentity("fixture-guid", "fixture-mid"),
                    dfid = "fixture-dfid",
                    token = token,
                    userId = userId,
                ),
            )
    }

    private fun settingsRepository(settings: Flow<AppSettingsSnapshot>) =
        object : AppSettingsRepository {
            override val settings: Flow<AppSettingsSnapshot> = settings

            override suspend fun setTheme(theme: AppThemePreference) = AppSettingsUpdateResult.Success

            override suspend fun setPlaybackQuality(quality: PlaybackQualityPreference) = AppSettingsUpdateResult.Success

            override suspend fun setAutoSkipFailedPlayback(enabled: Boolean) = AppSettingsUpdateResult.Success

            override suspend fun setDynamicCoverColors(enabled: Boolean) = AppSettingsUpdateResult.Success

            override suspend fun setShowLyricsSupplementalText(enabled: Boolean) = AppSettingsUpdateResult.Success

            override suspend fun setLyricsTextSize(size: LyricsTextSizePreference) = AppSettingsUpdateResult.Success
        }

    private fun throwingSettingsRepository() = settingsRepository(flow { throw IllegalStateException("Settings must not be read") })

    private fun privilegeBody(vararg candidates: Pair<String, String>) =
        candidates.joinToString(prefix = "{\"data\":[", postfix = "]}") { (quality, hash) ->
            "{\"hash\":\"$hash\",\"quality\":\"$quality\",\"level\":1}"
        }

    private fun success(
        body: String,
        headers: Map<String, List<String>> = emptyMap(),
    ) = KugouTransportResult.Success(KugouRawResponse(200, headers, body.encodeToByteArray()))

    private fun failure(error: KugouError) =
        when (error) {
            is KugouError.Network -> KugouTransportResult.Failure(error)
            else -> errorResponse(error)
        }

    private fun errorResponse(error: KugouError): KugouTransportResult =
        when (error) {
            is KugouError.Http -> KugouTransportResult.Success(KugouRawResponse(error.statusCode, emptyMap(), ByteArray(0)))
            else -> error("Only network and HTTP fixtures are supported")
        }

    private class RecordingTransport(
        vararg results: KugouTransportResult,
    ) : KugouTransport {
        private val queuedResults = ArrayDeque(results.toList())
        val requests = mutableListOf<KugouPreparedRequest>()

        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult {
            requests += request
            return queuedResults.removeFirst()
        }
    }

    private class ThrowingTransport(
        private val throwable: Throwable,
    ) : KugouTransport {
        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult = throw throwable
    }
}
