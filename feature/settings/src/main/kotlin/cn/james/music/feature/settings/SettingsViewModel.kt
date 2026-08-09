package cn.james.music.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.settings.AppSettingsProblem
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel
    @Inject
    constructor(
        private val repository: AppSettingsRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SettingsUiState())
        val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

        private var themeUpdateJob: Job? = null
        private var autoSkipUpdateJob: Job? = null
        private var dynamicCoverColorsUpdateJob: Job? = null
        private var retryRequest: RetryRequest? = null
        private var themeUpdateGeneration = 0L
        private var autoSkipUpdateGeneration = 0L
        private var dynamicCoverColorsUpdateGeneration = 0L
        private var persistedTheme = AppThemePreference.System
        private var persistedAutoSkipFailedPlayback = true
        private var persistedDynamicCoverColors = true

        init {
            viewModelScope.launch {
                repository.settings.collect { snapshot ->
                    persistedTheme = snapshot.settings.theme
                    persistedAutoSkipFailedPlayback = snapshot.settings.autoSkipFailedPlayback
                    persistedDynamicCoverColors = snapshot.settings.dynamicCoverColors
                    mutableState.update { current ->
                        val theme =
                            if (current.savingTheme == null || current.savingTheme.toDomain() == snapshot.settings.theme) {
                                snapshot.settings.theme.toUi()
                            } else {
                                current.theme
                            }
                        val autoSkip =
                            if (
                                current.savingAutoSkipFailedPlayback == null ||
                                current.savingAutoSkipFailedPlayback == snapshot.settings.autoSkipFailedPlayback
                            ) {
                                snapshot.settings.autoSkipFailedPlayback
                            } else {
                                current.autoSkipFailedPlayback
                            }
                        val dynamicColors =
                            if (
                                current.savingDynamicCoverColors == null ||
                                current.savingDynamicCoverColors == snapshot.settings.dynamicCoverColors
                            ) {
                                snapshot.settings.dynamicCoverColors
                            } else {
                                current.dynamicCoverColors
                            }
                        current.withGroups(
                            theme = theme,
                            autoSkipFailedPlayback = autoSkip,
                            dynamicCoverColors = dynamicColors,
                            problem = if (current.problem == SettingsProblemUi.Write) current.problem else snapshot.problem?.toUi(),
                        )
                    }
                }
            }
        }

        fun onAction(action: SettingsAction) {
            when (action) {
                SettingsAction.Back -> Unit
                is SettingsAction.SelectTheme -> selectTheme(action.theme)
                SettingsAction.OpenTheme -> mutableState.update { it.copy(overlay = SettingsOverlay.ThemeSelection) }
                SettingsAction.OpenAbout -> mutableState.update { it.copy(overlay = SettingsOverlay.About) }
                is SettingsAction.SetAutoSkipFailedPlayback -> setAutoSkipFailedPlayback(action.enabled)
                is SettingsAction.SetDynamicCoverColors -> setDynamicCoverColors(action.enabled)
                SettingsAction.DismissOverlay -> mutableState.update { it.copy(overlay = null) }
                SettingsAction.Retry -> retry()
                SettingsAction.DismissProblem -> dismissProblem()
            }
        }

        private fun selectTheme(theme: SettingsThemeUi) {
            mutableState.update { it.copy(overlay = null) }
            if (theme == mutableState.value.theme && mutableState.value.savingTheme == null) return
            val generation = ++themeUpdateGeneration
            themeUpdateJob?.cancel()
            themeUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingTheme = theme, problem = null, canRetry = false)
                    }
                    when (repository.setTheme(theme.toDomain())) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == themeUpdateGeneration) {
                                mutableState.update { current -> current.withGroups(theme = theme, savingTheme = null) }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == themeUpdateGeneration) {
                                retryRequest = RetryRequest.Theme(theme)
                                mutableState.update { current ->
                                    current.withGroups(
                                        theme = persistedTheme.toUi(),
                                        savingTheme = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun setAutoSkipFailedPlayback(enabled: Boolean) {
            if (enabled == mutableState.value.autoSkipFailedPlayback && mutableState.value.savingAutoSkipFailedPlayback == null) return
            val generation = ++autoSkipUpdateGeneration
            autoSkipUpdateJob?.cancel()
            autoSkipUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingAutoSkipFailedPlayback = enabled, problem = null, canRetry = false)
                    }
                    when (repository.setAutoSkipFailedPlayback(enabled)) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == autoSkipUpdateGeneration) {
                                mutableState.update { current ->
                                    current.withGroups(autoSkipFailedPlayback = enabled, savingAutoSkipFailedPlayback = null)
                                }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == autoSkipUpdateGeneration) {
                                retryRequest = RetryRequest.AutoSkipFailedPlayback(enabled)
                                mutableState.update { current ->
                                    current.withGroups(
                                        autoSkipFailedPlayback = persistedAutoSkipFailedPlayback,
                                        savingAutoSkipFailedPlayback = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun setDynamicCoverColors(enabled: Boolean) {
            if (enabled == mutableState.value.dynamicCoverColors && mutableState.value.savingDynamicCoverColors == null) return
            val generation = ++dynamicCoverColorsUpdateGeneration
            dynamicCoverColorsUpdateJob?.cancel()
            dynamicCoverColorsUpdateJob =
                viewModelScope.launch {
                    retryRequest = null
                    mutableState.update { current ->
                        current.withGroups(savingDynamicCoverColors = enabled, problem = null, canRetry = false)
                    }
                    when (repository.setDynamicCoverColors(enabled)) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation == dynamicCoverColorsUpdateGeneration) {
                                mutableState.update { current ->
                                    current.withGroups(dynamicCoverColors = enabled, savingDynamicCoverColors = null)
                                }
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation == dynamicCoverColorsUpdateGeneration) {
                                retryRequest = RetryRequest.DynamicCoverColors(enabled)
                                mutableState.update { current ->
                                    current.withGroups(
                                        dynamicCoverColors = persistedDynamicCoverColors,
                                        savingDynamicCoverColors = null,
                                        problem = SettingsProblemUi.Write,
                                        canRetry = true,
                                    )
                                }
                            }
                        }
                    }
                }
        }

        private fun retry() {
            when (val request = retryRequest) {
                null -> Unit
                is RetryRequest.Theme -> selectTheme(request.theme)
                is RetryRequest.AutoSkipFailedPlayback -> setAutoSkipFailedPlayback(request.enabled)
                is RetryRequest.DynamicCoverColors -> setDynamicCoverColors(request.enabled)
            }
        }

        private fun dismissProblem() {
            retryRequest = null
            mutableState.update { it.copy(problem = null, canRetry = false) }
        }

        private sealed interface RetryRequest {
            data class Theme(
                val theme: SettingsThemeUi,
            ) : RetryRequest

            data class AutoSkipFailedPlayback(
                val enabled: Boolean,
            ) : RetryRequest

            data class DynamicCoverColors(
                val enabled: Boolean,
            ) : RetryRequest
        }

        private fun SettingsUiState.withGroups(
            theme: SettingsThemeUi = this.theme,
            autoSkipFailedPlayback: Boolean = this.autoSkipFailedPlayback,
            dynamicCoverColors: Boolean = this.dynamicCoverColors,
            savingTheme: SettingsThemeUi? = this.savingTheme,
            savingAutoSkipFailedPlayback: Boolean? = this.savingAutoSkipFailedPlayback,
            savingDynamicCoverColors: Boolean? = this.savingDynamicCoverColors,
            problem: SettingsProblemUi? = this.problem,
            canRetry: Boolean = this.canRetry,
            overlay: SettingsOverlay? = this.overlay,
        ): SettingsUiState =
            copy(
                theme = theme,
                autoSkipFailedPlayback = autoSkipFailedPlayback,
                dynamicCoverColors = dynamicCoverColors,
                savingTheme = savingTheme,
                savingAutoSkipFailedPlayback = savingAutoSkipFailedPlayback,
                savingDynamicCoverColors = savingDynamicCoverColors,
                problem = problem,
                canRetry = canRetry,
                overlay = overlay,
                groups =
                    settingsGroups(
                        theme = theme,
                        autoSkipFailedPlayback = autoSkipFailedPlayback,
                        dynamicCoverColors = dynamicCoverColors,
                        savingTheme = savingTheme,
                        savingAutoSkipFailedPlayback = savingAutoSkipFailedPlayback,
                        savingDynamicCoverColors = savingDynamicCoverColors,
                    ),
            )

        private fun AppThemePreference.toUi(): SettingsThemeUi =
            when (this) {
                AppThemePreference.System -> SettingsThemeUi.System
                AppThemePreference.Light -> SettingsThemeUi.Light
                AppThemePreference.Dark -> SettingsThemeUi.Dark
                AppThemePreference.Amoled -> SettingsThemeUi.Amoled
            }

        private fun SettingsThemeUi.toDomain(): AppThemePreference =
            when (this) {
                SettingsThemeUi.System -> AppThemePreference.System
                SettingsThemeUi.Light -> AppThemePreference.Light
                SettingsThemeUi.Dark -> AppThemePreference.Dark
                SettingsThemeUi.Amoled -> AppThemePreference.Amoled
            }

        private fun AppSettingsProblem.toUi(): SettingsProblemUi =
            when (this) {
                AppSettingsProblem.Read -> SettingsProblemUi.Read
                AppSettingsProblem.Write -> SettingsProblemUi.Write
            }
    }
