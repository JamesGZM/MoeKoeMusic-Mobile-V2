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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.james.music.core.model.settings.AppThemePreference

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    onThemeDialogRequest: () -> Unit,
    onAboutDialogRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unavailable = stringResource(R.string.settings_unavailable)
    LazyColumn(
        modifier = modifier.fillMaxSize().widthIn(max = 720.dp),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingsGroup(
                title = stringResource(R.string.settings_appearance),
                modifier = Modifier.settingsLayoutProbe(SETTINGS_PROBE_APPEARANCE),
            ) {
                SettingsItem(
                    icon = Icons.Default.Colorize,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = stringResource(R.string.settings_theme_mode),
                    value = state.theme.displayName(),
                    loading = state.savingTheme != null,
                    onClick = onThemeDialogRequest,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Palette,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_theme_color),
                    value = unavailable,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.AutoAwesome,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(R.string.settings_dynamic_color),
                    value = unavailable,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.settings_amoled_mode),
                    checked = state.theme == AppThemePreference.Amoled,
                    onClick = onThemeDialogRequest,
                )
            }
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.settings_playback_quality),
                modifier = Modifier.settingsLayoutProbe(SETTINGS_PROBE_PLAYBACK),
            ) {
                SettingsItem(
                    icon = Icons.Default.GraphicEq,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_default_quality),
                    value = unavailable,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.SkipNext,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(R.string.settings_skip_failed),
                    value = unavailable,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Waves,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = stringResource(R.string.settings_fade),
                    value = unavailable,
                )
            }
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.settings_lyrics),
                modifier = Modifier.settingsLayoutProbe(SETTINGS_PROBE_LYRICS),
            ) {
                SettingsItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_lyrics_display),
                    value = unavailable,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Translate,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(R.string.settings_translation),
                    value = unavailable,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = stringResource(R.string.settings_lyrics_font_size),
                    value = unavailable,
                )
            }
        }
        item {
            SettingsGroup(
                title = stringResource(R.string.settings_storage),
                modifier = Modifier.settingsLayoutProbe(SETTINGS_PROBE_STORAGE),
            ) {
                SettingsItem(
                    icon = Icons.Default.FolderOpen,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_cache_limit),
                    value = unavailable,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.DeleteOutline,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(R.string.settings_clear_cache),
                    value = unavailable,
                )
            }
        }
        item {
            SettingsGroup(title = stringResource(R.string.settings_other)) {
                SettingsItem(
                    icon = Icons.Default.Language,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = stringResource(R.string.settings_language),
                    value = unavailable,
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_about),
                    onClick = onAboutDialogRequest,
                )
            }
        }
    }
}
