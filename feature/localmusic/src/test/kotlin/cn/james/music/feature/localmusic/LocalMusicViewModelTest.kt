package cn.james.music.feature.localmusic

import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.core.model.playback.PlaybackSource
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun searchMatchesTitleAndArtistIgnoringCase() =
        runTest(dispatcher) {
            val repository =
                FakeRepository(
                    initialMusic =
                        listOf(
                            music(id = "title", title = "Two Faced", artist = "Linkin Park"),
                            music(id = "artist", title = "Numb", artist = "LINKIN PARK"),
                            music(id = "other", title = "Whose Blue", artist = "Y 2025"),
                        ),
                )
            val viewModel = LocalMusicViewModel(repository, FakePlaybackController())
            advanceUntilIdle()

            viewModel.onAction(LocalMusicAction.QueryChanged("linkin"))
            viewModel.onAction(LocalMusicAction.SelectSort(LocalMusicSortUi.Title))
            advanceUntilIdle()

            assertEquals(
                listOf("artist", "title"),
                viewModel.state.value.songs
                    .map(LocalSongRowUiModel::id),
            )
            assertEquals(LocalMusicContentUi.Songs, viewModel.state.value.content)
            assertEquals("linkin", viewModel.state.value.query)
            assertEquals(LocalMusicSortUi.Title, viewModel.state.value.sort)
        }

    @Test
    fun sortOptionsAreDerivedByViewModel() =
        runTest(dispatcher) {
            val repository =
                FakeRepository(
                    initialMusic =
                        listOf(
                            music(id = "first", title = "C", artist = "B", durationMs = 2_000, importedAt = 1),
                            music(id = "second", title = "A", artist = "C", durationMs = 3_000, importedAt = 3),
                            music(id = "third", title = "B", artist = "A", durationMs = 1_000, importedAt = 2),
                        ),
                )
            val viewModel = LocalMusicViewModel(repository, FakePlaybackController())
            advanceUntilIdle()

            val expected =
                mapOf(
                    LocalMusicSortUi.Newest to listOf("second", "third", "first"),
                    LocalMusicSortUi.Title to listOf("second", "third", "first"),
                    LocalMusicSortUi.Artist to listOf("third", "first", "second"),
                    LocalMusicSortUi.Duration to listOf("second", "first", "third"),
                )
            expected.forEach { (sort, ids) ->
                viewModel.onAction(LocalMusicAction.SelectSort(sort))
                advanceUntilIdle()
                assertEquals(
                    ids,
                    viewModel.state.value.songs
                        .map(LocalSongRowUiModel::id),
                )
            }
        }

    @Test
    fun stableIdPlayBuildsQueueFromCurrentVisibleSnapshot() =
        runTest(dispatcher) {
            val playback = FakePlaybackController()
            val repository =
                FakeRepository(
                    initialMusic =
                        listOf(
                            music(id = "hidden", title = "Hidden"),
                            music(id = "second", title = "Match B"),
                            music(id = "first", title = "Match A"),
                        ),
                )
            val viewModel = LocalMusicViewModel(repository, playback)
            advanceUntilIdle()

            viewModel.onAction(LocalMusicAction.QueryChanged("match"))
            viewModel.onAction(LocalMusicAction.SelectSort(LocalMusicSortUi.Title))
            advanceUntilIdle()
            viewModel.onAction(LocalMusicAction.PlaySong("second"))
            advanceUntilIdle()

            assertEquals(listOf("first", "second"), playback.replacedQueue.map(PlaybackItem::id))
            assertEquals(1, playback.startIndex)
        }

    @Test
    fun stableIdDeleteKeepsOnlyIdInDialogAndRemovesEveryQueueOccurrence() =
        runTest(dispatcher) {
            val playback =
                FakePlaybackController(
                    initialState =
                        PlaybackState(
                            queue =
                                listOf(
                                    playbackItem("target"),
                                    playbackItem("other"),
                                    playbackItem("target"),
                                ),
                            currentIndex = 0,
                        ),
                )
            val repository = FakeRepository(initialMusic = listOf(music("target")))
            val viewModel = LocalMusicViewModel(repository, playback)
            advanceUntilIdle()

            viewModel.onAction(LocalMusicAction.RequestDeleteSong("target"))
            advanceUntilIdle()
            assertEquals("target", viewModel.state.value.pendingDeleteSongId)

            viewModel.onAction(LocalMusicAction.ConfirmDeleteSong("target"))
            advanceUntilIdle()

            assertEquals(listOf(2, 0), playback.removedIndices)
            assertEquals(listOf("target"), repository.deletedIds)
            assertEquals(null, viewModel.state.value.pendingDeleteSongId)
        }

    @Test
    fun playbackCurrentItemMapsToPurePlayingState() =
        runTest(dispatcher) {
            val playback = FakePlaybackController()
            val viewModel = LocalMusicViewModel(FakeRepository(initialMusic = listOf(music("playing"))), playback)
            advanceUntilIdle()

            playback.state.value =
                PlaybackState(
                    queue = listOf(playbackItem("playing")),
                    currentIndex = 0,
                    isPlaying = true,
                )
            advanceUntilIdle()

            assertEquals("playing", viewModel.state.value.playingSongId)
            assertTrue(
                viewModel.state.value.songs
                    .single()
                    .isPlaying,
            )
        }

    @Test
    fun importProgressAndCancelAreMappedToStableId() =
        runTest(dispatcher) {
            val repository =
                FakeRepository(
                    initialImports =
                        listOf(
                            LocalImportProgress(
                                batchId = "batch-7",
                                state = LocalImportBatchState.Running,
                                totalCount = 5,
                                completedCount = 2,
                                failedCount = 0,
                                currentDisplayName = "source.mp3",
                                copiedBytes = 1_000,
                                totalBytes = 2_000,
                            ),
                        ),
                )
            val viewModel = LocalMusicViewModel(repository, FakePlaybackController())
            advanceUntilIdle()

            assertEquals(LocalImportProgressUiModel("batch-7", 2, 5), viewModel.state.value.activeImport)

            viewModel.onAction(LocalMusicAction.CancelImport("batch-7"))
            advanceUntilIdle()
            assertEquals(listOf("batch-7"), repository.cancelledIds)
        }

    @Test
    fun localMusicUiStateDoesNotExposeDomainOrRuntimeTypes() {
        val exposedTypes =
            listOf(
                LocalMusicUiState::class.java,
                LocalSongRowUiModel::class.java,
                LocalImportProgressUiModel::class.java,
                DeviceCandidateRowUiModel::class.java,
                DeviceImportUiState.Scanning::class.java,
                DeviceImportUiState.Selection::class.java,
            ).flatMap { type -> type.declaredFields.map { field -> field.genericType } }

        val forbiddenNames =
            listOf(
                "cn.james.music.core.model.local.LocalMusic",
                "cn.james.music.core.model.local.LocalImportProgress",
                "cn.james.music.core.model.local.DeviceAudioCandidate",
                "cn.james.music.playback.PlaybackState",
                "cn.james.music.playback.PlaybackController",
                "cn.james.music.core.model.playback.PlaybackItem",
                "cn.james.music.core.model.local.LocalMusicRepository",
                "java.io.File",
                "android.net.Uri",
                "androidx.compose.ui.unit.Dp",
                "androidx.compose.ui.graphics.Color",
                "androidx.compose.ui.graphics.Shape",
            )
        forbiddenNames.forEach { forbidden ->
            assertFalse(
                "$forbidden leaked through ${exposedTypes.joinToString { type -> type.typeName }}",
                exposedTypes.any { type -> forbidden in type.typeName },
            )
        }
    }

    @Test
    fun scanResultBelongsToCompletedSelectionState() =
        runTest(dispatcher) {
            val candidate = DeviceAudioCandidate(7, "fixture.mp3", "MoeKoe", 61_000, 2_000)
            val viewModel = LocalMusicViewModel(FakeRepository(scanResult = listOf(candidate)), FakePlaybackController())

            viewModel.onDeviceImportAction(DeviceImportAction.Open(hasPermission = true))
            advanceUntilIdle()

            assertEquals(
                DeviceImportUiState.Selection(
                    candidates =
                        listOf(
                            DeviceCandidateRowUiModel(
                                id = 7,
                                title = "fixture",
                                artist = DeviceCandidateArtistUi.Known("MoeKoe"),
                                durationLabel = "1:01",
                            ),
                        ),
                ),
                viewModel.state.value.deviceImport,
            )
        }

    @Test
    fun candidateMapperOwnsTitleDurationAndArtistSemantics() =
        runTest(dispatcher) {
            val candidates =
                listOf(
                    DeviceAudioCandidate(1, "known.flac", "Artist", 218_000, 1),
                    DeviceAudioCandidate(2, "unknown.mp3", null, -1, 1),
                    DeviceAudioCandidate(3, "blank.ogg", "", 1_000, 1),
                )
            val viewModel = LocalMusicViewModel(FakeRepository(scanResult = candidates), FakePlaybackController())

            viewModel.onDeviceImportAction(DeviceImportAction.Open(hasPermission = true))
            advanceUntilIdle()

            val rows = (viewModel.state.value.deviceImport as DeviceImportUiState.Selection).candidates
            assertEquals(listOf("known", "unknown", "blank"), rows.map(DeviceCandidateRowUiModel::title))
            assertEquals(listOf("3:38", "0:00", "0:01"), rows.map(DeviceCandidateRowUiModel::durationLabel))
            assertEquals(DeviceCandidateArtistUi.Known("Artist"), rows[0].artist)
            assertEquals(DeviceCandidateArtistUi.Unknown, rows[1].artist)
            assertEquals(DeviceCandidateArtistUi.Unknown, rows[2].artist)
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

            viewModel.onDeviceImportAction(DeviceImportAction.Open(hasPermission = true))
            runCurrent()

            val scanning = viewModel.state.value.deviceImport as DeviceImportUiState.Scanning
            assertEquals(listOf(7L, 9L), scanning.candidates.map(DeviceCandidateRowUiModel::id))
            viewModel.onDeviceImportAction(DeviceImportAction.ToggleCandidate(7))
            viewModel.onDeviceImportAction(DeviceImportAction.ToggleAll)
            runCurrent()
            assertTrue(viewModel.state.value.deviceImport is DeviceImportUiState.Scanning)

            finishScan.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.deviceImport is DeviceImportUiState.Selection)
        }

    @Test
    fun candidateSelectionLivesOnlyInCompletedSelectionState() =
        runTest(dispatcher) {
            val candidate = DeviceAudioCandidate(7, "fixture.mp3", "MoeKoe", 1_000, 2_000)
            val viewModel = LocalMusicViewModel(FakeRepository(scanResult = listOf(candidate)), FakePlaybackController())

            viewModel.onDeviceImportAction(DeviceImportAction.ToggleCandidate(7))
            assertEquals(DeviceImportUiState.PermissionRequired, viewModel.state.value.deviceImport)

            viewModel.onDeviceImportAction(DeviceImportAction.Open(hasPermission = true))
            advanceUntilIdle()
            viewModel.onDeviceImportAction(DeviceImportAction.ToggleCandidate(7))
            advanceUntilIdle()

            val selection = viewModel.state.value.deviceImport as DeviceImportUiState.Selection
            assertEquals(setOf(7L), selection.selectedIds)
            assertEquals(listOf(7L), selection.candidates.map(DeviceCandidateRowUiModel::id))
        }

    @Test
    fun permissionGrantStartsScanWithoutSecondAction() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val viewModel = LocalMusicViewModel(repository, FakePlaybackController())

            viewModel.onDeviceImportAction(DeviceImportAction.Open(hasPermission = false))
            advanceUntilIdle()
            assertEquals(DeviceImportUiState.PermissionRequired, viewModel.state.value.deviceImport)
            assertEquals(0, repository.scanCount)

            viewModel.onDeviceImportAction(DeviceImportAction.PermissionResult(granted = true))
            advanceUntilIdle()

            assertEquals(1, repository.scanCount)
            assertEquals(DeviceImportUiState.Selection(emptyList()), viewModel.state.value.deviceImport)
        }

    @Test
    fun deniedPermissionKeepsOriginalPermissionState() =
        runTest(dispatcher) {
            val repository = FakeRepository()
            val viewModel = LocalMusicViewModel(repository, FakePlaybackController())

            viewModel.onDeviceImportAction(DeviceImportAction.Open(hasPermission = false))
            viewModel.onDeviceImportAction(DeviceImportAction.PermissionResult(granted = false))
            advanceUntilIdle()

            assertEquals(DeviceImportUiState.PermissionRequired, viewModel.state.value.deviceImport)
            assertEquals(0, repository.scanCount)
        }

    private fun music(
        id: String,
        title: String = id,
        artist: String = "artist",
        durationMs: Long = 1_000,
        importedAt: Long = 1,
    ) = LocalMusic(id, title, artist, null, durationMs, 2_000, "audio/mpeg", null, importedAt)

    private fun playbackItem(id: String) =
        PlaybackItem(
            id = id,
            title = id,
            artist = "artist",
            source = PlaybackSource.ImportedLocal(id),
        )

    private class FakeRepository(
        initialMusic: List<LocalMusic> = emptyList(),
        initialImports: List<LocalImportProgress> = emptyList(),
        scanResult: List<DeviceAudioCandidate> = emptyList(),
        private val scanFlow: Flow<DeviceAudioCandidate> = scanResult.asFlow(),
    ) : LocalMusicRepository {
        private val music = MutableStateFlow(initialMusic)
        private val imports = MutableStateFlow(initialImports)
        var scanCount: Int = 0
        val cancelledIds = mutableListOf<String>()
        val deletedIds = mutableListOf<String>()

        override fun observeMusic(): Flow<List<LocalMusic>> = music

        override fun observeImports(): Flow<List<LocalImportProgress>> = imports

        override fun scanDevice(): Flow<DeviceAudioCandidate> {
            scanCount += 1
            return scanFlow
        }

        override suspend fun cancelImport(batchId: String) {
            cancelledIds += batchId
        }

        override suspend fun delete(localMusicId: String) {
            deletedIds += localMusicId
        }
    }

    private class FakePlaybackController(
        initialState: PlaybackState = PlaybackState(),
    ) : PlaybackController {
        override val state = MutableStateFlow(initialState)
        override val progress = MutableStateFlow(PlaybackProgress())
        var replacedQueue: List<PlaybackItem> = emptyList()
        var startIndex: Int = -1
        val removedIndices = mutableListOf<Int>()

        override suspend fun replaceQueue(
            items: List<PlaybackItem>,
            startIndex: Int,
            play: Boolean,
        ): PlaybackCommandResult {
            replacedQueue = items
            this.startIndex = startIndex
            return accepted()
        }

        override suspend fun append(items: List<PlaybackItem>) = accepted()

        override suspend fun playNext(item: PlaybackItem) = accepted()

        override suspend fun playNow(item: PlaybackItem) = accepted()

        override suspend fun playAt(index: Int) = accepted()

        override suspend fun remove(index: Int): PlaybackCommandResult {
            removedIndices += index
            return accepted()
        }

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
