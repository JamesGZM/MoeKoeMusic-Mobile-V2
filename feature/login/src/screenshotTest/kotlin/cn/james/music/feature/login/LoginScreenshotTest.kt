package cn.james.music.feature.login

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthRiskChallenge
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.QrLoginSession
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "TencentCaptchaLoading", widthDp = 390, heightDp = 845)
@Composable
fun TencentCaptchaLoadingScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        TencentCaptchaScreen(onClose = {})
    }
}

@PreviewTest
@Preview(name = "TencentCaptchaLoadingLargeText", widthDp = 390, heightDp = 845, fontScale = 1.5f)
@Composable
fun TencentCaptchaLoadingLargeTextScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        TencentCaptchaScreen(onClose = {})
    }
}

@PreviewTest
@Preview(name = "MobileCode", widthDp = 390, heightDp = 845)
@Composable
fun LoginMobileCodeScreenshot() {
    LoginScreenshotContent(LoginUiState())
}

@PreviewTest
@Preview(name = "MobileCodeLayoutProbe", widthDp = 390, heightDp = 845)
@Composable
fun LoginMobileCodeLayoutProbeScreenshot() {
    LoginLayoutProbeScreenshot(
        state = LoginUiState(),
        colors = mapOf("card" to Color.Magenta, "selector" to Color.Cyan),
    )
}

@PreviewTest
@Preview(name = "MobileCodeSending", widthDp = 390, heightDp = 845)
@Composable
fun LoginMobileCodeSendingScreenshot() {
    LoginScreenshotContent(LoginUiState(phone = "13800138000", sendingCode = true))
}

@PreviewTest
@Preview(name = "MobileCodeCountdown", widthDp = 390, heightDp = 845)
@Composable
fun LoginMobileCodeCountdownScreenshot() {
    LoginScreenshotContent(previewMobileCodeCountdownState)
}

@PreviewTest
@Preview(name = "MobileCodeInput", widthDp = 390, heightDp = 845)
@Composable
fun LoginMobileCodeInputScreenshot() {
    LoginScreenshotContent(previewMobileCodeInputState)
}

@PreviewTest
@Preview(name = "MobileCodeSubmitting", widthDp = 390, heightDp = 845)
@Composable
fun LoginMobileCodeSubmittingScreenshot() {
    LoginScreenshotContent(previewMobileCodeInputState.copy(loggingIn = true))
}

@PreviewTest
@Preview(name = "MobileCodePhoneViewport", widthDp = 360, heightDp = 780)
@Composable
fun LoginMobileCodePhoneViewportScreenshot() {
    LoginScreenshotContent(LoginUiState())
}

@PreviewTest
@Preview(name = "MobileCodeDark", widthDp = 390, heightDp = 845)
@Composable
fun LoginMobileCodeDarkScreenshot() {
    LoginScreenshotContent(LoginUiState(), themeMode = ThemeMode.Dark)
}

@PreviewTest
@Preview(name = "MobileCodeAmoled", widthDp = 390, heightDp = 845)
@Composable
fun LoginMobileCodeAmoledScreenshot() {
    LoginScreenshotContent(LoginUiState(), themeMode = ThemeMode.Amoled)
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
            selectedUserId = "18600002258",
        ),
    )
}

@PreviewTest
@Preview(name = "MultipleAccountsLayoutProbe", widthDp = 390, heightDp = 844)
@Composable
fun LoginMultipleAccountsLayoutProbeScreenshot() {
    LoginLayoutProbeScreenshot(
        state =
            LoginUiState(
                phone = "13800138000",
                code = "123456",
                accounts = previewAccounts,
                selectedUserId = "18600002258",
            ),
        colors = mapOf("card" to Color.Magenta, "accountList" to Color.Green),
    )
}

@PreviewTest
@Preview(name = "MultipleAccountsUnselected", widthDp = 390, heightDp = 844)
@Composable
fun LoginMultipleAccountsUnselectedScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            phone = "13800138000",
            code = "123456",
            accounts = previewAccounts,
        ),
    )
}

@PreviewTest
@Preview(name = "MultipleAccountsFailure", widthDp = 390, heightDp = 844)
@Composable
fun LoginMultipleAccountsFailureScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            phone = "13800138000",
            code = "123456",
            accounts = previewAccounts,
            selectedUserId = "18600002258",
            notice = LoginNotice.Failure(AuthError.Rejected),
        ),
    )
}

@PreviewTest
@Preview(name = "MultipleAccountsFailureLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LoginMultipleAccountsFailureLargeTextScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            phone = "13800138000",
            code = "123456",
            accounts = previewAccounts,
            selectedUserId = "18600002258",
            notice = LoginNotice.Failure(AuthError.Rejected),
        ),
    )
}

@PreviewTest
@Preview(name = "MultipleAccountsFailureLargestText", widthDp = 390, heightDp = 844, fontScale = 2f)
@Composable
fun LoginMultipleAccountsFailureLargestTextScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            phone = "13800138000",
            code = "123456",
            accounts = previewAccounts,
            selectedUserId = "18600002258",
            notice = LoginNotice.Failure(AuthError.Rejected),
        ),
    )
}

@PreviewTest
@Preview(name = "MobileCodeLargeText", widthDp = 390, heightDp = 845, fontScale = 1.5f)
@Composable
fun LoginMobileCodeLargeTextScreenshot() {
    LoginScreenshotContent(LoginUiState())
}

@PreviewTest
@Preview(name = "Password", widthDp = 390, heightDp = 844)
@Composable
fun LoginPasswordScreenshot() {
    LoginScreenshotContent(LoginUiState(mode = LoginMode.Password))
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
@Preview(name = "PasswordSubmitting", widthDp = 390, heightDp = 844)
@Composable
fun LoginPasswordSubmittingScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.Password,
            username = "miyu.song@moekoe.com",
            password = "fixture-password",
            passwordLoggingIn = true,
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
        ),
    )
}

