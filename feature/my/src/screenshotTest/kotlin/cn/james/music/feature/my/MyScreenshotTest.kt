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
        ),
    )
}

@Composable
private fun MyScreenshotContent(state: MyUiState) {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Surface {
            MyScreen(
                state = state,
                onRefresh = {},
                onLogin = {},
                onLocalMusic = {},
                onAccountSettings = {},
                onDismissLogout = {},
                onConfirmLogout = {},
                onFoundationLab = {},
                showFoundationLab = false,
            )
        }
    }
}
