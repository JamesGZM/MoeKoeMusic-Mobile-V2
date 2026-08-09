package cn.james.music.features.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.feature.search.R
import cn.james.music.feature.search.LocalSearchLayoutProbeColors
import cn.james.music.feature.search.SEARCH_PROBE_ARTIST_HERO
import cn.james.music.feature.search.SEARCH_PROBE_COLLECTIONS_HEADER
import cn.james.music.feature.search.SEARCH_PROBE_COLLECTIONS_ROW
import cn.james.music.feature.search.SEARCH_PROBE_FIRST_SONG
import cn.james.music.feature.search.SEARCH_PROBE_FOURTH_SONG
import cn.james.music.feature.search.SEARCH_PROBE_SONGS_HEADER
import cn.james.music.feature.search.SEARCH_PROBE_TABS
import cn.james.music.feature.search.SEARCH_PROBE_TOOLBAR
import cn.james.music.feature.search.SearchArtistUi
import cn.james.music.feature.search.SearchCollectionUi
import cn.james.music.feature.search.SearchScreen
import cn.james.music.feature.search.SearchProblemUi
import cn.james.music.feature.search.SearchSongBadge
import cn.james.music.feature.search.SearchSongArtworkUi
import cn.james.music.feature.search.SearchSongUiModel
import cn.james.music.feature.search.SearchUiState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Idle", widthDp = 390, heightDp = 764)
@Composable
fun SearchIdleScreenshot() = SearchScreenshotContent(SearchUiState())

@PreviewTest
@Preview(name = "Content", widthDp = 390, heightDp = 764)
@Composable
fun SearchContentScreenshot() = SearchScreenshotContent(contentState)

@PreviewTest
@Preview(name = "LongQueryClear", widthDp = 390, heightDp = 764)
@Composable
fun SearchLongQueryClearScreenshot() =
    SearchScreenshotContent(
        SearchUiState(
            query = "初音未来最受欢迎的经典歌曲合集",
            submittedQuery = "初音未来最受欢迎的经典歌曲合集",
            loading = true,
        ),
    )

@PreviewTest
@Preview(name = "ContentLayoutProbe", widthDp = 390, heightDp = 764)
@Composable
fun SearchContentLayoutProbeScreenshot() =
    CompositionLocalProvider(
        LocalSearchLayoutProbeColors provides
            mapOf(
                SEARCH_PROBE_TOOLBAR to Color.Magenta,
                SEARCH_PROBE_TABS to Color.Cyan,
                SEARCH_PROBE_ARTIST_HERO to Color.Green,
                SEARCH_PROBE_SONGS_HEADER to Color.Blue,
                SEARCH_PROBE_FIRST_SONG to Color.Red,
                SEARCH_PROBE_FOURTH_SONG to Color.Yellow,
                SEARCH_PROBE_COLLECTIONS_HEADER to Color(0xFF00FF7F),
                SEARCH_PROBE_COLLECTIONS_ROW to Color(0xFFFF7F00),
            ),
    ) {
        SearchScreenshotContent(contentState)
    }

@PreviewTest
@Preview(name = "ContentDark", widthDp = 390, heightDp = 764)
@Composable
fun SearchContentDarkScreenshot() = SearchScreenshotContent(contentState, ThemeMode.Dark)

@PreviewTest
@Preview(name = "ContentAmoled", widthDp = 390, heightDp = 764)
@Composable
fun SearchContentAmoledScreenshot() = SearchScreenshotContent(contentState, ThemeMode.Amoled)

@PreviewTest
@Preview(name = "ContentLargeText15", widthDp = 390, heightDp = 764, fontScale = 1.5f)
@Composable
fun SearchContentLargeText15Screenshot() = SearchScreenshotContent(contentState)

@PreviewTest
@Preview(name = "ContentLargeText20", widthDp = 390, heightDp = 764, fontScale = 2f)
@Composable
fun SearchContentLargeText20Screenshot() = SearchScreenshotContent(contentState)

@PreviewTest
@Preview(name = "Loading", widthDp = 390, heightDp = 764)
@Composable
fun SearchLoadingScreenshot() = SearchScreenshotContent(SearchUiState(query = "初音未来", submittedQuery = "初音未来", loading = true))

@PreviewTest
@Preview(name = "Empty", widthDp = 390, heightDp = 764)
@Composable
fun SearchEmptyScreenshot() = SearchScreenshotContent(SearchUiState(query = "不存在", submittedQuery = "不存在"))

@PreviewTest
@Preview(name = "Error", widthDp = 390, heightDp = 764)
@Composable
fun SearchErrorScreenshot() =
    SearchScreenshotContent(SearchUiState(query = "初音未来", submittedQuery = "初音未来", error = SearchProblemUi.Offline))

@PreviewTest
@Preview(name = "PaginationError", widthDp = 390, heightDp = 764)
@Composable
fun SearchPaginationErrorScreenshot() = SearchScreenshotContent(contentState.copy(error = SearchProblemUi.Timeout, hasMore = true))

@Composable
private fun SearchScreenshotContent(
    state: SearchUiState,
    themeMode: ThemeMode = ThemeMode.Light,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
            SearchScreen(
                state = state,
                onAction = {},
            )
        }
    }
}

private val screenshotSearchSongs =
    listOf(
        SearchSongUiModel("song-1", "初音未来的消失", "初音未来", "初音未来的消失", SearchSongArtworkUi.Resource(R.drawable.search_song_portrait_confirmed), SearchSongBadge.Quality, true),
        SearchSongUiModel("song-2", "甩葱歌", "初音未来", "未来序曲", SearchSongArtworkUi.Resource(R.drawable.search_artist_confirmed), SearchSongBadge.Quality),
        SearchSongUiModel("song-3", "Tell Your World", "初音未来", "Tell Your World EP", SearchSongArtworkUi.Resource(R.drawable.search_song_stage_confirmed), SearchSongBadge.Quality),
        SearchSongUiModel("song-4", "深海少女", "初音未来", "深海少女", SearchSongArtworkUi.Resource(R.drawable.search_song_ocean_confirmed), SearchSongBadge.Mv),
    )

private val contentState =
    SearchUiState(
        query = "初音未来",
        submittedQuery = "初音未来",
        songs = screenshotSearchSongs,
        page = 1,
        artist =
            SearchArtistUi(
                name = "初音未来",
                badge = "虚拟歌手",
                stats = "歌曲 1.2万  |  粉丝 326.6万",
                representativeWorks = "代表作：《甩葱歌》《Tell Your World》",
                artworkRes = R.drawable.search_artist_confirmed,
            ),
        collections =
            listOf(
                SearchCollectionUi("c1", "初音未来精选集", "96 首", R.drawable.search_collection_seaside_confirmed),
                SearchCollectionUi("c2", "初音未来经典曲目", "72 首", R.drawable.search_collection_blossom_confirmed),
                SearchCollectionUi("c3", "初音未来热门歌曲", "58 首", R.drawable.search_collection_crystal_confirmed),
                SearchCollectionUi("c4", "初音未来热门MV", "45 首", R.drawable.search_collection_sky_confirmed),
            ),
    )
