package cn.james.music.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme

@Composable
fun MoeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    emphasized: Boolean = true,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = MoeKoeTheme.dimensions.minimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emphasized) {
            Box(
                modifier =
                    Modifier
                        .padding(end = MoeKoeTheme.spacing.compact)
                        .size(width = 4.dp, height = 24.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supportingText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        action?.invoke(this)
    }
}

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
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
fun MoeSongRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    minimumHeight: Dp = 72.dp,
    verticalContentPadding: Dp = MoeKoeTheme.spacing.small,
    metadata: String? = null,
    enabled: Boolean = true,
    isPlaying: Boolean = false,
    titleLeading: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val largeText = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    val titleLines = if (largeText) 2 else 1
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = if (largeText) 96.dp else minimumHeight)
                .clickable(enabled = enabled, onClick = onClick)
                .alpha(if (enabled) 1f else DISABLED_CONTENT_ALPHA)
                .padding(vertical = verticalContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPlaying) {
            Box(
                modifier = Modifier.padding(end = MoeKoeTheme.spacing.small).size(4.dp, 32.dp),
            ) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
            }
        }
        MoeArtwork(content = artwork)
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = MoeKoeTheme.spacing.compact),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                titleLeading()
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = titleLines,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(start = MoeKoeTheme.spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    content = badges,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (largeText) {
                    metadata?.let { text ->
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = MoeKoeTheme.spacing.small),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (!largeText) {
            metadata?.let { text ->
            Text(
                text = text,
                modifier = Modifier.padding(end = MoeKoeTheme.spacing.small),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            }
        }
        trailing()
    }
}

@Composable
fun MoeMiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    positionLabel: String,
    durationLabel: String,
    badgeLabel: String?,
    playContentDescription: String,
    pauseContentDescription: String,
    previousContentDescription: String,
    nextContentDescription: String,
    queueContentDescription: String,
    onTogglePlayback: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    onOpenPlayer: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = MoeKoeTheme.spacing.small, vertical = MoeKoeTheme.spacing.space4)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .then(
                            if (onOpenPlayer == null) {
                                Modifier
                            } else {
                                Modifier.clickable(onClick = onOpenPlayer)
                            },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoeArtwork(size = 48.dp, content = artwork)
                Column(modifier = Modifier.weight(1f).padding(start = MoeKoeTheme.spacing.compact, end = MoeKoeTheme.spacing.small)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        badgeLabel?.let { label ->
                            MoeMediaBadge(
                                text = label,
                                modifier = Modifier.padding(start = MoeKoeTheme.spacing.space4),
                            )
                        }
                    }
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MiniPlayerIconButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = previousContentDescription,
                    onClick = onPrevious,
                )
                IconButton(
                    onClick = onTogglePlayback,
                    modifier =
                        Modifier
                            .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                            .semantics {
                                contentDescription = if (isPlaying) pauseContentDescription else playContentDescription
                            },
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) MoePauseIcon else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard),
                            )
                        }
                    }
                }
                MiniPlayerIconButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = nextContentDescription,
                    onClick = onNext,
                )
                MiniPlayerIconButton(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = queueContentDescription,
                    onClick = onOpenQueue,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniPlayerTime(positionLabel)
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).padding(horizontal = MoeKoeTheme.spacing.small).height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                MiniPlayerTime(durationLabel)
            }
        }
    }
}

@Composable
private fun MiniPlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard),
        )
    }
}

@Composable
private fun MiniPlayerTime(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

@Composable
fun MoeQueueRow(
    positionLabel: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    removeContentDescription: String,
    onRemove: () -> Unit,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    metadata: String? = null,
    isPlaying: Boolean = false,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onClick)
                .padding(vertical = MoeKoeTheme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = positionLabel,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        MoeArtwork(size = 44.dp, content = artwork)
        Column(modifier = Modifier.weight(1f).padding(horizontal = MoeKoeTheme.spacing.compact)) {
            Text(
                text = title,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        metadata?.let { text ->
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = removeContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard),
            )
        }
    }
}

private data class BadgeColors(
    val content: Color,
    val container: Color,
    val border: Color,
)

@Composable
private fun MoeMediaBadgeTone.badgeColors(): BadgeColors =
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

private const val LARGE_TEXT_SCALE = 1.3f
private const val DISABLED_CONTENT_ALPHA = 0.48f

private val MoePauseIcon: ImageVector =
    ImageVector
        .Builder(
            name = "MoePause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 5f)
                horizontalLineTo(10f)
                verticalLineTo(19f)
                horizontalLineTo(6f)
                close()
                moveTo(14f, 5f)
                horizontalLineTo(18f)
                verticalLineTo(19f)
                horizontalLineTo(14f)
                close()
            }
        }.build()
