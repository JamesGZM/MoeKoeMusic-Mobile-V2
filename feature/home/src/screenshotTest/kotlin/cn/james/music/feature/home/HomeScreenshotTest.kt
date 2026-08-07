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
@Preview(name = "ContentAmoled", widthDp = 390, heightDp = 844)
@Composable
fun HomeContentAmoledScreenshot() = HomeScreenshotContent(contentState, ThemeMode.Amoled)

@PreviewTest
@Preview(name = "ContentLargeText15", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun HomeContentLargeText15Screenshot() = HomeScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentLargeText20", widthDp = 390, heightDp = 844, fontScale = 2f)
@Composable
fun HomeContentLargeText20Screenshot() = HomeScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "RefreshProblem", widthDp = 390, heightDp = 844)
@Composable
fun HomeRefreshProblemScreenshot() =
    HomeScreenshotContent(contentState.copy(refreshProblem = HomeProblemUi.Timeout), ThemeMode.Light)

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
                            previewSong("1", "これからも。", "水瀬祈（みなせ いのり）", R.drawable.home_playlist_seaside, "HQ"),
                            previewSong("2", "キライ…でも好き", "BRIGHT", R.drawable.home_playlist_room, "MV", true),
                            previewSong("3", "クリームソーダとシャンデリア", "ねんね", R.drawable.home_playlist_bamboo, "HQ"),
                            previewSong("4", "フェイスレス", "蓝井エイル（Aoi Eir）", R.drawable.home_playlist_night_city, "HQ"),
                        ),
                    playlists =
                        listOf(
                            HomePlaylistUi("1", "治愈海风", null, 120_000, R.drawable.home_playlist_seaside, "海风与日落"),
                            HomePlaylistUi("2", "日系放松", null, 86_000, R.drawable.home_playlist_room, "午后的闲适时光"),
                            HomePlaylistUi("3", "清新旋律", null, 32_000, R.drawable.home_playlist_bamboo, "自然与轻音乐"),
                            HomePlaylistUi("4", "夜色电台", null, 58_000, R.drawable.home_playlist_night_city, "深夜陪伴"),
                        ),
                ),
            ),
    )

private fun previewSong(
    id: String,
    title: String,
    artist: String,
    artworkRes: Int,
    badge: String,
    badgeIsError: Boolean = false,
) = HomeSongUi(
    id = id,
    hash = "hash-$id",
    title = title,
    artistName = artist,
    albumId = null,
    albumTitle = null,
    durationMs = 215_000,
    artworkUrl = null,
    note = null,
    previewArtworkRes = artworkRes,
    previewBadge = badge,
    previewBadgeIsError = badgeIsError,
)
