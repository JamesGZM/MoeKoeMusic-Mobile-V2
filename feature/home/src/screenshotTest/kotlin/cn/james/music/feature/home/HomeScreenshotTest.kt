package cn.james.music.feature.home

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "ContentLight", widthDp = 390, heightDp = 844)
@Composable
fun HomeContentLightScreenshot() = HomeScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentLayoutProbe", widthDp = 390, heightDp = 844)
@Composable
fun HomeContentLayoutProbeScreenshot() =
    CompositionLocalProvider(
        LocalHomeLayoutProbeColors provides
            mapOf(
                HOME_PROBE_HERO to Color.Magenta,
                HOME_PROBE_QUICK_ENTRIES to Color.Cyan,
                HOME_PROBE_DAILY_HEADER to Color.Green,
                HOME_PROBE_FIRST_SONG to Color.Blue,
                HOME_PROBE_PLAYLIST_HEADER to Color.Red,
                HOME_PROBE_PLAYLIST_GRID to Color.Yellow,
            ),
    ) {
        HomeScreenshotContent(contentState, ThemeMode.Light)
    }

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
                onAction = {},
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
                            screenshotSong("1", "これからも。", "水瀬祈（みなせ いのり）", R.drawable.home_playlist_seaside, HomeSongBadgeUi.Quality),
                            screenshotSong("2", "キライ…でも好き", "BRIGHT", R.drawable.home_playlist_room, HomeSongBadgeUi.MusicVideo),
                            screenshotSong("3", "クリームソーダとシャンデリア", "ねんね", R.drawable.home_playlist_bamboo, HomeSongBadgeUi.Quality),
                            screenshotSong("4", "フェイスレス", "蓝井エイル（Aoi Eir）", R.drawable.home_playlist_night_city, HomeSongBadgeUi.Quality),
                        ),
                    playlists =
                        listOf(
                            HomePlaylistUi("1", "治愈海风", HomeArtworkUi.Resource(R.drawable.home_playlist_seaside), HomePlaylistSupportingUi.Text("海风与日落")),
                            HomePlaylistUi("2", "日系放松", HomeArtworkUi.Resource(R.drawable.home_playlist_room), HomePlaylistSupportingUi.Text("午后的闲适时光")),
                            HomePlaylistUi("3", "清新旋律", HomeArtworkUi.Resource(R.drawable.home_playlist_bamboo), HomePlaylistSupportingUi.Text("自然与轻音乐")),
                            HomePlaylistUi("4", "夜色电台", HomeArtworkUi.Resource(R.drawable.home_playlist_night_city), HomePlaylistSupportingUi.Text("深夜陪伴")),
                        ),
                ),
            ),
    )

private fun screenshotSong(
    id: String,
    title: String,
    artist: String,
    artworkRes: Int,
    badge: HomeSongBadgeUi,
) = HomeSongUi(
    id = id,
    title = title,
    artistName = artist,
    artwork = HomeArtworkUi.Resource(artworkRes),
    badge = badge,
)
