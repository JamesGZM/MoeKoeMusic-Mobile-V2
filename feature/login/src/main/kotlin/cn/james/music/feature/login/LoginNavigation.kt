package cn.james.music.feature.login

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination

sealed interface TencentCaptchaResult {
    data class Success(
        val ticket: String,
        val randomString: String,
    ) : TencentCaptchaResult {
        override fun toString(): String = "TencentCaptchaResult.Success(ticket=<redacted>, randomString=<redacted>)"
    }

    data object Cancelled : TencentCaptchaResult

    data object Failure : TencentCaptchaResult
}

fun NavGraphBuilder.loginDestination(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    onLaunchTencentCaptcha: (String, (TencentCaptchaResult) -> Unit) -> Unit,
) {
    composable<LoginDestination> {
        val viewModel: LoginViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        BackHandler(enabled = state.hasActiveRisk) { viewModel.cancelRisk() }
        LaunchedEffect(viewModel) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    LoginEffect.Completed -> {
                        onLoggedIn()
                    }

                    is LoginEffect.LaunchTencentCaptcha -> {
                        onLaunchTencentCaptcha(effect.appId, viewModel::handleTencentCaptchaResult)
                    }
                }
            }
        }
        LoginScreen(
            state = state,
            onBack = onBack,
            onPhoneChange = viewModel::updatePhone,
            onCodeChange = viewModel::updateCode,
            onModeChange = viewModel::switchMode,
            onRefreshQrLogin = viewModel::refreshQrLogin,
            onSendCode = viewModel::sendCode,
            onSubmitMobileCode = viewModel::submitMobileCode,
            onUsernameChange = viewModel::updateUsername,
            onPasswordChange = viewModel::updatePassword,
            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
            onSubmitPassword = viewModel::submitPassword,
            onStartRiskVerification = viewModel::startRiskVerification,
            onRetryTencentVerification = viewModel::retryTencentVerification,
            onRiskCodeChange = viewModel::updateRiskCode,
            onVerifyRiskCode = viewModel::verifyRiskCode,
            onCancelRisk = viewModel::cancelRisk,
            onSelectAccount = viewModel::selectAccount,
            onChooseOtherAccount = viewModel::chooseOtherAccount,
        )
    }
}
