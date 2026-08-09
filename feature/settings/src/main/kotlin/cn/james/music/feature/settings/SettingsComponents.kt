package cn.james.music.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeHorizontalDivider
import cn.james.music.core.designsystem.component.MoePassiveOutline
import cn.james.music.core.model.settings.AppThemePreference

@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = MoePassiveOutline(),
    ) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
internal fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String? = null,
    loading: Boolean = false,
    checked: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val interactionModifier =
        if (onClick != null) {
            Modifier.clickable(enabled = !loading, onClick = onClick)
        } else {
            Modifier.semantics { disabled() }
        }
    val enabled = onClick != null
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(interactionModifier)
                .padding(horizontal = 14.dp, vertical = if (checked == null) 6.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp).background(iconColor.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else if (checked != null) {
            Switch(
                checked = checked,
                onCheckedChange = if (onClick == null) null else { _ -> onClick() },
                enabled = enabled,
                modifier = Modifier.graphicsLayer(scaleX = 0.7f, scaleY = 0.7f),
            )
        } else {
            value?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(start = MoeKoeTheme.spacing.space8).alpha(if (enabled) 1f else 0.72f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = MoeKoeTheme.spacing.space8),
                )
            }
        }
    }
}

@Composable
internal fun SettingsDivider() {
    MoeHorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
}

@Composable
internal fun AppThemePreference.displayName(): String =
    stringResource(
        when (this) {
            AppThemePreference.System -> R.string.settings_theme_system
            AppThemePreference.Light -> R.string.settings_theme_light
            AppThemePreference.Dark -> R.string.settings_theme_dark
            AppThemePreference.Amoled -> R.string.settings_theme_amoled
        },
    )
