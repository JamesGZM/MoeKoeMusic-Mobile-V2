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

@Composable
private fun PlayerScreenshotContent(
    isPlaying: Boolean = true,
    isBuffering: Boolean = false,
    controlsEnabled: Boolean = true,
    durationMs: Long = 268_000,
    showArtwork: Boolean = false,
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
        artworkContent =
            if (showArtwork) {
                { _ -> PlayerScreenshotArtwork() }
            } else {
                null
            },
    )
}

@Composable
private fun PlayerScreenshotArtwork() {
    Image(
        painter = painterResource(R.drawable.player_preview_cover),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
