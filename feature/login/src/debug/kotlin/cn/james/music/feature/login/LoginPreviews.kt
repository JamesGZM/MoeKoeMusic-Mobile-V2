package cn.james.music.feature.login

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.QrLoginSession

@Preview(name = "Phone · Default", group = "Login", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginPhonePreview() {
    LoginPreviewContent(LoginUiState())
}

@Preview(name = "Phone · 3-button safe area", group = "Login Insets", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginPhoneNavigationSafeAreaPreview() {
    LoginPreviewContent(LoginUiState(), bottomSafeInset = 56.dp)
}

@Preview(name = "Phone · Countdown", group = "Login", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginPhoneCountdownPreview() {
    LoginPreviewContent(LoginUiState(phone = "13800138000", countdownSeconds = 48, notice = LoginNotice.CodeSent))
}

@Preview(name = "Phone · Invalid phone", group = "Login", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginPhoneInvalidPreview() {
    LoginPreviewContent(LoginUiState(phone = "138", notice = LoginNotice.Validation(LoginValidationError.InvalidPhone)))
}

@Preview(name = "Phone · Invalid code", group = "Login", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginCodeInvalidPreview() {
    LoginPreviewContent(
        LoginUiState(
            phone = "13800138000",
            code = "123",
            notice = LoginNotice.Validation(LoginValidationError.InvalidCode),
        ),
    )
}

@Preview(name = "Password · Rejected", group = "Login", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginPasswordRejectedPreview() {
    LoginPreviewContent(
        LoginUiState(
            mode = LoginMode.Password,
            username = "miyu.song@moekoe.com",
            password = "fixture-password",
            notice = LoginNotice.PasswordRejected,
        ),
    )
}

@Preview(name = "QR · Generating", group = "Login QR", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginQrGeneratingPreview() {
    LoginPreviewContent(LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Generating))
}

@Preview(name = "QR · Waiting", group = "Login QR", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginQrWaitingPreview() {
    LoginPreviewContent(
        LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Waiting(previewQrSession, 106)),
    )
}

@Preview(name = "QR · Scanned", group = "Login QR", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginQrScannedPreview() {
    LoginPreviewContent(
        LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Scanned(previewQrSession, "MoeKoe", 106)),
    )
}

@Preview(name = "QR · Expired", group = "Login QR", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginQrExpiredPreview() {
    LoginPreviewContent(LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Expired(previewQrSession)))
}

@Preview(name = "QR · Failure", group = "Login QR", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LoginQrFailurePreview() {
    LoginPreviewContent(LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Failure(AuthError.Connection)))
}

@Composable
private fun LoginPreviewContent(
    state: LoginUiState,
    bottomSafeInset: Dp? = null,
) {
    MoeKoeTheme {
        Surface {
            LoginScreen(
                state = state,
                bottomSafeInsetOverride = bottomSafeInset,
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

private val previewQrSession = QrLoginSession("preview-key", "https://example.test/qr-login/preview-key")
