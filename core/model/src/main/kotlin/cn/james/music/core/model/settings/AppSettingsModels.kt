package cn.james.music.core.model.settings

import kotlinx.coroutines.flow.Flow

enum class AppThemePreference {
    System,
    Light,
    Dark,
    Amoled,
}

enum class BrandThemeColorPreference {
    SkyBlue,
    SakuraPink,
    StarPurple,
    MintGreen,
    LakeCyan,
    SunsetOrange,
}

enum class PlaybackQualityPreference {
    Standard,
    High,
    Lossless,
    HiRes,
    ViperAtmos,
    ViperClear,
    ViperTape,
}

enum class LyricsTextSizePreference {
    Standard,
    Large,
    Largest,
}

data class AppSettings(
    val theme: AppThemePreference = AppThemePreference.System,
    val playbackQuality: PlaybackQualityPreference = PlaybackQualityPreference.Standard,
    val autoSkipFailedPlayback: Boolean = true,
    val dynamicCoverColors: Boolean = true,
    val showLyricsSupplementalText: Boolean = true,
    val lyricsTextSize: LyricsTextSizePreference = LyricsTextSizePreference.Standard,
    val brandThemeColor: BrandThemeColorPreference = BrandThemeColorPreference.SkyBlue,
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

    suspend fun setBrandThemeColor(color: BrandThemeColorPreference): AppSettingsUpdateResult

    suspend fun setPlaybackQuality(quality: PlaybackQualityPreference): AppSettingsUpdateResult

    suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult

    suspend fun setDynamicCoverColors(enabled: Boolean): AppSettingsUpdateResult

    suspend fun setShowLyricsSupplementalText(enabled: Boolean): AppSettingsUpdateResult

    suspend fun setLyricsTextSize(size: LyricsTextSizePreference): AppSettingsUpdateResult
}
