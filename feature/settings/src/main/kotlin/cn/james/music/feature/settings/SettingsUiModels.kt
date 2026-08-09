package cn.james.music.feature.settings

import androidx.compose.runtime.Immutable

@Immutable
internal enum class SettingsThemeUi {
    System,
    Light,
    Dark,
    Amoled,
}

@Immutable
internal enum class SettingsLyricsTextSizeUi {
    Standard,
    Large,
    Largest,
}

@Immutable
internal enum class SettingsPlaybackQualityUi {
    Standard,
    High,
    Lossless,
    HiRes,
    ViperAtmos,
    ViperClear,
    ViperTape,
}

@Immutable
internal enum class SettingsBrandThemeColorUi {
    SkyBlue,
    SakuraPink,
    StarPurple,
    MintGreen,
    LakeCyan,
    SunsetOrange,
}

@Immutable
internal enum class SettingsProblemUi {
    Read,
    Write,
}

@Immutable
internal sealed interface SettingsOverlay {
    data object ThemeSelection : SettingsOverlay

    data object LyricsTextSizeSelection : SettingsOverlay

    data object PlaybackQualitySelection : SettingsOverlay

    data object BrandThemeColorSelection : SettingsOverlay

    data object ClearCacheConfirmation : SettingsOverlay

    data object About : SettingsOverlay
}

@Immutable
internal enum class SettingsGroupId {
    Appearance,
    PlaybackQuality,
    Lyrics,
    Storage,
    Other,
}

@Immutable
internal enum class SettingsRowId {
    ThemeMode,
    ThemeColor,
    DynamicColor,
    AmoledMode,
    DefaultQuality,
    SkipFailed,
    Fade,
    LyricsDisplay,
    Translation,
    LyricsFontSize,
    CacheLimit,
    ClearCache,
    Language,
    About,
}

@Immutable
internal sealed interface SettingsRowUi {
    val id: SettingsRowId

    data class Action(
        override val id: SettingsRowId,
        val loading: Boolean = false,
    ) : SettingsRowUi

    data class Value(
        override val id: SettingsRowId,
        val value: SettingsThemeUi,
        val loading: Boolean,
    ) : SettingsRowUi

    data class TextValue(
        override val id: SettingsRowId,
        val value: SettingsLyricsTextSizeUi,
        val loading: Boolean,
    ) : SettingsRowUi

    data class PlaybackQualityValue(
        override val id: SettingsRowId,
        val value: SettingsPlaybackQualityUi,
        val loading: Boolean,
    ) : SettingsRowUi

    data class BrandThemeColorValue(
        override val id: SettingsRowId,
        val value: SettingsBrandThemeColorUi,
        val loading: Boolean,
    ) : SettingsRowUi

    data class Unavailable(
        override val id: SettingsRowId,
    ) : SettingsRowUi

    data class Toggle(
        override val id: SettingsRowId,
        val checked: Boolean,
        val loading: Boolean,
    ) : SettingsRowUi
}

@Immutable
internal data class SettingsGroupUi(
    val id: SettingsGroupId,
    val rows: List<SettingsRowUi>,
)

@Immutable
internal data class SettingsUiState(
    val theme: SettingsThemeUi = SettingsThemeUi.System,
    val autoSkipFailedPlayback: Boolean = true,
    val dynamicCoverColors: Boolean = true,
    val showLyricsSupplementalText: Boolean = true,
    val lyricsTextSize: SettingsLyricsTextSizeUi = SettingsLyricsTextSizeUi.Standard,
    val playbackQuality: SettingsPlaybackQualityUi = SettingsPlaybackQualityUi.Standard,
    val brandThemeColor: SettingsBrandThemeColorUi = SettingsBrandThemeColorUi.SkyBlue,
    val savingTheme: SettingsThemeUi? = null,
    val savingAutoSkipFailedPlayback: Boolean? = null,
    val savingDynamicCoverColors: Boolean? = null,
    val savingLyricsSupplementalText: Boolean? = null,
    val savingLyricsTextSize: SettingsLyricsTextSizeUi? = null,
    val savingPlaybackQuality: SettingsPlaybackQualityUi? = null,
    val savingBrandThemeColor: SettingsBrandThemeColorUi? = null,
    val clearingCache: Boolean = false,
    val problem: SettingsProblemUi? = null,
    val cacheClearFeedback: SettingsCacheClearFeedbackUi? = null,
    val canRetry: Boolean = false,
    val overlay: SettingsOverlay? = null,
    val groups: List<SettingsGroupUi> = settingsGroups(SettingsThemeUi.System, autoSkipFailedPlayback = true),
)

@Immutable
internal enum class SettingsCacheClearFeedbackUi {
    Cleared,
    PartiallyCleared,
    Failed,
}

internal sealed interface SettingsAction {
    data object Back : SettingsAction

