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
import cn.james.music.core.model.settings.AppThemePreference

@Composable
internal fun ThemeSelectionDialog(
    selected: AppThemePreference,
    saving: AppThemePreference?,
    onSelect: (AppThemePreference) -> Unit,
    onDismiss: () -> Unit,
) {
    MoeDialog(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.settings_choose_theme),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Column(Modifier.padding(top = MoeKoeTheme.spacing.space12).selectableGroup()) {
            AppThemePreference.entries.forEach { theme ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = theme == selected,
                                enabled = saving == null,
                                role = Role.RadioButton,
                                onClick = { onSelect(theme) },
                            )
                            .padding(vertical = MoeKoeTheme.spacing.space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = theme == selected, onClick = null, enabled = saving == null)
                    Text(theme.displayName(), modifier = Modifier.padding(start = MoeKoeTheme.spacing.space8))
                }
            }
        }
        MoeTextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.settings_cancel))
        }
    }
}

@Composable
internal fun AboutDialog(onDismiss: () -> Unit) {
    MoeDialog(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MoeHorizontalDivider(Modifier.padding(vertical = MoeKoeTheme.spacing.space16))
        Text(stringResource(R.string.settings_about_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoeTextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End).padding(top = MoeKoeTheme.spacing.space16),
        ) {
            Text(stringResource(R.string.settings_done))
        }
    }
}