@PreviewTest
@Preview(name = "RiskSmsSubmitting", widthDp = 390, heightDp = 844)
@Composable
fun LoginRiskSmsSubmittingScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.Password,
            username = "miyu.song@moekoe.com",
            password = "fixture-password",
            risk = PasswordRiskUiState.Sms(previewChallenge),
            riskCode = "246810",
            verifyingRisk = true,
        ),
    )
}

@PreviewTest
@Preview(name = "RiskSmsRejected", widthDp = 390, heightDp = 844)
@Composable
fun LoginRiskSmsRejectedScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.Password,
            username = "miyu.song@moekoe.com",
            password = "fixture-password",
            risk = PasswordRiskUiState.Sms(previewChallenge),
            riskCode = "246810",
            notice = LoginNotice.RiskRejected,
        ),
    )
}

@PreviewTest
@Preview(name = "RiskTencent", widthDp = 390, heightDp = 844)
@Composable
fun LoginRiskTencentScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.Password,
            username = "miyu.song@moekoe.com",
            password = "fixture-password",
            risk = PasswordRiskUiState.Tencent(previewChallenge, "123456789"),
            notice = LoginNotice.RiskRejected,
        ),
    )
}

@PreviewTest
@Preview(name = "PasswordLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LoginPasswordLargeTextScreenshot() {
    LoginScreenshotContent(LoginUiState(mode = LoginMode.Password))
}

@PreviewTest
@Preview(name = "QrGenerating", widthDp = 390, heightDp = 844)
@Composable
fun LoginQrGeneratingScreenshot() {
    LoginScreenshotContent(LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Generating))
}

@PreviewTest
@Preview(name = "QrGeneratingLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LoginQrGeneratingLargeTextScreenshot() {
    LoginScreenshotContent(LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Generating))
}

@PreviewTest
@Preview(name = "QrWaiting", widthDp = 390, heightDp = 844)
@Composable
fun LoginQrWaitingScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.QrCode,
            qrLogin = QrLoginUiState.Waiting(previewQrSession, remainingSeconds = 106),
        ),
    )
}

@PreviewTest
@Preview(name = "QrScanned", widthDp = 390, heightDp = 844)
@Composable
fun LoginQrScannedScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.QrCode,
            qrLogin = QrLoginUiState.Scanned(previewQrSession, nickname = "MoeKoe", remainingSeconds = 94),
        ),
    )
}

@PreviewTest
@Preview(name = "QrExpired", widthDp = 390, heightDp = 844)
@Composable
fun LoginQrExpiredScreenshot() {
    LoginScreenshotContent(LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Expired(previewQrSession)))
}

@PreviewTest
@Preview(name = "QrExpiredLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LoginQrExpiredLargeTextScreenshot() {
    LoginScreenshotContent(LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Expired(previewQrSession)))
}

@PreviewTest
@Preview(name = "QrFailure", widthDp = 390, heightDp = 844)
@Composable
fun LoginQrFailureScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.QrCode,
            qrLogin = QrLoginUiState.Failure(AuthError.Connection),
        ),
    )
}

@PreviewTest
@Preview(name = "QrFailureLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LoginQrFailureLargeTextScreenshot() {
    LoginScreenshotContent(
        LoginUiState(
            mode = LoginMode.QrCode,
            qrLogin = QrLoginUiState.Failure(AuthError.Connection),
        ),
    )
}

@Composable
private fun LoginScreenshotContent(
    state: LoginUiState,
    themeMode: ThemeMode = ThemeMode.Light,
) {
    MoeKoeTheme(themeMode = themeMode) {
        CompositionLocalProvider(
            LocalAccountAvatarModels provides
                mapOf(
                    "18600000721" to R.drawable.login_account_avatar_navy,
                    "18600002258" to R.drawable.login_account_avatar_summer,
                    "18600005533" to R.drawable.login_account_avatar_pink,
                ),
        ) {
            Surface {
                LoginScreen(
                    state = state,
                    onBack = {},
                    onPhoneChange = {},
                    onCodeChange = {},
                    onModeChange = {},
                    onRefreshQrLogin = {},
                    onSendCode = {},
                    onSubmitMobileCode = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmitPassword = {},
                    onStartRiskVerification = {},
                    onRetryTencentVerification = {},
                    onRiskCodeChange = {},
                    onVerifyRiskCode = {},
                    onCancelRisk = {},
                    onSelectAccount = {},
                    onChooseOtherAccount = {},
                )
            }
        }
    }
}

@Composable
private fun LoginLayoutProbeScreenshot(
    state: LoginUiState,
    colors: Map<String, Color>,
) {
    CompositionLocalProvider(LocalLoginLayoutProbeColors provides colors) {
        LoginScreenshotContent(state)
    }
}

private val previewMobileCodeCountdownState =
    LoginUiState(
        phone = "13800138000",
        code = "123456",
        countdownSeconds = 48,
        notice = LoginNotice.CodeSent,
    )

private val previewMobileCodeInputState =
    LoginUiState(
        phone = "13800138000",
        code = "123456",
    )

private val previewAccounts =
    listOf(
        AuthAccountOption("18600000721", "小萌酱", null, "VIP"),
        AuthAccountOption("18600002258", "海风与音乐", null, null),
        AuthAccountOption("18600005533", "MoeKoe玩家", null, null),
    )

private val previewChallenge = AuthRiskChallenge("fixture-event", null, null)
private val previewQrSession = QrLoginSession("fixture-key", "https://example.test/qr-login/fixture-key")
