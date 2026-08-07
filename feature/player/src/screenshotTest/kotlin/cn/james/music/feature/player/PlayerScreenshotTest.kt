package cn.james.music.feature.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.core.model.playback.PlaybackSource
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Cover", widthDp = 390, heightDp = 844)
@Composable
fun PlayerCoverScreenshot() {
    PlayerScreenshotContent(showArtwork = true)
}

@PreviewTest
@Preview(name = "CoverFallback", widthDp = 390, heightDp = 844)
@Composable
fun PlayerCoverFallbackScreenshot() {
    PlayerScreenshotContent()
}

@PreviewTest
@Preview(name = "CoverLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun PlayerCoverLargeTextScreenshot() {
    PlayerScreenshotContent(showArtwork = true)
}

@PreviewTest
@Preview(name = "CoverLargestText", widthDp = 390, heightDp = 844, fontScale = 2f)
@Composable
fun PlayerCoverLargestTextScreenshot() {
    PlayerScreenshotContent(showArtwork = true)
}

@PreviewTest
@Preview(name = "CoverPaused", widthDp = 390, heightDp = 844)
@Composable
fun PlayerCoverPausedScreenshot() {
    PlayerScreenshotContent(isPlaying = false, showArtwork = true)
}

@PreviewTest
@Preview(name = "CoverBuffering", widthDp = 390, heightDp = 844)
@Composable
fun PlayerCoverBufferingScreenshot() {
    PlayerScreenshotContent(isBuffering = true, showArtwork = true)
}

@PreviewTest
@Preview(name = "CoverUnknownDuration", widthDp = 390, heightDp = 844)
@Composable
fun PlayerCoverUnknownDurationScreenshot() {
    PlayerScreenshotContent(durationMs = 0, showArtwork = true)
}

@PreviewTest
@Preview(name = "CoverConnecting", widthDp = 390, heightDp = 844)
@Composable
fun PlayerCoverConnectingScreenshot() {
    PlayerScreenshotContent(controlsEnabled = false, showArtwork = true)
}

@PreviewTest
@Preview(name = "Empty", widthDp = 390, heightDp = 844)
@Composable
fun PlayerEmptyScreenshot() {
    PlayerScreen(
        state = PlayerUiState(),
        progress = remember { mutableStateOf(PlayerProgressUiState()) },
        onBack = {},
        onTogglePlayback = {},
        onSeek = {},
        onPrevious = {},
        onNext = {},
        onChangeMode = {},
        onOpenQueue = {},
    )
}

@PreviewTest
@Preview(name = "LyricsTranslation", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsTranslationScreenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = translationLyricsState())
}

@PreviewTest
@Preview(name = "LyricsLoading", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsLoadingScreenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = PlayerLyricsUiState.Loading)
}

@PreviewTest
@Preview(name = "LyricsEmpty", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsEmptyScreenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = PlayerLyricsUiState.Empty)
}

@PreviewTest
@Preview(name = "LyricsOffline", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsOfflineScreenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = PlayerLyricsUiState.Offline)
}

@PreviewTest
@Preview(name = "LyricsError", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsErrorScreenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = PlayerLyricsUiState.Error)
}

@PreviewTest
@Preview(name = "LyricsOriginal", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsOriginalScreenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = originalLyricsState())
}

@PreviewTest
@Preview(name = "LyricsPhonetic", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsPhoneticScreenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = phoneticLyricsState())
}

@PreviewTest
@Preview(name = "LyricsFont150", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsFont150Screenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = largeTextLyricsState())
}

@PreviewTest
@Preview(name = "LyricsFont200", widthDp = 390, heightDp = 844)
@Composable
fun PlayerLyricsFont200Screenshot() {
    PlayerScreenshotContent(initialPage = PlayerPage.Lyrics, lyricsState = largestTextLyricsState())
}

