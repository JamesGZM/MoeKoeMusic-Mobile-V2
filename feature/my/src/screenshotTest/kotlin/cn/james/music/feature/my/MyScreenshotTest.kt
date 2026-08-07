package cn.james.music.feature.my

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Authenticated", widthDp = 390, heightDp = 844)
@Composable
fun MyAuthenticatedScreenshot() {
    MyScreenshotContent(
        MyUiState(
            account =
                MyAccountUiState.Authenticated(
                    MyProfileUi(
                        userId = "42",
                        nickname = "阿珏",
                        avatarUrl = null,
                        vipLabel = "VIP",
                        vipUnavailable = false,
                    ),
                ),
            library = designLibrary(),
        ),
    )
}

@PreviewTest
@Preview(name = "Anonymous", widthDp = 390, heightDp = 844)
@Composable
fun MyAnonymousScreenshot() {
    MyScreenshotContent(MyUiState(account = MyAccountUiState.Anonymous))
}

@PreviewTest
@Preview(name = "AnonymousDark", widthDp = 390, heightDp = 844)
@Composable
fun MyAnonymousDarkScreenshot() {
    MyScreenshotContent(
        state = MyUiState(account = MyAccountUiState.Anonymous),
        themeMode = ThemeMode.Dark,
    )
}

@PreviewTest
@Preview(name = "AnonymousLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun MyAnonymousLargeTextScreenshot() {
    MyScreenshotContent(MyUiState(account = MyAccountUiState.Anonymous))
}

@PreviewTest
@Preview(name = "AuthenticatedLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun MyAuthenticatedLargeTextScreenshot() {
    MyScreenshotContent(
        MyUiState(
            account =
                MyAccountUiState.Authenticated(
                    MyProfileUi(
                        userId = "42",
                        nickname = "一位昵称比较长的用户",
                        avatarUrl = null,
                        vipLabel = null,
                        vipUnavailable = true,
                    ),
                ),
            library = designLibrary(),
        ),
    )
}

private fun designLibrary() =
    MyLibraryUi(
        likedCount = "126 首",
        recentCount = "78 首",
        localCount = "54 首",
        cloudSize = "2.4 GB",
        savedPlaylistCount = "36 个",
        savedAlbumCount = "28 张",
        followedArtistCount = "19 位",
        followedFriendCount = "12 位",
        playlists =
            listOf(
                MyPlaylistUi("我喜欢", "126 首", MyPlaylistArtwork.Liked),
                MyPlaylistUi("ACG 收藏", "82 首", MyPlaylistArtwork.Acg),
                MyPlaylistUi("夜间电台", "45 首", MyPlaylistArtwork.NightRadio),
            ),
    )

@Composable
private fun MyScreenshotContent(
    state: MyUiState,
    themeMode: ThemeMode = ThemeMode.Light,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
            MyScreen(
                state = state,
                onRefresh = {},
                onLogin = {},
                onLocalMusic = {},
                onSettings = {},
                onRequestLogout = {},
                onDismissLogout = {},
                onConfirmLogout = {},
                onFoundationLab = {},
                showFoundationLab = false,
            )
        }
    }
}
