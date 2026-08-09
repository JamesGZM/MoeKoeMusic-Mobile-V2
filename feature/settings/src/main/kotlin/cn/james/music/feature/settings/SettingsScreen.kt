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
import cn.james.music.core.designsystem.component.MoeSnackbarActionId
import cn.james.music.core.designsystem.component.MoeSnackbarActionUiModel
import cn.james.music.core.designsystem.component.MoeSnackbarEvent
import cn.james.music.core.designsystem.component.MoeSnackbarTone
import cn.james.music.core.designsystem.component.MoeSnackbarUiModel
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarEvent
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarUiModel

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            MoeStandardTopBar(
                model =
                    MoeStandardTopBarUiModel(
                        title = stringResource(R.string.settings_title),
                        navigationContentDescription = stringResource(R.string.settings_back),
                    ),
                onEvent = { event ->
                    if (event == MoeStandardTopBarEvent.NavigateBack) onAction(SettingsAction.Back)
                },
                modifier = Modifier.settingsLayoutProbe(SETTINGS_PROBE_TOOLBAR),
            )
        },
        snackbarHost = {
            state.problem?.let { problem ->
                MoeSnackbar(
                    model =
                        MoeSnackbarUiModel(
                            message =
                                stringResource(
                                    when (problem) {
                                        SettingsProblemUi.Read -> R.string.settings_read_failed
                                        SettingsProblemUi.Write -> R.string.settings_write_failed
                                    },
                                ),
                            tone = MoeSnackbarTone.Error,
                            action =
                                MoeSnackbarActionUiModel(
                                    id = if (state.canRetry) MoeSnackbarActionId.Retry else MoeSnackbarActionId.Dismiss,
                                    label =
                                        if (state.canRetry) {
                                            stringResource(R.string.settings_retry)
                                        } else {
                                            stringResource(R.string.settings_dismiss)
                                        },
                                ),
                        ),
                    onEvent = { event ->
                        when (event) {
                            is MoeSnackbarEvent.Action ->
                                when (event.id) {
                                    MoeSnackbarActionId.Retry -> onAction(SettingsAction.Retry)
                                    MoeSnackbarActionId.Dismiss -> onAction(SettingsAction.DismissProblem)
                                    MoeSnackbarActionId.Undo -> Unit
                                }
                        }
                    },
                    modifier = Modifier.padding(MoeKoeTheme.spacing.space16),
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
        SettingsOverlay.LyricsTextSizeSelection ->
            LyricsTextSizeSelectionDialog(
                selected = state.lyricsTextSize,
                saving = state.savingLyricsTextSize,
                onAction = onAction,
            )
        SettingsOverlay.About -> AboutDialog(onAction = onAction)
        null -> Unit
    }
}
