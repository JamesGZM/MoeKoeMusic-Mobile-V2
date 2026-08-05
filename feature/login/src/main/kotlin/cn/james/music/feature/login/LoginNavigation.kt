package cn.james.music.feature.login

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

fun NavGraphBuilder.loginDestination(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    composable<LoginDestination> {
        val viewModel: LoginViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(viewModel) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    LoginEffect.Completed -> onLoggedIn()
                }
            }
        }
        LoginScreen(
            state = state,
            onBack = onBack,
            onPhoneChange = viewModel::updatePhone,
            onCodeChange = viewModel::updateCode,
            onSendCode = viewModel::sendCode,
            onSubmit = viewModel::submit,
            onSelectAccount = viewModel::selectAccount,
            onChooseOtherAccount = viewModel::chooseOtherAccount,
        )
    }
}
