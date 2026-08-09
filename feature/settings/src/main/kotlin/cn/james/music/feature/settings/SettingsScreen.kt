package cn.james.music.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarTone
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            MoeStandardTopBar(
                title = stringResource(R.string.settings_title),
                navigationContentDescription = stringResource(R.string.settings_back),
                onNavigateBack = { onAction(SettingsAction.Back) },
                modifier = Modifier.settingsLayoutProbe(SETTINGS_PROBE_TOOLBAR),
            )
        },
        snackbarHost = {
            state.problem?.let { problem ->
                MoeSnackbar(
                    message =
                        stringResource(
                            when (problem) {
                                SettingsProblemUi.Read -> R.string.settings_read_failed
                                SettingsProblemUi.Write -> R.string.settings_write_failed
                            },
                        ),
                    modifier = Modifier.padding(MoeKoeTheme.spacing.space16),
                    tone = MoeSnackbarTone.Error,
                    actionLabel = if (state.canRetry) stringResource(R.string.settings_retry) else stringResource(R.string.settings_dismiss),
                    onAction = {
                        onAction(if (state.canRetry) SettingsAction.Retry else SettingsAction.DismissProblem)
                    },
                )
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            SettingsContent(
                groups = state.groups,
                onAction = onAction,
            )
        }
    }

    when (state.overlay) {
        SettingsOverlay.ThemeSelection ->
            ThemeSelectionDialog(
                selected = state.theme,
                saving = state.savingTheme,
                onAction = onAction,
            )
        SettingsOverlay.About -> AboutDialog(onAction = onAction)
        null -> Unit
    }
}
