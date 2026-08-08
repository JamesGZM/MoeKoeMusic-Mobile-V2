package cn.james.music.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme

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
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = MoeKoeTheme.spacing.small,
                    top = 2.dp,
                    end = MoeKoeTheme.spacing.space4,
                    bottom = 2.dp,
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MoeKoeTheme.dimensions.minimumTouchTarget)
                        .then(
                            if (onOpenPlayer == null) {
                                Modifier
                            } else {
                                Modifier.clickable(onClick = onOpenPlayer)
                            },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoeArtwork(size = 44.dp, content = artwork)
                Column(modifier = Modifier.weight(1f).padding(start = MoeKoeTheme.spacing.small, end = MoeKoeTheme.spacing.space4)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, lineHeight = 14.sp),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        badgeLabel?.let { label ->
                            MiniPlayerBadge(
                                text = label,
                                modifier = Modifier.padding(start = MoeKoeTheme.spacing.space4),
                            )
                        }
                    }
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-8).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniPlayerIconButton(
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = previousContentDescription,
                        onClick = onPrevious,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                                .clickable(
                                    role = Role.Button,
                                    onClick = onTogglePlayback,
                                ).semantics {
                                    contentDescription = if (isPlaying) pauseContentDescription else playContentDescription
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) MoePauseIcon else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(MoeKoeTheme.dimensions.iconSupporting),
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
private fun MiniPlayerBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = MoeMediaBadgeTone.Primary.badgeColors()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = MoeKoeTheme.spacing.space4),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 12.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun MiniPlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(MoeKoeTheme.dimensions.iconSupporting),
        )
    }
}

@Composable
private fun MiniPlayerTime(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

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
