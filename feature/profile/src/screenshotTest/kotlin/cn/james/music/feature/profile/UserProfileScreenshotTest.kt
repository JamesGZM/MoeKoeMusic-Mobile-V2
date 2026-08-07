package cn.james.music.feature.profile

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "ContentLight", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileContentLightScreenshot() = UserProfileScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentDark", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileContentDarkScreenshot() = UserProfileScreenshotContent(contentState, ThemeMode.Dark)

@PreviewTest
@Preview(name = "ContentAmoled", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileContentAmoledScreenshot() = UserProfileScreenshotContent(contentState, ThemeMode.Amoled)

@PreviewTest
@Preview(name = "ContentLargeText15", widthDp = 390, heightDp = 764, fontScale = 1.5f)
@Composable
fun UserProfileContentLargeText15Screenshot() = UserProfileScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContentLargeText20", widthDp = 390, heightDp = 764, fontScale = 2f)
@Composable
fun UserProfileContentLargeText20Screenshot() = UserProfileScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "Loading", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileLoadingScreenshot() = UserProfileScreenshotContent(UserProfileUiState.Loading, ThemeMode.Light)

@PreviewTest
@Preview(name = "Empty", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileEmptyScreenshot() =
    UserProfileScreenshotContent(
        UserProfileUiState.Content(userProfileDesignPreview.copy(playlists = emptyList())),
        ThemeMode.Light,
    )

@PreviewTest
@Preview(name = "Error", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileErrorScreenshot() = UserProfileScreenshotContent(UserProfileUiState.Error, ThemeMode.Light)

@Composable
private fun UserProfileScreenshotContent(
    state: UserProfileUiState,
    themeMode: ThemeMode,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
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
    }
}

private val contentState = UserProfileUiState.Content(userProfileDesignPreview)
