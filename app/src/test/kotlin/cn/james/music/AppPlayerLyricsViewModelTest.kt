package cn.james.music

import cn.james.music.core.model.lyrics.LyricsDocument
import cn.james.music.core.model.lyrics.LyricsError
import cn.james.music.core.model.lyrics.LyricsLine
import cn.james.music.core.model.lyrics.LyricsRepository
import cn.james.music.core.model.lyrics.LyricsResult
import cn.james.music.core.model.lyrics.LyricsSyllable
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.feature.player.PlayerLyricsUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppPlayerLyricsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun coverDoesNotRequestAndVisibleSuccessIsReused() =
        runTest(dispatcher) {
            val repository = FakeLyricsRepository()
            val viewModel = AppPlayerLyricsViewModel(repository)

            viewModel.updateRequest("one", source, lyricsPageVisible = false)
            runCurrent()
            assertEquals(0, repository.calls)

            viewModel.updateRequest("one", source, lyricsPageVisible = true)
            advanceUntilIdle()
            assertTrue(viewModel.state.value is PlayerLyricsUiState.Content)
            viewModel.updateRequest("one", source, lyricsPageVisible = true)
            assertEquals(1, repository.calls)
        }

    @Test
    fun leavingWhileLoadingCancelsAndLateOldResultCannotReplaceNewItem() =
        runTest(dispatcher) {
            val old = CompletableDeferred<LyricsResult>()
            val repository = FakeLyricsRepository(results = ArrayDeque(listOf(old, LyricsResult.NotFound)))
            val viewModel = AppPlayerLyricsViewModel(repository)

            viewModel.updateRequest("old", source, lyricsPageVisible = true)
            runCurrent()
            viewModel.updateRequest("old", source, lyricsPageVisible = false)
            assertEquals(PlayerLyricsUiState.Unrequested, viewModel.state.value)
            viewModel.updateRequest("new", source, lyricsPageVisible = true)
            advanceUntilIdle()
            old.complete(documentResult)
            advanceUntilIdle()
            assertEquals(PlayerLyricsUiState.Empty, viewModel.state.value)
        }

    @Test
    fun mapsTypedFailuresAndRetryTargetsCurrentItem() =
        runTest(dispatcher) {
            val repository =
                FakeLyricsRepository(
                    results = ArrayDeque(listOf(LyricsResult.Failure(LyricsError.Offline), documentResult)),
                )
            val viewModel = AppPlayerLyricsViewModel(repository)

            viewModel.updateRequest("one", source, lyricsPageVisible = true)
            advanceUntilIdle()
            assertEquals(PlayerLyricsUiState.Offline, viewModel.state.value)
            viewModel.retry()
            advanceUntilIdle()
            assertTrue(viewModel.state.value is PlayerLyricsUiState.Content)
            assertEquals(2, repository.calls)
        }

    @Test
    fun mapperUsesTranslationThenPhoneticAndTypedEmpty() {
        val document =
            LyricsDocument(
                listOf(
                    LyricsLine(listOf(LyricsSyllable("原文", 0, 1_000, "yuanwen")), translation = "译文"),
                    LyricsLine(listOf(LyricsSyllable("第二", 1_000, 2_000, "dier"))),
                ),
            )
        val state = LyricsResult.Success(document).toPlayerLyricsUiState() as PlayerLyricsUiState.Content

        assertEquals("译文", state.lines[0].secondary)
        assertEquals("dier", state.lines[1].secondary)
        assertEquals(PlayerLyricsUiState.Empty, LyricsResult.Failure(LyricsError.UnsupportedSource).toPlayerLyricsUiState())
        assertEquals(PlayerLyricsUiState.Error, LyricsResult.Failure(LyricsError.Protocol).toPlayerLyricsUiState())
    }

    private class FakeLyricsRepository(
        private val results: ArrayDeque<Any> = ArrayDeque(listOf(documentResult)),
    ) : LyricsRepository {
        var calls = 0

        override suspend fun getLyrics(source: PlaybackSource): LyricsResult {
            calls++
            return when (val result = results.removeFirst()) {
                is CompletableDeferred<*> -> result.await() as LyricsResult
                is LyricsResult -> result
                else -> error("Unsupported fixture")
            }
        }
    }

    private companion object {
        val source = PlaybackSource.Kugou("hash")
        val documentResult =
            LyricsResult.Success(
                LyricsDocument(listOf(LyricsLine(listOf(LyricsSyllable("歌词", 0, 1_000, "geci"))))),
            )
    }
}
