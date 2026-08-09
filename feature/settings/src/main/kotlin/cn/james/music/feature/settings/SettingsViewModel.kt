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
        private var retryRequest: RetryRequest? = null
        private var themeUpdateGeneration = 0L
        private var autoSkipUpdateGeneration = 0L
        private var persistedTheme = AppThemePreference.System
        private var persistedAutoSkipFailedPlayback = true

        init {
            viewModelScope.launch {
                repository.settings.collect { snapshot ->
                    persistedTheme = snapshot.settings.theme
                    persistedAutoSkipFailedPlayback = snapshot.settings.autoSkipFailedPlayback
                    mutableState.update { current ->
                        val theme =
                            if (current.savingTheme == null || current.savingTheme.toDomain() == snapshot.settings.theme) {
                                snapshot.settings.theme.toUi()
                            } else {
                                current.theme
                            }
                        val autoSkipFailedPlayback =
                            if (
                                current.savingAutoSkipFailedPlayback == null ||
                                current.savingAutoSkipFailedPlayback == snapshot.settings.autoSkipFailedPlayback
                            ) {
                                snapshot.settings.autoSkipFailedPlayback
                            } else {
                                current.autoSkipFailedPlayback
                            }
                        current.copy(
                            theme = theme,
                            autoSkipFailedPlayback = autoSkipFailedPlayback,
                            problem = if (current.problem == SettingsProblemUi.Write) current.problem else snapshot.problem?.toUi(),
                            groups =
                                settingsGroups(
                                    theme = theme,
                                    autoSkipFailedPlayback = autoSkipFailedPlayback,
                                    savingTheme = current.savingTheme,
                                    savingAutoSkipFailedPlayback = current.savingAutoSkipFailedPlayback,
                                ),
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
                    mutableState.update {
                        it.copy(
                            savingTheme = theme,
                            problem = null,
                            canRetry = false,
                            groups =
                                settingsGroups(
                                    theme = it.theme,
                                    autoSkipFailedPlayback = it.autoSkipFailedPlayback,
                                    savingTheme = theme,
                                    savingAutoSkipFailedPlayback = it.savingAutoSkipFailedPlayback,
                                ),
                        )
                    }
                    when (repository.setTheme(theme.toDomain())) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation != themeUpdateGeneration) return@launch
                            mutableState.update {
                                it.copy(
                                    theme = theme,
                                    savingTheme = null,
                                    groups =
                                        settingsGroups(
                                            theme = theme,
                                            autoSkipFailedPlayback = it.autoSkipFailedPlayback,
                                            savingAutoSkipFailedPlayback = it.savingAutoSkipFailedPlayback,
                                        ),
                                )
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation != themeUpdateGeneration) return@launch
                            retryRequest = RetryRequest.Theme(theme)
                            mutableState.update {
                                val persisted = persistedTheme.toUi()
                                it.copy(
                                    theme = persisted,
                                    savingTheme = null,
                                    problem = SettingsProblemUi.Write,
                                    canRetry = true,
                                    groups =
                                        settingsGroups(
                                            theme = persisted,
                                            autoSkipFailedPlayback = it.autoSkipFailedPlayback,
                                            savingAutoSkipFailedPlayback = it.savingAutoSkipFailedPlayback,
                                        ),
                                )
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
                    mutableState.update {
                        it.copy(
                            savingAutoSkipFailedPlayback = enabled,
                            problem = null,
                            canRetry = false,
                            groups =
                                settingsGroups(
                                    theme = it.theme,
                                    autoSkipFailedPlayback = it.autoSkipFailedPlayback,
                                    savingTheme = it.savingTheme,
                                    savingAutoSkipFailedPlayback = enabled,
                                ),
                        )
                    }
                    when (repository.setAutoSkipFailedPlayback(enabled)) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation != autoSkipUpdateGeneration) return@launch
                            mutableState.update {
                                it.copy(
                                    autoSkipFailedPlayback = enabled,
                                    savingAutoSkipFailedPlayback = null,
                                    groups =
                                        settingsGroups(
                                            theme = it.theme,
                                            autoSkipFailedPlayback = enabled,
                                            savingTheme = it.savingTheme,
                                        ),
                                )
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation != autoSkipUpdateGeneration) return@launch
                            retryRequest = RetryRequest.AutoSkipFailedPlayback(enabled)
                            mutableState.update {
                                it.copy(
                                    autoSkipFailedPlayback = persistedAutoSkipFailedPlayback,
                                    savingAutoSkipFailedPlayback = null,
                                    problem = SettingsProblemUi.Write,
                                    canRetry = true,
                                    groups =
                                        settingsGroups(
                                            theme = it.theme,
                                            autoSkipFailedPlayback = persistedAutoSkipFailedPlayback,
                                            savingTheme = it.savingTheme,
                                        ),
                                )
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
        }

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
