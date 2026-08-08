package cn.james.music.feature.localmusic

import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.core.model.local.LocalMusicSort
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.playback.PlaybackCommandResult
import cn.james.music.playback.PlaybackController
import cn.james.music.playback.PlaybackProgress
import cn.james.music.playback.PlaybackState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalMusicViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun scanResultBelongsToCompletedSelectionState() =
        runTest(dispatcher) {
            val candidate = DeviceAudioCandidate(7, "fixture.mp3", "MoeKoe", 1_000, 2_000)
            val viewModel = LocalMusicViewModel(FakeRepository(scanResult = listOf(candidate)), FakePlaybackController())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            viewModel.openImporter(hasPermission = true)
            advanceUntilIdle()

            assertEquals(DeviceImportUiState.Selection(listOf(candidate)), viewModel.state.value.deviceImport)
        }

    @Test
    fun scanningAndSelectionAreMutuallyExclusive() =
        runTest(dispatcher) {
            val first = DeviceAudioCandidate(7, "first.mp3", "MoeKoe", 1_000, 2_000)
            val second = DeviceAudioCandidate(9, "second.mp3", "MoeKoe", 2_000, 3_000)
            val finishScan = CompletableDeferred<Unit>()
            val repository =
                FakeRepository(
                    scanFlow =
                        flow {
                            emit(first)
                            emit(second)
                            finishScan.await()
                        },
                )
            val viewModel = LocalMusicViewModel(repository, FakePlaybackController())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            viewModel.openImporter(hasPermission = true)
            runCurrent()

            assertEquals(DeviceImportUiState.Scanning(listOf(first, second)), viewModel.state.value.deviceImport)
            viewModel.toggleCandidate(first.mediaStoreId)
            viewModel.toggleAllCandidates()
            runCurrent()
            assertEquals(DeviceImportUiState.Scanning(listOf(first, second)), viewModel.state.value.deviceImport)

            finishScan.complete(Unit)
            advanceUntilIdle()

            assertEquals(DeviceImportUiState.Selection(listOf(first, second)), viewModel.state.value.deviceImport)
        }

    @Test
    fun permissionGrantStartsScanWithoutSecondAction() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val viewModel = LocalMusicViewModel(repository, FakePlaybackController())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            viewModel.openImporter(hasPermission = false)
            assertEquals(DeviceImportUiState.PermissionRequired, viewModel.state.value.deviceImport)
            assertEquals(0, repository.scanCount)

            viewModel.onPermissionResult(granted = true)
            advanceUntilIdle()

            assertEquals(1, repository.scanCount)
            assertEquals(DeviceImportUiState.Selection(emptyList()), viewModel.state.value.deviceImport)
        }

    @Test
    fun localPlayBuildsQueueWithoutRootAppViewModel() =
        runTest(dispatcher) {
            val playback = FakePlaybackController()
            val viewModel = LocalMusicViewModel(FakeRepository(), playback)
            val first = music("first")
            val second = music("second")

            viewModel.play(second, listOf(first, second))
            advanceUntilIdle()

            assertEquals(listOf("first", "second"), playback.replacedQueue.map(PlaybackItem::id))
            assertEquals(1, playback.startIndex)
        }

    @Test
    fun searchMatchesTitleAndArtistIgnoringCase() {
        val titleMatch = music(id = "title", title = "Two Faced", artist = "Linkin Park")
        val artistMatch = music(id = "artist", title = "Numb", artist = "LINKIN PARK")
        val unrelated = music(id = "other", title = "Whose Blue", artist = "Y 2025")

        val result = filterAndSortMusic(listOf(titleMatch, artistMatch, unrelated), "linkin", LocalMusicSort.Title)

        assertEquals(listOf("artist", "title"), result.map(LocalMusic::id))
    }

    @Test
    fun sortOptionsKeepTheirDomainOrdering() {
        val first = music(id = "first", title = "C", artist = "B", durationMs = 2_000, importedAt = 1)
        val second = music(id = "second", title = "A", artist = "C", durationMs = 3_000, importedAt = 3)
        val third = music(id = "third", title = "B", artist = "A", durationMs = 1_000, importedAt = 2)
        val items = listOf(first, second, third)

        assertEquals(listOf("second", "third", "first"), filterAndSortMusic(items, "", LocalMusicSort.Newest).map(LocalMusic::id))
        assertEquals(listOf("second", "third", "first"), filterAndSortMusic(items, "", LocalMusicSort.Title).map(LocalMusic::id))
        assertEquals(listOf("third", "first", "second"), filterAndSortMusic(items, "", LocalMusicSort.Artist).map(LocalMusic::id))
        assertEquals(listOf("second", "first", "third"), filterAndSortMusic(items, "", LocalMusicSort.Duration).map(LocalMusic::id))
    }

    @Test
    fun candidateSelectionLivesOnlyInCompletedSelectionState() =
        runTest(dispatcher) {
            val candidate = DeviceAudioCandidate(7, "fixture.mp3", "MoeKoe", 1_000, 2_000)
            val viewModel = LocalMusicViewModel(FakeRepository(scanResult = listOf(candidate)), FakePlaybackController())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            viewModel.toggleCandidate(candidate.mediaStoreId)
            assertEquals(DeviceImportUiState.PermissionRequired, viewModel.state.value.deviceImport)

            viewModel.openImporter(hasPermission = true)
            advanceUntilIdle()
            viewModel.toggleCandidate(candidate.mediaStoreId)
            runCurrent()

            assertEquals(
                DeviceImportUiState.Selection(listOf(candidate), setOf(candidate.mediaStoreId)),
                viewModel.state.value.deviceImport,
            )
        }

    private fun music(
        id: String,
        title: String = id,
        artist: String = "artist",
        durationMs: Long = 1_000,
        importedAt: Long = 1,
    ) = LocalMusic(id, title, artist, null, durationMs, 2_000, "audio/mpeg", null, importedAt)

    private class FakeRepository(
        scanResult: List<DeviceAudioCandidate> = emptyList(),
        private val scanFlow: Flow<DeviceAudioCandidate> = scanResult.asFlow(),
    ) : LocalMusicRepository {
        var scanCount: Int = 0

        override fun observeMusic(): Flow<List<LocalMusic>> = MutableStateFlow(emptyList())

        override fun observeImports(): Flow<List<LocalImportProgress>> = MutableStateFlow(emptyList())

        override fun scanDevice(): Flow<DeviceAudioCandidate> {
            scanCount += 1
            return scanFlow
        }

        override suspend fun cancelImport(batchId: String) = Unit

        override suspend fun delete(localMusicId: String) = Unit
    }

    private class FakePlaybackController : PlaybackController {
        override val state = MutableStateFlow(PlaybackState())
        override val progress = MutableStateFlow(PlaybackProgress())
        var replacedQueue: List<PlaybackItem> = emptyList()
        var startIndex: Int = -1

        override suspend fun replaceQueue(
            items: List<PlaybackItem>,
            startIndex: Int,
            play: Boolean,
        ): PlaybackCommandResult {
            replacedQueue = items
            this.startIndex = startIndex
            return PlaybackCommandResult.Accepted
        }

        override suspend fun append(items: List<PlaybackItem>) = accepted()

        override suspend fun playNext(item: PlaybackItem) = accepted()

        override suspend fun playNow(item: PlaybackItem) = accepted()

        override suspend fun playAt(index: Int) = accepted()

        override suspend fun remove(index: Int) = accepted()

        override suspend fun move(
            fromIndex: Int,
            toIndex: Int,
        ) = accepted()

        override suspend fun clear() = accepted()

        override suspend fun play() = accepted()

        override suspend fun pause() = accepted()

        override suspend fun seekTo(positionMs: Long) = accepted()

        override suspend fun skipNext() = accepted()

        override suspend fun skipPrevious() = accepted()

        override suspend fun setMode(mode: PlaybackMode) = accepted()

        private fun accepted() = PlaybackCommandResult.Accepted
    }
}
