package cn.james.music.feature.profile

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "ContentLight", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileContentLightScreenshot() = UserProfileScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContractContentLight", widthDp = 390, heightDp = 843)
@Composable
fun UserProfileContractContentLightScreenshot() = UserProfileScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "ContractLayoutProbe", widthDp = 390, heightDp = 843)
@Composable
fun UserProfileContractLayoutProbeScreenshot() {
    CompositionLocalProvider(
        LocalUserProfileLayoutProbeColors provides
            mapOf(
                PROBE_HERO to Color.Magenta,
                PROBE_EDIT_TOP to Color(0xFF0102FD),
                PROBE_EDIT_BOTTOM to Color(0xFFFD0201),
                PROBE_OVERVIEW_TITLE_TOP to Color(0xFF02FD7F),
                PROBE_OVERVIEW_TITLE_BOTTOM to Color(0xFFFD7F02),
                PROBE_OVERVIEW to Color.Cyan,
                PROBE_PLAYLIST_LIST to Color.Green,
                PROBE_PLAYLIST_ROW_2 to Color(0xFF0DF1A7),
                PROBE_PLAYLIST_ROW_3 to Color(0xFF7FFD02),
            ),
    ) {
        UserProfileScreenshotContent(contentState, ThemeMode.Light)
    }
}

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

@PreviewTest
@Preview(name = "Offline", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileOfflineScreenshot() = UserProfileScreenshotContent(UserProfileUiState.Offline, ThemeMode.Light)

@PreviewTest
@Preview(name = "WideContent", widthDp = 840, heightDp = 764)
@Composable
fun UserProfileWideContentScreenshot() = UserProfileScreenshotContent(contentState, ThemeMode.Light)

@PreviewTest
@Preview(name = "LongContent", widthDp = 390, heightDp = 764)
@Composable
fun UserProfileLongContentScreenshot() =
    UserProfileScreenshotContent(
        UserProfileUiState.Content(
            userProfileDesignPreview.copy(
                nickname = "一段用于验证省略与徽标布局的很长昵称",
                signature = "这是一段用于验证个人签名最多两行、不会挤压关系统计和编辑按钮的长签名内容。",
                vipLabel = null,
            ),
        ),
        ThemeMode.Light,
    )

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
                onAction = {},
            )
        }
    }
}

private val contentState = UserProfileUiState.Content(userProfileDesignPreview)
