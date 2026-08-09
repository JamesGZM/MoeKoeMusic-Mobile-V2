package cn.james.music

import cn.james.music.core.model.online.Song
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.playback.PlaybackCommandResult
import cn.james.music.playback.PlaybackController
import cn.james.music.playback.PlaybackError
import cn.james.music.playback.PlaybackProgress
import cn.james.music.playback.PlaybackSourceError
import cn.james.music.playback.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppPlaybackViewModelTest {
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
    fun rejectedSourcePublishesTypedNoticeAndBuildsStablePlaybackItem() =
        runTest(dispatcher) {
            val playback = FakePlaybackController(sourceFailure(PlaybackSourceError.NoCopyright))
            val viewModel = AppPlaybackViewModel(playback)

            viewModel.play(song())
            advanceUntilIdle()

            val notice = requireNotNull(viewModel.notice.value)
            assertEquals("该歌曲暂无可用版权", notice.message)
            assertFalse(notice.canRetry)
            assertEquals("kugou:fixture-hash", playback.lastItem?.id)
        }

    @Test
    fun recoverableFailureCanRetryAndStaleDismissDoesNotRemoveNewNotice() =
        runTest(dispatcher) {
            val playback = FakePlaybackController(sourceFailure(PlaybackSourceError.Timeout))
            val viewModel = AppPlaybackViewModel(playback)

            viewModel.play(song())
            advanceUntilIdle()
            val first = requireNotNull(viewModel.notice.value)
            assertTrue(first.canRetry)

            viewModel.retryPlayback()
            advanceUntilIdle()
            val second = requireNotNull(viewModel.notice.value)
            assertTrue(second.id > first.id)

            viewModel.dismissNotice(first.id)
            assertEquals(second, viewModel.notice.value)
            viewModel.dismissNotice(second.id)
            assertNull(viewModel.notice.value)
            assertEquals(2, playback.playNowCalls)
        }

    @Test
    fun forwardsFullscreenPlayerCommandsAndCyclesMode() =
        runTest(dispatcher) {
            val playback = FakePlaybackController(PlaybackCommandResult.Accepted)
            playback.state.value = PlaybackState(mode = PlaybackMode.RepeatAll)
            val viewModel = AppPlaybackViewModel(playback)

            viewModel.seekTo(84_000)
            viewModel.skipPrevious()
            viewModel.skipNext()
            viewModel.cycleMode()
            advanceUntilIdle()

            assertEquals(84_000L, playback.lastSeekPositionMs)
            assertEquals(1, playback.previousCalls)
            assertEquals(1, playback.nextCalls)
            assertEquals(PlaybackMode.RepeatOne, playback.lastMode)
        }

    @Test
    fun rejectedFullscreenPlayerCommandPublishesTypedNotice() =
        runTest(dispatcher) {
            val playback = FakePlaybackController(PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand))
            val viewModel = AppPlaybackViewModel(playback)

            viewModel.skipNext()
            advanceUntilIdle()

            val notice = requireNotNull(viewModel.notice.value)
            assertEquals(PlaybackError.InvalidCommand, notice.error)
            assertEquals("当前操作无法执行", notice.message)
            assertFalse(notice.canRetry)
        }

    @Test
    fun queueMoveAwaitsControllerAndReportsRejectedResult() =
        runTest(dispatcher) {
            val playback = FakePlaybackController(PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand))
            val viewModel = AppPlaybackViewModel(playback)

            assertEquals(PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand), viewModel.move(2, 0))

            assertEquals(2 to 0, playback.lastMove)
            assertEquals(PlaybackError.InvalidCommand, viewModel.notice.value?.error)
        }

    @Test
    fun acceptedQueueMoveReturnsWithoutPublishingNotice() =
        runTest(dispatcher) {
            val playback = FakePlaybackController(PlaybackCommandResult.Accepted)
            val viewModel = AppPlaybackViewModel(playback)

            assertEquals(PlaybackCommandResult.Accepted, viewModel.move(0, 2))

            assertEquals(0 to 2, playback.lastMove)
            assertNull(viewModel.notice.value)
        }

    private fun song() =
        Song(
            id = "fixture-id",
            hash = "fixture-hash",
            title = "Fixture",
            artistName = "Artist",
            albumId = null,
            albumTitle = null,
            durationMs = 1_000,
            artworkUrl = null,
        )

    private fun sourceFailure(reason: PlaybackSourceError) =
        PlaybackCommandResult.Rejected(PlaybackError.SourceUnavailable("kugou:fixture-hash", reason))

    private class FakePlaybackController(
        private val commandResult: PlaybackCommandResult,
    ) : PlaybackController {
        override val state = MutableStateFlow(PlaybackState())
        override val progress = MutableStateFlow(PlaybackProgress())
        var lastItem: PlaybackItem? = null
        var playNowCalls = 0
        var lastSeekPositionMs: Long? = null
        var previousCalls = 0
        var nextCalls = 0
        var lastMode: PlaybackMode? = null
        var lastMove: Pair<Int, Int>? = null

        override suspend fun playNow(item: PlaybackItem): PlaybackCommandResult {
            lastItem = item
            playNowCalls += 1
            return commandResult
        }

        override suspend fun replaceQueue(
            items: List<PlaybackItem>,
            startIndex: Int,
            play: Boolean,
        ) = accepted()

        override suspend fun append(items: List<PlaybackItem>) = accepted()

        override suspend fun playNext(item: PlaybackItem) = accepted()

        override suspend fun playAt(index: Int) = accepted()

        override suspend fun remove(index: Int) = accepted()

        override suspend fun move(
            fromIndex: Int,
            toIndex: Int,
        ): PlaybackCommandResult {
            lastMove = fromIndex to toIndex
            return accepted()
        }

        override suspend fun clear() = accepted()

        override suspend fun play() = accepted()

        override suspend fun pause() = accepted()

        override suspend fun seekTo(positionMs: Long): PlaybackCommandResult {
            lastSeekPositionMs = positionMs
            return accepted()
        }

        override suspend fun skipNext(): PlaybackCommandResult {
            nextCalls += 1
            return accepted()
        }

        override suspend fun skipPrevious(): PlaybackCommandResult {
            previousCalls += 1
            return accepted()
        }

        override suspend fun setMode(mode: PlaybackMode): PlaybackCommandResult {
            lastMode = mode
            return accepted()
        }

        private fun accepted() = commandResult
    }
}
