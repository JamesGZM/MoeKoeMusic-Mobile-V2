package cn.james.music.feature.profile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun offlineStateKeepsToolbarAndRetryActionVisible() {
        setProfileContent(UserProfileUiState.Offline)

        composeRule.onNodeWithText("个人主页").assertIsDisplayed()
        composeRule.onNodeWithText("当前处于离线状态").assertIsDisplayed()
        composeRule.onNodeWithText("重试").assertIsDisplayed()
    }

    @Test
    fun loadingStateUsesStableTestTagWithoutSyntheticTalkBackText() {
        setProfileContent(UserProfileUiState.Loading)

        composeRule.onNodeWithTag(USER_PROFILE_LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("正在加载个人主页").assertIsDisplayed()
    }

    @Test
    fun twoTimesFontScaleKeepsLastPlaylistReachableBySinglePageScroll() {
        val density = composeRule.density
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MoeKoeTheme(themeMode = ThemeMode.Light) {
                    TestUserProfileScreen(UserProfileUiState.Content(userProfileDesignPreview))
                }
            }
        }

        composeRule.onNodeWithTag(USER_PROFILE_CONTENT_TAG).performScrollToNode(hasText("夜间电台"))
        composeRule.onNodeWithText("夜间电台").assertIsDisplayed()
    }

    private fun setProfileContent(state: UserProfileUiState) {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) { TestUserProfileScreen(state) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TestUserProfileScreen(state: UserProfileUiState) {
    UserProfileScreen(
        state = state,
        onBack = {},
        onShare = {},
        onMore = {},
        onEdit = {},
        onFollowing = {},
        onFollowers = {},
        onFriends = {},
        onViewAll = {},
        onPlaylist = {},
        onPlaylistMore = {},
        onRetry = {},
    )
}
