package cn.james.music.feature.player

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerLyricsPageTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun errorRetryAndLyricLineClickExposeTypedActions() {
        var retries = 0
        var seek = -1L
        composeRule.setContent {
            PlayerScreenContent(
                state = PlayerUiState(item = PlayerItemUiModel("id", "title", "artist")),
                progress = remember { mutableStateOf(PlayerProgressUiState()) },
                lyricsState = PlayerLyricsUiState.Error,
                lyricsProgress = remember { mutableStateOf(PlayerLyricsProgressUiState()) },
                initialPage = PlayerPage.Lyrics,
                onBack = {},
                onTogglePlayback = {},
                onSeek = {},
                onPrevious = {},
                onNext = {},
                onChangeMode = {},
                onOpenQueue = {},
                onRetryLyrics = { retries++ },
                onLyricClick = { seek = it },
            )
        }
        composeRule.onNodeWithText("重试").performClick()
        assertEquals(1, retries)
        composeRule.setContent {
            PlayerScreenContent(
                state = PlayerUiState(item = PlayerItemUiModel("id", "title", "artist")),
                progress = remember { mutableStateOf(PlayerProgressUiState()) },
                lyricsState = PlayerLyricsUiState.Content(listOf(PlayerLyricLineUi("歌词行", startTimeMs = 1_234))),
                lyricsProgress = remember { mutableStateOf(PlayerLyricsProgressUiState(activeLineIndex = 0)) },
                initialPage = PlayerPage.Lyrics,
                onBack = {},
                onTogglePlayback = {},
                onSeek = {},
                onPrevious = {},
                onNext = {},
                onChangeMode = {},
                onOpenQueue = {},
                onLyricClick = { seek = it },
            )
        }
        composeRule.onNodeWithText("歌词行").performClick()
        assertEquals(1_234L, seek)
    }
}
