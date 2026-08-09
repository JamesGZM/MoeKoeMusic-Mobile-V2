package cn.james.music.feature.profile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun offlineStateKeepsToolbarAndRetryActionVisible() {
        var action: UserProfileAction? = null
        setProfileContent(UserProfileUiState.Offline) { action = it }

        composeRule.onNodeWithText("个人主页").assertIsDisplayed()
        composeRule.onNodeWithText("当前处于离线状态").assertIsDisplayed()
        composeRule.onNodeWithText("重试").assertIsDisplayed().performClick()
        assertEquals(UserProfileAction.Retry, action)
    }

    @Test
    fun loadingStateUsesStableTestTagWithoutSyntheticTalkBackText() {
        setProfileContent(UserProfileUiState.Loading)

        composeRule.onNodeWithTag(USER_PROFILE_LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("正在加载个人主页").assertIsDisplayed()
    }

    @Test
    fun editActionSeparatesVisibleHeightFromAccessibleTouchTarget() {
        setProfileContent(UserProfileUiState.Content(userProfileDesignPreview))

        composeRule.onNodeWithTag(USER_PROFILE_EDIT_TOUCH_TAG).assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag(USER_PROFILE_EDIT_VISUAL_TAG).assertHeightIsEqualTo(38.dp)
        composeRule.onNodeWithTag(USER_PROFILE_OVERVIEW_TITLE_TAG).assertHeightIsEqualTo(24.dp)
    }

    @Test
    fun toolbarForwardsBackShareAndMoreEvents() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                UserProfileScreen(
                    state = UserProfileUiState.Content(userProfileDesignPreview),
                    onBack = { events += "back" },
                    onAction = { action ->
                        when (action) {
                            UserProfileAction.Share -> events += "share"
                            UserProfileAction.More -> events += "more"
                            else -> Unit
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText("个人主页").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithContentDescription("分享个人主页").performClick()
        composeRule.onNodeWithContentDescription("更多个人主页操作").performClick()

        assertEquals(listOf("back", "share", "more"), events)
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

    private fun setProfileContent(
        state: UserProfileUiState,
        onAction: (UserProfileAction) -> Unit = {},
    ) {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) { TestUserProfileScreen(state, onAction) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TestUserProfileScreen(
    state: UserProfileUiState,
    onAction: (UserProfileAction) -> Unit = {},
) {
    UserProfileScreen(
        state = state,
        onBack = {},
        onAction = onAction,
    )
}
