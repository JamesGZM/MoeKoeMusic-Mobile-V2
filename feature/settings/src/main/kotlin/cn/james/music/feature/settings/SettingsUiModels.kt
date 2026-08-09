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
internal enum class SettingsProblemUi {
    Read,
    Write,
}

@Immutable
internal sealed interface SettingsOverlay {
    data object ThemeSelection : SettingsOverlay

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
    ) : SettingsRowUi

    data class Value(
        override val id: SettingsRowId,
        val value: SettingsThemeUi,
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
    val savingTheme: SettingsThemeUi? = null,
    val savingAutoSkipFailedPlayback: Boolean? = null,
    val problem: SettingsProblemUi? = null,
    val canRetry: Boolean = false,
    val overlay: SettingsOverlay? = null,
    val groups: List<SettingsGroupUi> = settingsGroups(SettingsThemeUi.System, autoSkipFailedPlayback = true),
)

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

    data object DismissOverlay : SettingsAction

    data object Retry : SettingsAction

    data object DismissProblem : SettingsAction
}

internal fun settingsGroups(
    theme: SettingsThemeUi,
    autoSkipFailedPlayback: Boolean,
    savingTheme: SettingsThemeUi? = null,
    savingAutoSkipFailedPlayback: Boolean? = null,
): List<SettingsGroupUi> =
    listOf(
        SettingsGroupUi(
            id = SettingsGroupId.Appearance,
            rows =
                listOf(
                    SettingsRowUi.Value(SettingsRowId.ThemeMode, theme, savingTheme != null),
                    SettingsRowUi.Unavailable(SettingsRowId.ThemeColor),
                    SettingsRowUi.Unavailable(SettingsRowId.DynamicColor),
                    SettingsRowUi.Toggle(SettingsRowId.AmoledMode, theme == SettingsThemeUi.Amoled, savingTheme != null),
                ),
        ),
        SettingsGroupUi(
            id = SettingsGroupId.PlaybackQuality,
            rows =
                listOf(
                    SettingsRowUi.Unavailable(SettingsRowId.DefaultQuality),
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
                    SettingsRowUi.Unavailable(SettingsRowId.Translation),
                    SettingsRowUi.Unavailable(SettingsRowId.LyricsFontSize),
                ),
        ),
        SettingsGroupUi(
            id = SettingsGroupId.Storage,
            rows =
                listOf(
                    SettingsRowUi.Unavailable(SettingsRowId.CacheLimit),
                    SettingsRowUi.Unavailable(SettingsRowId.ClearCache),
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