    data class SelectTheme(
        val theme: SettingsThemeUi,
    ) : SettingsAction

    data object OpenTheme : SettingsAction

    data object OpenAbout : SettingsAction

    data class SetAutoSkipFailedPlayback(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetDynamicCoverColors(
        val enabled: Boolean,
    ) : SettingsAction

    data class SetShowLyricsSupplementalText(
        val enabled: Boolean,
    ) : SettingsAction

    data class SelectLyricsTextSize(
        val size: SettingsLyricsTextSizeUi,
    ) : SettingsAction

    data class SelectPlaybackQuality(
        val quality: SettingsPlaybackQualityUi,
    ) : SettingsAction

    data class SelectBrandThemeColor(
        val color: SettingsBrandThemeColorUi,
    ) : SettingsAction

    data object OpenLyricsTextSize : SettingsAction

    data object OpenPlaybackQuality : SettingsAction

    data object OpenBrandThemeColor : SettingsAction

    data object OpenClearCache : SettingsAction

    data object ConfirmClearCache : SettingsAction

    data object DismissOverlay : SettingsAction

    data object Retry : SettingsAction

    data object DismissCacheClearFeedback : SettingsAction

    data object DismissProblem : SettingsAction
}

internal fun settingsGroups(
    theme: SettingsThemeUi,
    autoSkipFailedPlayback: Boolean,
    dynamicCoverColors: Boolean = true,
    showLyricsSupplementalText: Boolean = true,
    lyricsTextSize: SettingsLyricsTextSizeUi = SettingsLyricsTextSizeUi.Standard,
    playbackQuality: SettingsPlaybackQualityUi = SettingsPlaybackQualityUi.Standard,
    brandThemeColor: SettingsBrandThemeColorUi = SettingsBrandThemeColorUi.SkyBlue,
    savingTheme: SettingsThemeUi? = null,
    savingAutoSkipFailedPlayback: Boolean? = null,
    savingDynamicCoverColors: Boolean? = null,
    savingLyricsSupplementalText: Boolean? = null,
    savingLyricsTextSize: SettingsLyricsTextSizeUi? = null,
    savingPlaybackQuality: SettingsPlaybackQualityUi? = null,
    savingBrandThemeColor: SettingsBrandThemeColorUi? = null,
    clearingCache: Boolean = false,
): List<SettingsGroupUi> =
    listOf(
        SettingsGroupUi(
            id = SettingsGroupId.Appearance,
            rows =
                listOf(
                    SettingsRowUi.Value(SettingsRowId.ThemeMode, theme, savingTheme != null),
                    SettingsRowUi.BrandThemeColorValue(
                        id = SettingsRowId.ThemeColor,
                        value = brandThemeColor,
                        loading = savingBrandThemeColor != null,
                    ),
                    SettingsRowUi.Toggle(
                        id = SettingsRowId.DynamicColor,
                        checked = dynamicCoverColors,
                        loading = savingDynamicCoverColors != null,
                    ),
                    SettingsRowUi.Toggle(SettingsRowId.AmoledMode, theme == SettingsThemeUi.Amoled, savingTheme != null),
                ),
        ),
        SettingsGroupUi(
            id = SettingsGroupId.PlaybackQuality,
            rows =
                listOf(
                    SettingsRowUi.PlaybackQualityValue(
                        id = SettingsRowId.DefaultQuality,
                        value = playbackQuality,
                        loading = savingPlaybackQuality != null,
                    ),
                    SettingsRowUi.Toggle(
                        id = SettingsRowId.SkipFailed,
                        checked = autoSkipFailedPlayback,
                        loading = savingAutoSkipFailedPlayback != null,
                    ),
                    SettingsRowUi.Unavailable(SettingsRowId.Fade),
                ),
        ),
        SettingsGroupUi(
            id = SettingsGroupId.Lyrics,
            rows =
                listOf(
                    SettingsRowUi.Unavailable(SettingsRowId.LyricsDisplay),
                    SettingsRowUi.Toggle(
                        id = SettingsRowId.Translation,
                        checked = showLyricsSupplementalText,
                        loading = savingLyricsSupplementalText != null,
                    ),
                    SettingsRowUi.TextValue(
                        id = SettingsRowId.LyricsFontSize,
                        value = lyricsTextSize,
                        loading = savingLyricsTextSize != null,
                    ),
                ),
        ),
        SettingsGroupUi(
            id = SettingsGroupId.Storage,
            rows =
                listOf(
                    SettingsRowUi.Unavailable(SettingsRowId.CacheLimit),
                    SettingsRowUi.Action(SettingsRowId.ClearCache, loading = clearingCache),
                ),
        ),
        SettingsGroupUi(
            id = SettingsGroupId.Other,
            rows =
                listOf(
                    SettingsRowUi.Unavailable(SettingsRowId.Language),
                    SettingsRowUi.Action(SettingsRowId.About),
                ),
        ),
    )
