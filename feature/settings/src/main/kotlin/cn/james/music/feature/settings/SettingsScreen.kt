package cn.james.music.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarActionId
import cn.james.music.core.designsystem.component.MoeSnackbarActionUiModel
import cn.james.music.core.designsystem.component.MoeSnackbarEvent
import cn.james.music.core.designsystem.component.MoeSnackbarIcon
import cn.james.music.core.designsystem.component.MoeSnackbarTone
import cn.james.music.core.designsystem.component.MoeSnackbarUiModel
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarEvent
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarUiModel
import kotlinx.coroutines.delay

private const val CACHE_CLEAR_SUCCESS_FEEDBACK_DURATION_MILLIS = 4_000L

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
            when (val feedback = state.cacheClearFeedback) {
                SettingsCacheClearFeedbackUi.Cleared -> {
                    LaunchedEffect(feedback) {
                        delay(CACHE_CLEAR_SUCCESS_FEEDBACK_DURATION_MILLIS)
                        onAction(SettingsAction.DismissCacheClearFeedback)
                    }
                    MoeSnackbar(
                        model =
                            MoeSnackbarUiModel(
                                message = stringResource(R.string.settings_clear_cache_success),
                                tone = MoeSnackbarTone.Success,
                                icon = MoeSnackbarIcon.Confirmation,
                            ),
                        onEvent = {},
                        modifier = Modifier.padding(MoeKoeTheme.spacing.space16),
                    )
                }

                SettingsCacheClearFeedbackUi.PartiallyCleared,
                SettingsCacheClearFeedbackUi.Failed,
                ->
                    MoeSnackbar(
                        model =
                            MoeSnackbarUiModel(
                                message =
                                    stringResource(
                                        when (feedback) {
                                            SettingsCacheClearFeedbackUi.PartiallyCleared -> R.string.settings_clear_cache_partial_failed
                                            SettingsCacheClearFeedbackUi.Failed -> R.string.settings_clear_cache_failed
                                            SettingsCacheClearFeedbackUi.Cleared -> error("Handled above")
                                        },
                                    ),
                                tone = MoeSnackbarTone.Error,
                                icon = MoeSnackbarIcon.Warning,
                                action = MoeSnackbarActionUiModel(MoeSnackbarActionId.Retry, stringResource(R.string.settings_retry)),
                            ),
                        onEvent = { event ->
                            if (event is MoeSnackbarEvent.Action && event.id == MoeSnackbarActionId.Retry) {
                                onAction(SettingsAction.Retry)
                            }
                        },
                        modifier = Modifier.padding(MoeKoeTheme.spacing.space16),
                    )

                null ->
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
        SettingsOverlay.PlaybackQualitySelection ->
            PlaybackQualitySelectionDialog(
                selected = state.playbackQuality,
                saving = state.savingPlaybackQuality,
                onAction = onAction,
            )
        SettingsOverlay.BrandThemeColorSelection ->
            BrandThemeColorSelectionDialog(
                selected = state.brandThemeColor,
                saving = state.savingBrandThemeColor,
                onAction = onAction,
            )
        SettingsOverlay.ClearCacheConfirmation ->
            ClearCacheConfirmationDialog(
                clearing = state.clearingCache,
                onAction = onAction,
            )
        SettingsOverlay.About -> AboutDialog(onAction = onAction)
        null -> Unit
    }
}
