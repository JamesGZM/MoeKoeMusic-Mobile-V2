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
        private var updateJob: Job? = null
        private var retryTheme: SettingsThemeUi? = null
        private var updateGeneration = 0L
        private var persistedTheme = AppThemePreference.System

        init {
            viewModelScope.launch {
                repository.settings.collect { snapshot ->
                    persistedTheme = snapshot.settings.theme
                    mutableState.update { current ->
                        val theme =
                            if (current.savingTheme == null || current.savingTheme.toDomain() == snapshot.settings.theme) {
                                snapshot.settings.theme.toUi()
                            } else {
                                current.theme
                            }
                        current.copy(
                            theme = theme,
                            problem = if (current.problem == SettingsProblemUi.Write) current.problem else snapshot.problem?.toUi(),
                            groups = settingsGroups(theme, current.savingTheme),
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
                SettingsAction.DismissOverlay -> mutableState.update { it.copy(overlay = null) }
                SettingsAction.Retry -> retry()
                SettingsAction.DismissProblem -> dismissProblem()
            }
        }

        private fun selectTheme(theme: SettingsThemeUi) {
            mutableState.update { it.copy(overlay = null) }
            if (theme == mutableState.value.theme && mutableState.value.savingTheme == null) return
            val generation = ++updateGeneration
            updateJob?.cancel()
            updateJob =
                viewModelScope.launch {
                    retryTheme = null
                    mutableState.update {
                        it.copy(
                            savingTheme = theme,
                            problem = null,
                            canRetry = false,
                            groups = settingsGroups(it.theme, theme),
                        )
                    }
                    when (repository.setTheme(theme.toDomain())) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation != updateGeneration) return@launch
                            mutableState.update {
                                val persisted = persistedTheme.toUi()
                                it.copy(theme = persisted, savingTheme = null, groups = settingsGroups(persisted))
                            }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation != updateGeneration) return@launch
                            retryTheme = theme
                            mutableState.update {
                                it.copy(
                                    theme = persistedTheme.toUi(),
                                    savingTheme = null,
                                    problem = SettingsProblemUi.Write,
                                    canRetry = true,
                                    groups = settingsGroups(persistedTheme.toUi()),
                                )
                            }
                        }
                    }
                }
        }

        private fun retry() {
            retryTheme?.let(::selectTheme)
        }

        private fun dismissProblem() {
            retryTheme = null
            mutableState.update { it.copy(problem = null, canRetry = false) }
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
