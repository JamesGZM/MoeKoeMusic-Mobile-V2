package cn.james.music.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

internal const val LOGIN_SCREEN_ROOT_TAG = "login_screen_root"

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    bottomSafeInsetOverride: Dp? = null,
    onBack: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onModeChange: (LoginMode) -> Unit,
    onRefreshQrLogin: () -> Unit,
    onSendCode: () -> Unit,
    onSubmitMobileCode: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmitPassword: () -> Unit,
    onStartRiskVerification: () -> Unit,
    onRetryTencentVerification: () -> Unit,
    onRiskCodeChange: (String) -> Unit,
    onVerifyRiskCode: () -> Unit,
    onCancelRisk: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onChooseOtherAccount: () -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag(LOGIN_SCREEN_ROOT_TAG),
    ) {
        val density = LocalDensity.current
        val bottomSafeInset =
            bottomSafeInsetOverride
                ?: with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val layout =
            LoginLayoutSpec.create(
                availableWidth = maxWidth,
                viewportHeight = maxHeight,
                bottomSafeInset = bottomSafeInset,
            )
        Box(
            modifier =
                Modifier
                    .width(layout.canvasWidth)
                    .fillMaxHeight()
                    .align(Alignment.Center),
        ) {
            LoginPageLayout(
                layout = layout,
                state = state,
                onBack = onBack,
                onPhoneChange = onPhoneChange,
                onCodeChange = onCodeChange,
                onModeChange = onModeChange,
                onRefreshQrLogin = onRefreshQrLogin,
                onSendCode = onSendCode,
                onSubmitMobileCode = onSubmitMobileCode,
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                onSubmitPassword = onSubmitPassword,
                onStartRiskVerification = onStartRiskVerification,
                onRetryTencentVerification = onRetryTencentVerification,
                onRiskCodeChange = onRiskCodeChange,
                onVerifyRiskCode = onVerifyRiskCode,
                onCancelRisk = onCancelRisk,
                onSelectAccount = onSelectAccount,
                onChooseOtherAccount = onChooseOtherAccount,
            )
        }
    }
}
