package cn.james.music.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsContent(
    groups: List<SettingsGroupUi>,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groups.forEach { group ->
            item(key = group.id) {
                SettingsGroup(
                    title = group.id.title(),
                    modifier = group.id.probeName()?.let { Modifier.settingsLayoutProbe(it) } ?: Modifier,
                ) {
                    group.rows.forEachIndexed { index, row ->
                        SettingsRow(row = row, onAction = onAction)
                        if (index != group.rows.lastIndex) SettingsDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    row: SettingsRowUi,
    onAction: (SettingsAction) -> Unit,
) {
    val visual = row.id.visual()
    when (row) {
        is SettingsRowUi.Action ->
            SettingsActionItem(
                icon = visual.icon,
                iconColor = visual.color,
                title = stringResource(visual.titleRes),
                onClick = { onAction(SettingsAction.OpenAbout) },
            )
        is SettingsRowUi.Value ->
            SettingsActionItem(
                icon = visual.icon,
                iconColor = visual.color,
                title = stringResource(visual.titleRes),
                value = row.value.displayName(),
                loading = row.loading,
                onClick = { onAction(SettingsAction.OpenTheme) },
            )
        is SettingsRowUi.Unavailable ->
            SettingsUnavailableItem(
                icon = visual.icon,
                iconColor = visual.color,
                title = stringResource(visual.titleRes),
                value = stringResource(R.string.settings_unavailable),
            )
        is SettingsRowUi.Toggle ->
            SettingsToggleItem(
                icon = visual.icon,
                iconColor = visual.color,
                title = stringResource(visual.titleRes),
                checked = row.checked,
                loading = row.loading,
                onCheckedChange = { enabled ->
                    when (row.id) {
                        SettingsRowId.AmoledMode -> onAction(SettingsAction.OpenTheme)
                        SettingsRowId.SkipFailed -> onAction(SettingsAction.SetAutoSkipFailedPlayback(enabled))
                        else -> Unit
                    }
                },
            )
    }
}

private data class SettingsRowVisual(
    val icon: ImageVector,
    val color: Color,
    val titleRes: Int,
)

@Composable
private fun SettingsRowId.visual(): SettingsRowVisual =
    when (this) {
        SettingsRowId.ThemeMode -> SettingsRowVisual(Icons.Default.Colorize, MaterialTheme.colorScheme.secondary, R.string.settings_theme_mode)
        SettingsRowId.ThemeColor -> SettingsRowVisual(Icons.Default.Palette, MaterialTheme.colorScheme.primary, R.string.settings_theme_color)
        SettingsRowId.DynamicColor -> SettingsRowVisual(Icons.Default.AutoAwesome, MaterialTheme.colorScheme.tertiary, R.string.settings_dynamic_color)
        SettingsRowId.AmoledMode -> SettingsRowVisual(Icons.Default.DarkMode, MaterialTheme.colorScheme.onSurfaceVariant, R.string.settings_amoled_mode)
        SettingsRowId.DefaultQuality -> SettingsRowVisual(Icons.Default.GraphicEq, MaterialTheme.colorScheme.primary, R.string.settings_default_quality)
        SettingsRowId.SkipFailed -> SettingsRowVisual(Icons.Default.SkipNext, MaterialTheme.colorScheme.tertiary, R.string.settings_skip_failed)
        SettingsRowId.Fade -> SettingsRowVisual(Icons.Default.Waves, MaterialTheme.colorScheme.secondary, R.string.settings_fade)
        SettingsRowId.LyricsDisplay -> SettingsRowVisual(Icons.Default.ChatBubbleOutline, MaterialTheme.colorScheme.primary, R.string.settings_lyrics_display)
        SettingsRowId.Translation -> SettingsRowVisual(Icons.Default.Translate, MaterialTheme.colorScheme.tertiary, R.string.settings_translation)
        SettingsRowId.LyricsFontSize -> SettingsRowVisual(Icons.Default.TextFields, MaterialTheme.colorScheme.secondary, R.string.settings_lyrics_font_size)
        SettingsRowId.CacheLimit -> SettingsRowVisual(Icons.Default.FolderOpen, MaterialTheme.colorScheme.primary, R.string.settings_cache_limit)
        SettingsRowId.ClearCache -> SettingsRowVisual(Icons.Default.DeleteOutline, MaterialTheme.colorScheme.tertiary, R.string.settings_clear_cache)
        SettingsRowId.Language -> SettingsRowVisual(Icons.Default.Language, MaterialTheme.colorScheme.secondary, R.string.settings_language)
        SettingsRowId.About -> SettingsRowVisual(Icons.Default.Info, MaterialTheme.colorScheme.primary, R.string.settings_about)
    }

@Composable
private fun SettingsGroupId.title(): String =
    stringResource(
        when (this) {
            SettingsGroupId.Appearance -> R.string.settings_appearance
            SettingsGroupId.PlaybackQuality -> R.string.settings_playback_quality
            SettingsGroupId.Lyrics -> R.string.settings_lyrics
            SettingsGroupId.Storage -> R.string.settings_storage
            SettingsGroupId.Other -> R.string.settings_other
        },
    )

private fun SettingsGroupId.probeName(): String? =
    when (this) {
        SettingsGroupId.Appearance -> SETTINGS_PROBE_APPEARANCE
        SettingsGroupId.PlaybackQuality -> SETTINGS_PROBE_PLAYBACK
        SettingsGroupId.Lyrics -> SETTINGS_PROBE_LYRICS
        SettingsGroupId.Storage -> SETTINGS_PROBE_STORAGE
        SettingsGroupId.Other -> null
    }

@Composable
internal fun SettingsThemeUi.displayName(): String =
    stringResource(
        when (this) {
            SettingsThemeUi.System -> R.string.settings_theme_system
            SettingsThemeUi.Light -> R.string.settings_theme_light
            SettingsThemeUi.Dark -> R.string.settings_theme_dark
            SettingsThemeUi.Amoled -> R.string.settings_theme_amoled
        },
    )
