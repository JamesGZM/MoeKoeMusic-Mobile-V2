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
import cn.james.music.core.model.settings.LyricsTextSizePreference
import cn.james.music.core.model.settings.PlaybackQualityPreference
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

        override suspend fun setPlaybackQuality(quality: PlaybackQualityPreference): AppSettingsUpdateResult =
            update { preferences -> preferences[PLAYBACK_QUALITY] = quality.storageValue }

        override suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult =
            update { preferences -> preferences[AUTO_SKIP_FAILED_PLAYBACK] = enabled }

        override suspend fun setDynamicCoverColors(enabled: Boolean): AppSettingsUpdateResult =
            update { preferences -> preferences[DYNAMIC_COVER_COLORS] = enabled }

        override suspend fun setShowLyricsSupplementalText(enabled: Boolean): AppSettingsUpdateResult =
            update { preferences -> preferences[SHOW_LYRICS_SUPPLEMENTAL_TEXT] = enabled }

        override suspend fun setLyricsTextSize(size: LyricsTextSizePreference): AppSettingsUpdateResult =
            update { preferences -> preferences[LYRICS_TEXT_SIZE] = size.storageValue }

        private fun snapshot(preferences: Preferences): AppSettingsSnapshot {
            val storedTheme = preferences[THEME]
            val theme = storedTheme?.let { stored -> AppThemePreference.entries.firstOrNull { it.storageValue == stored } }
            val storedPlaybackQuality = preferences[PLAYBACK_QUALITY]
            val playbackQuality =
                storedPlaybackQuality?.let { stored ->
                    playbackQualityFromStorageValue(stored)
                }
            val storedLyricsTextSize = preferences[LYRICS_TEXT_SIZE]
            val lyricsTextSize =
                storedLyricsTextSize?.let { stored ->
                    LyricsTextSizePreference.entries.firstOrNull { it.storageValue == stored }
                }
            return AppSettingsSnapshot(
                settings =
                    AppSettings(
                        theme = theme ?: AppThemePreference.System,
                        playbackQuality = playbackQuality ?: PlaybackQualityPreference.Standard,
                        autoSkipFailedPlayback = preferences[AUTO_SKIP_FAILED_PLAYBACK] ?: true,
                        dynamicCoverColors = preferences[DYNAMIC_COVER_COLORS] ?: true,
                        showLyricsSupplementalText = preferences[SHOW_LYRICS_SUPPLEMENTAL_TEXT] ?: true,
                        lyricsTextSize = lyricsTextSize ?: LyricsTextSizePreference.Standard,
                    ),
                problem =
                    if (
                        (storedTheme != null && theme == null) ||
                        (storedPlaybackQuality != null && playbackQuality == null) ||
                        (storedLyricsTextSize != null && lyricsTextSize == null)
                    ) {
                        AppSettingsProblem.Read
                    } else {
                        null
                    },
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

        private val LyricsTextSizePreference.storageValue: String
            get() = name.lowercase()

        private val PlaybackQualityPreference.storageValue: String
            get() =
                when (this) {
                    PlaybackQualityPreference.Standard -> "128"
                    PlaybackQualityPreference.High -> "320"
                    PlaybackQualityPreference.Lossless -> "flac"
                    PlaybackQualityPreference.HiRes -> "high"
                    PlaybackQualityPreference.ViperAtmos -> "viper_atmos"
                    PlaybackQualityPreference.ViperClear -> "viper_clear"
                    PlaybackQualityPreference.ViperTape -> "viper_tape"
                }

        private fun playbackQualityFromStorageValue(value: String): PlaybackQualityPreference? =
            when (value) {
                "128" -> PlaybackQualityPreference.Standard
                "320" -> PlaybackQualityPreference.High
                "flac" -> PlaybackQualityPreference.Lossless
                "high" -> PlaybackQualityPreference.HiRes
                "viper_atmos" -> PlaybackQualityPreference.ViperAtmos
                "viper_clear" -> PlaybackQualityPreference.ViperClear
                "viper_tape" -> PlaybackQualityPreference.ViperTape
                else -> null
            }

        internal companion object {
            val THEME = stringPreferencesKey("theme_mode_v1")
            val PLAYBACK_QUALITY = stringPreferencesKey("playback_quality_v1")
            val AUTO_SKIP_FAILED_PLAYBACK = booleanPreferencesKey("auto_skip_failed_playback_v1")
            val DYNAMIC_COVER_COLORS = booleanPreferencesKey("dynamic_cover_colors_v1")
            val SHOW_LYRICS_SUPPLEMENTAL_TEXT = booleanPreferencesKey("show_lyrics_supplemental_text_v1")
            val LYRICS_TEXT_SIZE = stringPreferencesKey("lyrics_text_size_v1")
        }
    }
