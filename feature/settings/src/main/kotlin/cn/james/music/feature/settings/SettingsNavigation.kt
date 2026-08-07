package cn.james.music.feature.settings

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination

fun NavGraphBuilder.settingsDestination(onBack: () -> Unit) {
    composable<SettingsDestination> {
        val viewModel: SettingsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SettingsScreen(
            state = state,
            onBack = onBack,
            onThemeSelected = viewModel::selectTheme,
            onRetry = viewModel::retry,
            onDismissProblem = viewModel::dismissProblem,
        )
    }
}
