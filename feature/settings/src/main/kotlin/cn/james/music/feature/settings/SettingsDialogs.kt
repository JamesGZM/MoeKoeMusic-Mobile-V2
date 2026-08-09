package cn.james.music.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeHorizontalDivider
import cn.james.music.core.designsystem.component.action.MoeTextButton
import cn.james.music.core.designsystem.component.overlay.MoeDialog

@Composable
internal fun ThemeSelectionDialog(
    selected: SettingsThemeUi,
    saving: SettingsThemeUi?,
    onAction: (SettingsAction) -> Unit,
) {
    SettingsSelectionDialog(
        title = stringResource(R.string.settings_choose_theme),
        options = SettingsThemeUi.entries,
        selected = selected,
        saving = saving != null,
        optionLabel = { theme -> theme.displayName() },
        onSelected = { theme -> onAction(SettingsAction.SelectTheme(theme)) },
        onDismiss = { onAction(SettingsAction.DismissOverlay) },
    )
}

@Composable
internal fun LyricsTextSizeSelectionDialog(
    selected: SettingsLyricsTextSizeUi,
    saving: SettingsLyricsTextSizeUi?,
    onAction: (SettingsAction) -> Unit,
) {
    SettingsSelectionDialog(
        title = stringResource(R.string.settings_choose_lyrics_text_size),
        options = SettingsLyricsTextSizeUi.entries,
        selected = selected,
        saving = saving != null,
        optionLabel = { size -> size.displayName() },
        onSelected = { size -> onAction(SettingsAction.SelectLyricsTextSize(size)) },
        onDismiss = { onAction(SettingsAction.DismissOverlay) },
    )
}

@Composable
internal fun PlaybackQualitySelectionDialog(
    selected: SettingsPlaybackQualityUi,
    saving: SettingsPlaybackQualityUi?,
    onAction: (SettingsAction) -> Unit,
) {
    SettingsSelectionDialog(
        title = stringResource(R.string.settings_choose_playback_quality),
        options = SettingsPlaybackQualityUi.entries,
        selected = selected,
        saving = saving != null,
        optionLabel = { quality -> quality.fullDisplayName() },
        onSelected = { quality -> onAction(SettingsAction.SelectPlaybackQuality(quality)) },
        onDismiss = { onAction(SettingsAction.DismissOverlay) },
    )
}

@Composable
private fun <T> SettingsSelectionDialog(
    title: String,
    options: List<T>,
    selected: T,
    saving: Boolean,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    MoeDialog(onDismissRequest = onDismiss) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(Modifier.padding(top = MoeKoeTheme.spacing.space12).selectableGroup()) {
            options.forEach { option ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                enabled = !saving,
                                role = Role.RadioButton,
                                onClick = { onSelected(option) },
                            )
                            .padding(vertical = MoeKoeTheme.spacing.space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == selected, onClick = null, enabled = !saving)
                    Text(optionLabel(option), modifier = Modifier.padding(start = MoeKoeTheme.spacing.space8))
                }
            }
        }
        MoeTextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.settings_cancel))
        }
    }
}

@Composable
internal fun AboutDialog(onAction: (SettingsAction) -> Unit) {
    MoeDialog(onDismissRequest = { onAction(SettingsAction.DismissOverlay) }) {
        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MoeHorizontalDivider(Modifier.padding(vertical = MoeKoeTheme.spacing.space16))
        Text(stringResource(R.string.settings_about_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoeTextButton(
            onClick = { onAction(SettingsAction.DismissOverlay) },
            modifier = Modifier.align(Alignment.End).padding(top = MoeKoeTheme.spacing.space16),
        ) {
            Text(stringResource(R.string.settings_done))
        }
    }
}
