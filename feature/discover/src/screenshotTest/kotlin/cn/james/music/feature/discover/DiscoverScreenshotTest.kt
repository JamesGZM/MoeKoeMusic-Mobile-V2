package cn.james.music.feature.discover

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "ContentLight", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverContentLightScreenshot() = DiscoverScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentDark", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverContentDarkScreenshot() = DiscoverScreenshotContent(contentState, ThemeMode.Dark)

@PreviewTest
@Preview(name = "ContentAmoled", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverContentAmoledScreenshot() = DiscoverScreenshotContent(contentState, ThemeMode.Amoled)

@PreviewTest
@Preview(name = "ContentLargeText15", widthDp = 390, heightDp = 843, fontScale = 1.5f)
@Composable
fun DiscoverContentLargeText15Screenshot() = DiscoverScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentLargeText20", widthDp = 390, heightDp = 843, fontScale = 2f)
@Composable
fun DiscoverContentLargeText20Screenshot() = DiscoverScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "Loading", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverLoadingScreenshot() = DiscoverScreenshotContent(DiscoverUiState.Loading, ThemeMode.Light)

@PreviewTest
@Preview(name = "Empty", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverEmptyScreenshot() = DiscoverScreenshotContent(DiscoverUiState.Empty, ThemeMode.Light)

@PreviewTest
@Preview(name = "Error", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverErrorScreenshot() = DiscoverScreenshotContent(DiscoverUiState.Error, ThemeMode.Light)

@Composable
private fun DiscoverScreenshotContent(
    state: DiscoverUiState,
    themeMode: ThemeMode,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface { DiscoverScreen(state = state) }
    }
}

private val contentState = DiscoverUiState.Content(discoverDesignPreview)
