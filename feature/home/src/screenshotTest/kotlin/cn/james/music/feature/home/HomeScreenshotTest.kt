package cn.james.music.feature.home

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "ContentLight", widthDp = 390, heightDp = 844)
@Composable
fun HomeContentLightScreenshot() = HomeScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentDark", widthDp = 390, heightDp = 844)
@Composable
fun HomeContentDarkScreenshot() = HomeScreenshotContent(contentState, ThemeMode.Dark)

@PreviewTest
@Preview(name = "ContentLargeText15", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun HomeContentLargeText15Screenshot() = HomeScreenshotContent(contentState.copy(refreshProblem = null), ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentLargeText20", widthDp = 390, heightDp = 844, fontScale = 2f)
@Composable
fun HomeContentLargeText20Screenshot() = HomeScreenshotContent(contentState.copy(refreshProblem = null), ThemeMode.Light)

@PreviewTest
@Preview(name = "Loading", widthDp = 390, heightDp = 844)
@Composable
fun HomeLoadingScreenshot() = HomeScreenshotContent(HomeUiState(), ThemeMode.Light)

@PreviewTest
@Preview(name = "Empty", widthDp = 390, heightDp = 844)
@Composable
fun HomeEmptyScreenshot() =
    HomeScreenshotContent(HomeUiState(content = HomeContentUiState.Empty), ThemeMode.Light)

@PreviewTest
@Preview(name = "Failure", widthDp = 390, heightDp = 844)
@Composable
fun HomeFailureScreenshot() =
    HomeScreenshotContent(
        HomeUiState(content = HomeContentUiState.Failure(HomeProblemUi.Offline)),
        ThemeMode.Light,
    )

@Composable
private fun HomeScreenshotContent(
    state: HomeUiState,
    themeMode: ThemeMode,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
            HomeScreen(
                state = state,
                onSearch = {},
                onRefresh = {},
                onDismissProblem = {},
                onPlay = {},
            )
        }
    }
}

private val contentState =
    HomeUiState(
        content =
            HomeContentUiState.Content(
                HomeContentUi(
                    banners = emptyList(),
                    recommendations =
                        listOf(
                            previewSong("1", "夏日漱石", "RADWIMPS", "今日适合出发"),
                            previewSong("2", "Two Faced", "Linkin Park", null),
                            previewSong("3", "星间旅行", "室内系的TrackMaker", "夜晚推荐"),
                            previewSong("4", "Whose Blue", "Y 2025", null),
                        ),
                    playlists =
                        listOf(
                            HomePlaylistUi("1", "治愈海风", null, 120_000),
                            HomePlaylistUi("2", "午后的闲适时光", null, 86_000),
                            HomePlaylistUi("3", "清新旋律", null, 32_000),
                        ),
                ),
            ),
        refreshProblem = HomeProblemUi.Timeout,
    )

private fun previewSong(
    id: String,
    title: String,
    artist: String,
    note: String?,
) = HomeSongUi(id, "hash-$id", title, artist, null, null, 215_000, null, note)
