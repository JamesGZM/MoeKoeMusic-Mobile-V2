package cn.james.music.feature.login

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthRiskChallenge
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "MobileCode", widthDp = 390, heightDp = 844)
@Composable
fun LoginMobileCodeScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            phone = "13800138000",
            code = "123456",
            countdownSeconds = 48,
            notice = LoginNotice.CodeSent,
        ),
    )
}

@PreviewTest
@Preview(name = "MultipleAccounts", widthDp = 390, heightDp = 844)
@Composable
fun LoginMultipleAccountsScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            phone = "13800138000",
            code = "123456",
            accounts = previewAccounts,
            selectedUserId = "10000002",
        ),
    )
}

@PreviewTest
@Preview(name = "MobileCodeLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LoginMobileCodeLargeTextScreenshot() {
    LoginScreenshotContent(LoginUiState())
}

@PreviewTest
@Preview(name = "PasswordRejected", widthDp = 390, heightDp = 844)
@Composable
fun LoginPasswordRejectedScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.Password,
            username = "miyu.song@moekoe.com",
            password = "fixture-password",
            notice = LoginNotice.PasswordRejected,
        ),
    )
}

@PreviewTest
@Preview(name = "PasswordRiskRequired", widthDp = 390, heightDp = 844)
@Composable
fun LoginPasswordRiskRequiredScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.Password,
            username = "miyu.song@moekoe.com",
            password = "fixture-password",
            risk = PasswordRiskUiState.Required(previewChallenge),
        ),
    )
}

@PreviewTest
@Preview(name = "RiskSms", widthDp = 390, heightDp = 844)
@Composable
fun LoginRiskSmsScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.Password,
            username = "miyu.song@moekoe.com",
            password = "fixture-password",
            risk = PasswordRiskUiState.Sms(previewChallenge),
            riskCode = "28",
        ),
    )
}

@PreviewTest
@Preview(name = "PasswordLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LoginPasswordLargeTextScreenshot() {
    LoginScreenshotContent(LoginUiState(mode = LoginMode.Password))
}

@Composable
private fun LoginScreenshotContent(state: LoginUiState) {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Surface {
            LoginScreen(
                state = state,
                onBack = {},
                onPhoneChange = {},
                onCodeChange = {},
                onModeChange = {},
                onSendCode = {},
                onSubmitMobileCode = {},
                onUsernameChange = {},
                onPasswordChange = {},
                onTogglePasswordVisibility = {},
                onSubmitPassword = {},
                onStartRiskVerification = {},
                onRiskCodeChange = {},
                onVerifyRiskCode = {},
                onCancelRisk = {},
                onSelectAccount = {},
                onChooseOtherAccount = {},
            )
        }
    }
}

private val previewAccounts =
    listOf(
        AuthAccountOption("10000001", "夏日旋律", null, "VIP"),
        AuthAccountOption("10000002", "MoeKoe", null, null),
        AuthAccountOption("10000003", "蓝色留声机", null, null),
    )

private val previewChallenge = AuthRiskChallenge("fixture-event", null, null)
