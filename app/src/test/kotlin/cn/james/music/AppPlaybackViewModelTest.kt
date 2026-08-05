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
        private val playNowResult: PlaybackCommandResult,
    ) : PlaybackController {
        override val state = MutableStateFlow(PlaybackState())
        override val progress = MutableStateFlow(PlaybackProgress())
        var lastItem: PlaybackItem? = null
        var playNowCalls = 0

        override suspend fun playNow(item: PlaybackItem): PlaybackCommandResult {
            lastItem = item
            playNowCalls += 1
            return playNowResult
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