@Composable
private fun PlayerScreenshotContent(
    isPlaying: Boolean = true,
    isBuffering: Boolean = false,
    controlsEnabled: Boolean = true,
    durationMs: Long = 268_000,
    showArtwork: Boolean = false,
    initialPage: PlayerPage = PlayerPage.Cover,
    lyricsState: PlayerLyricsUiState = PlayerLyricsUiState.Loading,
) {
    PlayerScreen(
        state =
            PlayerUiState(
                item =
                    PlaybackItem(
                        id = "preview-player",
                        title = "コレカラ（从今以后）",
                        artist = "Machico",
                        albumTitle = "从今以后",
                        source = PlaybackSource.FoundationDemo,
                    ),
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                controlsEnabled = controlsEnabled,
                mode = PlaybackMode.RepeatAll,
            ),
        progress =
            remember {
                mutableStateOf(
                    PlayerProgressUiState(
                        positionMs = 84_000,
                        durationMs = durationMs,
                    ),
                )
            },
        onBack = {},
        onTogglePlayback = {},
        onSeek = {},
        onPrevious = {},
        onNext = {},
        onChangeMode = {},
        onOpenQueue = {},
        initialPage = initialPage,
        lyricsState = lyricsState,
        artworkContent =
            if (showArtwork) {
                { _ -> PlayerScreenshotArtwork() }
            } else {
                null
            },
    )
}

private fun translationLyricsState() =
    PlayerLyricsUiState.Content(
        lines =
            listOf(
                PlayerLyricLineUi("コレカラさ変わる未来", "现在开始 不同的未来"),
                PlayerLyricLineUi("なけなしの希望を手に", "手握缥缈的希望"),
                PlayerLyricLineUi("崖っぷち進もう", "朝悬边前进吧", highlightedCharacterCount = 3),
                PlayerLyricLineUi("崖の向こうには花が咲く", "山崖的对面是鲜花盛开"),
                PlayerLyricLineUi("ちっぽけで", "心里罗列着"),
                PlayerLyricLineUi("大きな夢を並べては", "渺小却又伟大的梦想"),
                PlayerLyricLineUi("砂のようにすり抜けても", "就算如沙般从指缝中溜走"),
            ),
        activeLineIndex = 2,
    )

private fun originalLyricsState() =
    PlayerLyricsUiState.Content(
        lines =
            listOf(
                PlayerLyricLineUi("コレカラさ変わる未来"),
                PlayerLyricLineUi("なけなしの希望を手に"),
                PlayerLyricLineUi("崖っぷち進もう", highlightedCharacterCount = 3),
                PlayerLyricLineUi("崖の向こうには花が咲く"),
                PlayerLyricLineUi("ちっぽけで"),
                PlayerLyricLineUi("大きな夢を並べては"),
            ),
        activeLineIndex = 2,
    )

private fun phoneticLyricsState() =
    PlayerLyricsUiState.Content(
        lines =
            listOf(
                PlayerLyricLineUi("コレカラさ変わる未来", "kore kara sa kawaru mirai"),
                PlayerLyricLineUi("なけなしの希望を手に", "nakenashi no kibō o te ni"),
                PlayerLyricLineUi("崖っぷち進もう", "gakeppuchi susumō", highlightedCharacterCount = 3),
                PlayerLyricLineUi("崖の向こうには花が咲く", "gake no mukō ni wa hana ga saku"),
            ),
        activeLineIndex = 2,
    )

private fun largeTextLyricsState() =
    PlayerLyricsUiState.Content(
        lines = translationLyricsState().lines.drop(1).take(4),
        activeLineIndex = 1,
        textSize = PlayerLyricsTextSize.Large,
    )

private fun largestTextLyricsState() =
    PlayerLyricsUiState.Content(
        lines = translationLyricsState().lines.drop(1).take(3),
        activeLineIndex = 1,
        textSize = PlayerLyricsTextSize.Largest,
    )

@Composable
private fun PlayerScreenshotArtwork() {
    Image(
        painter = painterResource(R.drawable.player_preview_cover),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
