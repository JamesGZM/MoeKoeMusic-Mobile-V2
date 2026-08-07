package cn.james.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppThemeViewModel
    @Inject
    constructor(
        private val repository: AppSettingsRepository,
    ) : ViewModel() {
        val themeMode: StateFlow<ThemeMode> =
            repository.settings
                .map { snapshot -> snapshot.settings.theme.toThemeMode() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ThemeMode.System,
                )

        private var updateJob: Job? = null

        fun updateTheme(themeMode: ThemeMode) {
            updateJob?.cancel()
            updateJob = viewModelScope.launch { repository.setTheme(themeMode.toPreference()) }
        }

        private fun AppThemePreference.toThemeMode(): ThemeMode =
            when (this) {
                AppThemePreference.System -> ThemeMode.System
                AppThemePreference.Light -> ThemeMode.Light
                AppThemePreference.Dark -> ThemeMode.Dark
                AppThemePreference.Amoled -> ThemeMode.Amoled
            }

        private fun ThemeMode.toPreference(): AppThemePreference =
            when (this) {
                ThemeMode.System -> AppThemePreference.System
                ThemeMode.Light -> AppThemePreference.Light
                ThemeMode.Dark -> AppThemePreference.Dark
                ThemeMode.Amoled -> AppThemePreference.Amoled
            }
    }
