package cn.james.music.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info as OutlinedInfo
import androidx.compose.material.icons.outlined.Warning as OutlinedWarning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme

@Immutable
private data class MoeSnackbarColors(
    val accent: Color,
    val container: Color,
    val content: Color,
)

@Composable
fun MoeSnackbar(
    model: MoeSnackbarUiModel,
    onEvent: (MoeSnackbarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = model.tone.colors()
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(colors.accent.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = model.icon.imageVector(model.tone),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = model.message,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            model.action?.let { action ->
                TextButton(onClick = { onEvent(MoeSnackbarEvent.Action(action.id)) }) {
                    Text(action.label, color = colors.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun MoeSnackbarIcon.imageVector(tone: MoeSnackbarTone): ImageVector =
    when (this) {
        MoeSnackbarIcon.ToneDefault ->
            when (tone) {
                MoeSnackbarTone.Success -> Icons.Default.Check
                MoeSnackbarTone.Info -> Icons.Default.Info
                MoeSnackbarTone.Error -> Icons.Default.Warning
                MoeSnackbarTone.Warning -> Icons.Default.Info
            }
        MoeSnackbarIcon.Confirmation -> Icons.Outlined.CheckCircle
        MoeSnackbarIcon.Removal -> Icons.Outlined.Delete
        MoeSnackbarIcon.Warning -> Icons.Outlined.OutlinedWarning
        MoeSnackbarIcon.Information -> Icons.Outlined.OutlinedInfo
    }

@Composable
private fun MoeSnackbarTone.colors(): MoeSnackbarColors =
    when (this) {
        MoeSnackbarTone.Success ->
            MoeSnackbarColors(
                accent = MoeKoeTheme.extraColors.success,
                container = MoeKoeTheme.extraColors.successContainer,
                content = MoeKoeTheme.extraColors.onSuccessContainer,
            )
        MoeSnackbarTone.Info ->
            MoeSnackbarColors(
                accent = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        MoeSnackbarTone.Error ->
            MoeSnackbarColors(
                accent = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        MoeSnackbarTone.Warning ->
            MoeSnackbarColors(
                accent = MoeKoeTheme.extraColors.warning,
                container = MoeKoeTheme.extraColors.warningContainer,
                content = MoeKoeTheme.extraColors.onWarningContainer,
            )
    }
