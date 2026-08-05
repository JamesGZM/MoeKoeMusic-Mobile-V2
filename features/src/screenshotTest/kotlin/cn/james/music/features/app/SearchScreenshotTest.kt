package cn.james.music.features.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.online.Song
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Idle", widthDp = 390, heightDp = 844)
@Composable
fun SearchIdleScreenshot() {
    SearchScreenshotContent(SearchUiState())
}

@PreviewTest
@Preview(name = "Content", widthDp = 390, heightDp = 844)
@Composable
fun SearchContentScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "MoeKoe",
            submittedQuery = "MoeKoe",
            songs = previewSearchSongs,
            page = 1,
            hasMore = true,
        ),
    )
}

@PreviewTest
@Preview(name = "ContentLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun SearchContentLargeTextScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "MoeKoe",
            submittedQuery = "MoeKoe",
            songs = previewSearchSongs,
            page = 1,
        ),
    )
}

@Composable
private fun SearchScreenshotContent(state: SearchUiState) {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Surface {
            SearchScreen(
                state = state,
                onBack = {},
                onQueryChange = {},
                onSearch = {},
                onLoadMore = {},
            )
        }
    }
}

private val previewSearchSongs =
    listOf(
        Song("song-1", "hash-1", "Two Faced", "Linkin Park", null, "From Zero", 186_000, null),
        Song("song-2", "hash-2", "Whose Blue", "Y 2025", null, null, 241_000, null),
        Song("song-3", "hash-3", "夏日漱石", "RADWIMPS", null, "夏日时光", 224_000, null),
        Song("song-4", "hash-4", "星间旅行", "室内系的TrackMaker", null, null, 198_000, null),
    )
