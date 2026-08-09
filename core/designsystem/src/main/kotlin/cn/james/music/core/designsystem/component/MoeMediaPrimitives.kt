package cn.james.music.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme

@Composable
fun MoeArtwork(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

enum class MoeMediaBadgeTone {
    Primary,
    Error,
    Vip,
    Success,
    Neutral,
}

@Composable
fun MoeMediaBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: MoeMediaBadgeTone = MoeMediaBadgeTone.Primary,
) {
    val colors = tone.badgeColors()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

internal data class BadgeColors(
    val content: Color,
    val container: Color,
    val border: Color,
)

@Composable
internal fun MoeMediaBadgeTone.badgeColors(): BadgeColors =
    when (this) {
        MoeMediaBadgeTone.Primary ->
            BadgeColors(
                content = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                border = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
            )
        MoeMediaBadgeTone.Error ->
            BadgeColors(
                content = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
                border = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
            )
        MoeMediaBadgeTone.Vip ->
            BadgeColors(
                content = MoeKoeTheme.extraColors.warning,
                container = MoeKoeTheme.extraColors.warningContainer.copy(alpha = 0.42f),
                border = MoeKoeTheme.extraColors.warning.copy(alpha = 0.72f),
            )
        MoeMediaBadgeTone.Success ->
            BadgeColors(
                content = MoeKoeTheme.extraColors.success,
                container = MoeKoeTheme.extraColors.successContainer.copy(alpha = 0.42f),
                border = MoeKoeTheme.extraColors.success.copy(alpha = 0.72f),
            )
        MoeMediaBadgeTone.Neutral ->
            BadgeColors(
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                container = MaterialTheme.colorScheme.surfaceContainer,
                border = MaterialTheme.colorScheme.outlineVariant,
            )
    }
