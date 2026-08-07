package cn.james.music.feature.playlist

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "ContentLight", widthDp = 390, heightDp = 764)
@Composable
fun PlaylistDetailContentLightScreenshot() = PlaylistDetailScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentDark", widthDp = 390, heightDp = 764)
@Composable
fun PlaylistDetailContentDarkScreenshot() = PlaylistDetailScreenshotContent(contentState, ThemeMode.Dark)

@PreviewTest
@Preview(name = "ContentAmoled", widthDp = 390, heightDp = 764)
@Composable
fun PlaylistDetailContentAmoledScreenshot() = PlaylistDetailScreenshotContent(contentState, ThemeMode.Amoled)

@PreviewTest
@Preview(name = "ContentLargeText15", widthDp = 390, heightDp = 764, fontScale = 1.5f)
@Composable
fun PlaylistDetailContentLargeText15Screenshot() = PlaylistDetailScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentLargeText20", widthDp = 390, heightDp = 764, fontScale = 2f)
@Composable
fun PlaylistDetailContentLargeText20Screenshot() = PlaylistDetailScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "Loading", widthDp = 390, heightDp = 764)
@Composable
fun PlaylistDetailLoadingScreenshot() = PlaylistDetailScreenshotContent(PlaylistDetailUiState.Loading, ThemeMode.Light)

@PreviewTest
@Preview(name = "Empty", widthDp = 390, heightDp = 764)
@Composable
fun PlaylistDetailEmptyScreenshot() = PlaylistDetailScreenshotContent(PlaylistDetailUiState.Empty, ThemeMode.Light)

@PreviewTest
@Preview(name = "Error", widthDp = 390, heightDp = 764)
@Composable
fun PlaylistDetailErrorScreenshot() = PlaylistDetailScreenshotContent(PlaylistDetailUiState.Error, ThemeMode.Light)

@Composable
private fun PlaylistDetailScreenshotContent(
    state: PlaylistDetailUiState,
    themeMode: ThemeMode,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
            PlaylistDetailScreen(
                state = state,
                onBack = {},
                onSearch = {},
                onMore = {},
                onPlayAll = {},
                onShuffle = {},
                onFavorite = {},
                onDownload = {},
                onSort = {},
                onTrack = {},
                onTrackMore = {},
                onRetry = {},
            )
        }
    }
}

private val contentState = PlaylistDetailUiState.Content(playlistDetailDesignPreview)
