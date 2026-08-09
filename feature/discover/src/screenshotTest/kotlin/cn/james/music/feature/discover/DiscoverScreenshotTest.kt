package cn.james.music.feature.discover

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "ContentLight", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverContentLightScreenshot() = DiscoverScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentLayoutProbe", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverContentLayoutProbeScreenshot() =
    CompositionLocalProvider(
        LocalDiscoverLayoutProbeColors provides
            mapOf(
                DISCOVER_PROBE_TABS to Color.Magenta,
                DISCOVER_PROBE_HERO to Color.Cyan,
                DISCOVER_PROBE_RANKING_HEADER to Color.Green,
                DISCOVER_PROBE_RANKING_GRID to Color.Blue,
                DISCOVER_PROBE_CATEGORY_HEADER to Color.Red,
                DISCOVER_PROBE_CATEGORY_CHIPS to Color.Yellow,
                DISCOVER_PROBE_CATEGORY_GRID to Color(0xFF00FF7F),
            ),
    ) {
        DiscoverScreenshotContent(contentState, ThemeMode.Light)
    }

@PreviewTest
@Preview(name = "SelectionRestored", widthDp = 390, heightDp = 843)
@Composable
fun DiscoverSelectionRestoredScreenshot() =
    DiscoverScreenshotContent(
        state = contentState,
        themeMode = ThemeMode.Light,
        selectedTabId = "ranking",
        selectedCategoryId = "light",
    )

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
    selectedTabId: String = discoverDefaultContent.tabs.first().id,
    selectedCategoryId: String = discoverDefaultContent.categories.first().id,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
            DiscoverScreen(
                state = state,
                selectedTabId = selectedTabId,
                selectedCategoryId = selectedCategoryId,
            )
        }
    }
}

private val contentState = DiscoverUiState.Content(discoverDefaultContent)
