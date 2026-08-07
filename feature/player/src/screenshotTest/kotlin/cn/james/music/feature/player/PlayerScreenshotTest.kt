package cn.james.music.feature.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
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
@Preview(name = "Queue", widthDp = 390, heightDp = 470)
@Composable
fun PlayerQueueScreenshot() {
    PlayerQueueScreenshotContent(queueScreenshotState())
}

@PreviewTest
@Preview(name = "QueueEmpty", widthDp = 390, heightDp = 470)
@Composable
fun PlayerQueueEmptyScreenshot() {
    PlayerQueueScreenshotContent(
        PlayerQueueUiState(items = emptyList(), currentIndex = -1, mode = PlaybackMode.RepeatAll),
    )
}

@PreviewTest
@Preview(name = "QueueLargeText", widthDp = 390, heightDp = 560, fontScale = 1.5f)
@Composable
fun PlayerQueueLargeTextScreenshot() {
    PlayerQueueScreenshotContent(queueScreenshotState(), sheetHeight = 533.dp)
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

@Composable
private fun PlayerQueueScreenshotContent(
    state: PlayerQueueUiState,
    sheetHeight: androidx.compose.ui.unit.Dp = 470.dp,
) {
    MoeKoeTheme(themeMode = ThemeMode.Dark) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF080D18)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            PlayerQueueSheetContent(
                state = state,
                onDismiss = {},
                onPlayAt = {},
                onRemove = {},
                onClear = {},
                onChangeMode = {},
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                artworkContent = { PlayerScreenshotArtwork() },
            )
        }
    }
}

private fun queueScreenshotState(): PlayerQueueUiState {
    val visibleSongs =
        listOf(
            Triple("コレカラ（从今以后）", "Machico", "3:04"),
            Triple("キライ…でも好き", "HoneyWorks", "4:12"),
            Triple("Cream Soda", "内田真礼", "3:45"),
            Triple("フェイスレス", "ReoNa", "4:07"),
            Triple("unlasting", "LiSA", "5:18"),
            Triple("IGNITE", "蓝井エイル", "3:37"),
        )
    val songs = visibleSongs + visibleSongs
    return PlayerQueueUiState(
        items =
            songs.mapIndexed { index, (title, artist, duration) ->
                PlayerQueueItemUi(
                    item =
                        PlaybackItem(
                            id = "queue-$index",
                            title = title,
                            artist = artist,
                            source = PlaybackSource.FoundationDemo,
                        ),
                    durationLabel = duration,
                )
            },
        currentIndex = 0,
        mode = PlaybackMode.RepeatAll,
        sourceLabel = "ACG 收藏",
    )
}
