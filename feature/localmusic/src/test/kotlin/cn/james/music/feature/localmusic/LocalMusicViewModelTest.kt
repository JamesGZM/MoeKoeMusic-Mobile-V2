package cn.james.music.feature.localmusic

import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.playback.PlaybackCommandResult
import cn.james.music.playback.PlaybackController
import cn.james.music.playback.PlaybackProgress
import cn.james.music.playback.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
    fun scanResultBelongsToLocalMusicState() =
        runTest(dispatcher) {
            val candidate = DeviceAudioCandidate(7, "fixture.mp3", "MoeKoe", 1_000, 2_000)
            val viewModel = LocalMusicViewModel(FakeRepository(scanResult = listOf(candidate)), FakePlaybackController())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            viewModel.scanDevice()
            advanceUntilIdle()

            assertEquals(listOf(candidate), viewModel.state.value.candidates)
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

    private fun music(id: String) = LocalMusic(id, id, "artist", null, 1_000, 2_000, "audio/mpeg", null, 1)

    private class FakeRepository(
        private val scanResult: List<DeviceAudioCandidate> = emptyList(),
    ) : LocalMusicRepository {
        override fun observeMusic(): Flow<List<LocalMusic>> = MutableStateFlow(emptyList())

        override fun observeImports(): Flow<List<LocalImportProgress>> = MutableStateFlow(emptyList())

        override suspend fun scanDevice(): List<DeviceAudioCandidate> = scanResult

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
