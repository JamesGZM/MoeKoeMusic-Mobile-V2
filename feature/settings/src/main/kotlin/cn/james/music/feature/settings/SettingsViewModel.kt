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

internal data class SettingsUiState(
    val theme: AppThemePreference = AppThemePreference.System,
    val savingTheme: AppThemePreference? = null,
    val problem: AppSettingsProblem? = null,
    val canRetry: Boolean = false,
    val overlay: SettingsOverlay? = null,
)

internal sealed interface SettingsOverlay {
    data object ThemeSelection : SettingsOverlay

    data object About : SettingsOverlay
}

@HiltViewModel
internal class SettingsViewModel
    @Inject
    constructor(
        private val repository: AppSettingsRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SettingsUiState())
        val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()
        private var updateJob: Job? = null
        private var retryTheme: AppThemePreference? = null
        private var updateGeneration = 0L
        private var persistedTheme = AppThemePreference.System

        init {
            viewModelScope.launch {
                repository.settings.collect { snapshot ->
                    persistedTheme = snapshot.settings.theme
                    mutableState.update { current ->
                        current.copy(
                            theme =
                                if (current.savingTheme == null || current.savingTheme == snapshot.settings.theme) {
                                    snapshot.settings.theme
                                } else {
                                    current.theme
                                },
                            problem = if (current.problem == AppSettingsProblem.Write) current.problem else snapshot.problem,
                        )
                    }
                }
            }
        }

        fun selectTheme(theme: AppThemePreference) {
            mutableState.update { it.copy(overlay = null) }
            if (theme == mutableState.value.theme && mutableState.value.savingTheme == null) return
            val generation = ++updateGeneration
            updateJob?.cancel()
            updateJob =
                viewModelScope.launch {
                    retryTheme = null
                    mutableState.update { it.copy(savingTheme = theme, problem = null, canRetry = false) }
                    when (repository.setTheme(theme)) {
                        AppSettingsUpdateResult.Success -> {
                            if (generation != updateGeneration) return@launch
                            mutableState.update { it.copy(theme = persistedTheme, savingTheme = null) }
                        }

                        is AppSettingsUpdateResult.Failure -> {
                            if (generation != updateGeneration) return@launch
                            retryTheme = theme
                            mutableState.update {
                                it.copy(
                                    theme = persistedTheme,
                                    savingTheme = null,
                                    problem = AppSettingsProblem.Write,
                                    canRetry = true,
                                )
                            }
                        }
                    }
                }
        }

        fun retry() {
            retryTheme?.let(::selectTheme)
        }

        fun dismissProblem() {
            retryTheme = null
            mutableState.update { it.copy(problem = null, canRetry = false) }
        }

        fun showThemeSelection() {
            mutableState.update { it.copy(overlay = SettingsOverlay.ThemeSelection) }
        }

        fun showAbout() {
            mutableState.update { it.copy(overlay = SettingsOverlay.About) }
        }

        fun dismissOverlay() {
            mutableState.update { it.copy(overlay = null) }
        }
    }
