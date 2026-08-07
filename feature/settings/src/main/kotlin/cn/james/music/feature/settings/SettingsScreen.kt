package cn.james.music.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarTone
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.overlay.MoeDialog
import cn.james.music.core.model.settings.AppSettingsProblem
import cn.james.music.core.model.settings.AppThemePreference

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onThemeSelected: (AppThemePreference) -> Unit,
    onRetry: () -> Unit,
    onDismissProblem: () -> Unit,
) {
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            MoeStandardTopBar(
                title = stringResource(R.string.settings_title),
                navigationContentDescription = stringResource(R.string.settings_back),
                onNavigateBack = onBack,
            )
        },
        snackbarHost = {
            state.problem?.let { problem ->
                MoeSnackbar(
                    message =
                        stringResource(
                            when (problem) {
                                AppSettingsProblem.Read -> R.string.settings_read_failed
                                AppSettingsProblem.Write -> R.string.settings_write_failed
                            },
                        ),
                    modifier = Modifier.padding(MoeKoeTheme.spacing.space16),
                    tone = MoeSnackbarTone.Error,
                    actionLabel = if (state.canRetry) stringResource(R.string.settings_retry) else stringResource(R.string.settings_dismiss),
                    onAction = if (state.canRetry) onRetry else onDismissProblem,
                )
            }
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
                contentPadding = PaddingValues(MoeKoeTheme.spacing.space16),
                verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space16),
            ) {
                item {
                    SettingsGroup(title = stringResource(R.string.settings_appearance)) {
                        SettingsItem(
                            icon = Icons.Default.Brightness4,
                            iconColor = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.settings_theme_mode),
                            value = state.theme.displayName(),
                            loading = state.savingTheme != null,
                            onClick = { showThemeDialog = true },
                        )
                    }
                }
                item {
                    SettingsGroup(title = stringResource(R.string.settings_other)) {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            title = stringResource(R.string.settings_about),
                            value = null,
                            loading = false,
                            onClick = { showAboutDialog = true },
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            selected = state.theme,
            saving = state.savingTheme,
            onSelect = {
                onThemeSelected(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String?,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .clickable(enabled = !loading, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(iconColor.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(MoeKoeTheme.spacing.space16))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            value?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(start = MoeKoeTheme.spacing.space8),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MoeKoeTheme.spacing.space8),
            )
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
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
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.settings_cancel))
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    MoeDialog(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(Modifier.padding(vertical = MoeKoeTheme.spacing.space16))
        Text(stringResource(R.string.settings_about_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = MoeKoeTheme.spacing.space16)) {
            Text(stringResource(R.string.settings_done))
        }
    }
}

@Composable
private fun AppThemePreference.displayName(): String =
    stringResource(
        when (this) {
            AppThemePreference.System -> R.string.settings_theme_system
            AppThemePreference.Light -> R.string.settings_theme_light
            AppThemePreference.Dark -> R.string.settings_theme_dark
            AppThemePreference.Amoled -> R.string.settings_theme_amoled
        },
    )
