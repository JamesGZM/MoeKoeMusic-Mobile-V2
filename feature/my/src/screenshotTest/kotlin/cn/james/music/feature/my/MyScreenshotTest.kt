package cn.james.music.feature.my

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
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
@Preview(name = "AuthenticatedLayoutProbe", widthDp = 390, heightDp = 844)
@Composable
fun MyAuthenticatedLayoutProbeScreenshot() {
    CompositionLocalProvider(
        LocalMyLayoutProbeColors provides
            mapOf(
                MY_PROBE_ACCOUNT_CARD to Color.Magenta,
                MY_PROBE_QUICK_ENTRIES to Color.Cyan,
                MY_PROBE_COLLECTION_GRID to Color.Green,
                MY_PROBE_PLAYLIST_SECTION to Color(0xFF0102FD),
                MY_PROBE_PLAYLIST_ROW_2 to Color(0xFFFD0201),
                MY_PROBE_PLAYLIST_ROW_3 to Color(0xFF02FD7F),
            ),
    ) {
        MyAuthenticatedScreenshot()
    }
}

@PreviewTest
@Preview(name = "Anonymous", widthDp = 390, heightDp = 844)
@Composable
fun MyAnonymousScreenshot() {
    MyScreenshotContent(anonymousState())
}

@PreviewTest
@Preview(name = "AnonymousDark", widthDp = 390, heightDp = 844)
@Composable
fun MyAnonymousDarkScreenshot() {
    MyScreenshotContent(
        state = anonymousState(),
        themeMode = ThemeMode.Dark,
    )
}

@PreviewTest
@Preview(name = "AnonymousLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun MyAnonymousLargeTextScreenshot() {
    MyScreenshotContent(anonymousState())
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
        quickEntries =
            myQuickEntries(
                accountAssetsAvailable = false,
                supportingText =
                    mapOf(
                        MyEntryId.Liked to "126 首",
                        MyEntryId.Recent to "78 首",
                        MyEntryId.LocalMusic to "54 首",
                        MyEntryId.Cloud to "2.4 GB",
                    ),
            ),
        collectionEntries =
            myCollectionEntries(
                accountAssetsAvailable = false,
                supportingText =
                    mapOf(
                        MyEntryId.SavedPlaylists to "36 个",
                        MyEntryId.SavedAlbums to "28 张",
                        MyEntryId.FollowedArtists to "19 位",
                        MyEntryId.FollowedFriends to "12 位",
                    ),
            ),
        playlists =
            listOf(
                MyPlaylistUi("liked", "我喜欢", "126 首", MyPlaylistArtwork.Liked),
                MyPlaylistUi("acg", "ACG 收藏", "82 首", MyPlaylistArtwork.Acg),
                MyPlaylistUi("night-radio", "夜间电台", "45 首", MyPlaylistArtwork.NightRadio),
            ),
    )

private fun anonymousState() =
    MyUiState(
        account = MyAccountUiState.Anonymous,
        library =
            MyLibraryUi(
                quickEntries = myQuickEntries(accountAssetsAvailable = true),
                collectionEntries = myCollectionEntries(accountAssetsAvailable = true),
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
                onAction = {},
                showFoundationLab = false,
            )
        }
    }
}
