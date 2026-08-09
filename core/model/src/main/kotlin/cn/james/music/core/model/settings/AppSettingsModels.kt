package cn.james.music.core.model.settings

import kotlinx.coroutines.flow.Flow

enum class AppThemePreference {
    System,
    Light,
    Dark,
    Amoled,
}

data class AppSettings(
    val theme: AppThemePreference = AppThemePreference.System,
    val autoSkipFailedPlayback: Boolean = true,
)

sealed interface AppSettingsProblem {
    data object Read : AppSettingsProblem

    data object Write : AppSettingsProblem
}

data class AppSettingsSnapshot(
    val settings: AppSettings = AppSettings(),
    val problem: AppSettingsProblem? = null,
)

sealed interface AppSettingsUpdateResult {
    data object Success : AppSettingsUpdateResult

    data class Failure(
        val problem: AppSettingsProblem,
    ) : AppSettingsUpdateResult
}

interface AppSettingsRepository {
    val settings: Flow<AppSettingsSnapshot>

    suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult

    suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult
}
