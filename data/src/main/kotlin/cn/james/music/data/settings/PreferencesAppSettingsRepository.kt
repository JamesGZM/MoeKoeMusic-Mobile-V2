package cn.james.music.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cn.james.music.core.model.settings.AppSettings
import cn.james.music.core.model.settings.AppSettingsProblem
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsSnapshot
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesAppSettingsRepository
    @Inject
    constructor(
        @param:AppSettingsDataStore private val dataStore: DataStore<Preferences>,
    ) : AppSettingsRepository {
        override val settings: Flow<AppSettingsSnapshot> =
            dataStore.data
                .map(::snapshot)
                .catch { error ->
                    when (error) {
                        is IOException, is CorruptionException -> emit(AppSettingsSnapshot(problem = AppSettingsProblem.Read))
                        else -> throw error
                    }
                }

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult =
            update { preferences -> preferences[THEME] = theme.storageValue }

        override suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult =
            update { preferences -> preferences[AUTO_SKIP_FAILED_PLAYBACK] = enabled }

        private fun snapshot(preferences: Preferences): AppSettingsSnapshot {
            val storedTheme = preferences[THEME]
            val theme = storedTheme?.let { stored -> AppThemePreference.entries.firstOrNull { it.storageValue == stored } }
            if (storedTheme != null && theme == null) return AppSettingsSnapshot(problem = AppSettingsProblem.Read)
            return AppSettingsSnapshot(
                settings =
                    AppSettings(
                        theme = theme ?: AppThemePreference.System,
                        autoSkipFailedPlayback = preferences[AUTO_SKIP_FAILED_PLAYBACK] ?: true,
                    ),
            )
        }

        private suspend fun update(block: suspend (MutablePreferences) -> Unit): AppSettingsUpdateResult =
            try {
                dataStore.edit(block)
                AppSettingsUpdateResult.Success
            } catch (_: IOException) {
                AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            } catch (_: CorruptionException) {
                AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            }

        private val AppThemePreference.storageValue: String
            get() = name.lowercase()

        internal companion object {
            val THEME = stringPreferencesKey("theme_mode_v1")
            val AUTO_SKIP_FAILED_PLAYBACK = booleanPreferencesKey("auto_skip_failed_playback_v1")
        }
    }
