package cn.james.music.feature.playlist

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cn.james.music.core.designsystem.MoeKoeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaylistDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentForwardsPageAndTrackEvents() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    PlaylistDetailScreen(
                        state = PlaylistDetailUiState.Content(playlistDetailDesignPreview),
                        onBack = { events += "back" },
                        onSearch = { events += "search" },
                        onMore = { events += "more" },
                        onPlayAll = { events += "play-all" },
                        onShuffle = { events += "shuffle" },
                        onFavorite = { events += "favorite" },
                        onDownload = { events += "download" },
                        onSort = { events += "sort" },
                        onTrack = { events += "track-$it" },
                        onTrackMore = { events += "track-more-$it" },
                        onRetry = { events += "retry" },
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithContentDescription("搜索歌单歌曲").performClick()
        composeRule.onNodeWithContentDescription("更多歌单操作").performClick()
        composeRule.onNodeWithText("播放全部").performClick()
        composeRule.onNodeWithText("随机播放").performClick()
        composeRule.onNodeWithContentDescription("收藏").performClick()
        composeRule.onNodeWithContentDescription("下载").performClick()
        composeRule.onNodeWithText("默认排序").performClick()
        composeRule.onNodeWithText("これからも。").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("更多歌曲操作：これからも。").performScrollTo().performClick()

        assertEquals(
            listOf("back", "search", "more", "play-all", "shuffle", "favorite", "download", "sort", "track-0", "track-more-0"),
            events,
        )
    }

    @Test
    fun nonContentStatesDoNotCreateTheScrollableSongList() {
        composeRule.setContent {
            MoeKoeTheme {
                PlaylistDetailScreen(
                    state = PlaylistDetailUiState.Error,
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

        composeRule.onNodeWithTag(PLAYLIST_DETAIL_CONTENT_TAG).assertDoesNotExist()
        composeRule.onNodeWithText("歌单加载失败").assertExists()
    }
}
