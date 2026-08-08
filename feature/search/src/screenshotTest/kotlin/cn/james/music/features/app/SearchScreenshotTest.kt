package cn.james.music.features.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.online.SearchError
import cn.james.music.core.model.online.Song
import cn.james.music.feature.search.R
import cn.james.music.feature.search.SearchArtistUi
import cn.james.music.feature.search.SearchCollectionUi
import cn.james.music.feature.search.SearchScreen
import cn.james.music.feature.search.SearchSongBadge
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
    SearchScreenshotContent(SearchUiState(query = "初音未来", submittedQuery = "初音未来", error = SearchError.Offline))

@PreviewTest
@Preview(name = "PaginationError", widthDp = 390, heightDp = 764)
@Composable
fun SearchPaginationErrorScreenshot() = SearchScreenshotContent(contentState.copy(error = SearchError.Timeout, hasMore = true))

@Composable
private fun SearchScreenshotContent(
    state: SearchUiState,
    themeMode: ThemeMode = ThemeMode.Light,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
            SearchScreen(
                state = state,
                onBack = {},
                onQueryChange = {},
                onSearch = {},
                onLoadMore = {},
                onPlay = {},
            )
        }
    }
}

private val previewSearchSongs =
    listOf(
        Song("song-1", "hash-1", "初音未来的消失", "初音未来", null, "初音未来的消失", 186_000, null),
        Song("song-2", "hash-2", "甩葱歌", "初音未来", null, "未来序曲", 241_000, null),
        Song("song-3", "hash-3", "Tell Your World", "初音未来", null, "Tell Your World EP", 224_000, null),
        Song("song-4", "hash-4", "深海少女", "初音未来", null, "深海少女", 198_000, null),
    )

private val contentState =
    SearchUiState(
        query = "初音未来",
        submittedQuery = "初音未来",
        songs = previewSearchSongs,
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
        songBadges =
            mapOf(
                "song-1" to SearchSongBadge.Quality,
                "song-2" to SearchSongBadge.Quality,
                "song-3" to SearchSongBadge.Quality,
                "song-4" to SearchSongBadge.Mv,
            ),
        songArtwork =
            mapOf(
                "song-1" to R.drawable.search_song_portrait_confirmed,
                "song-2" to R.drawable.search_artist_confirmed,
                "song-3" to R.drawable.search_song_stage_confirmed,
                "song-4" to R.drawable.search_song_ocean_confirmed,
            ),
        playingSongId = "song-1",
    )
