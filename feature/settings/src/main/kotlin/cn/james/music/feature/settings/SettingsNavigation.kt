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
            onAction = { action ->
                when (action) {
                    SettingsAction.Back -> onBack()

                    is SettingsAction.SelectTheme,
                    is SettingsAction.SetAutoSkipFailedPlayback,
                    is SettingsAction.SetDynamicCoverColors,
                    is SettingsAction.SetShowLyricsSupplementalText,
                    is SettingsAction.SelectLyricsTextSize,
                    SettingsAction.OpenTheme,
                    SettingsAction.OpenLyricsTextSize,
                    SettingsAction.OpenAbout,
                    SettingsAction.DismissOverlay,
                    SettingsAction.Retry,
                    SettingsAction.DismissProblem,
                    -> viewModel.onAction(action)
                }
            },
        )
    }
}
