package cn.james.music.core.designsystem.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme

enum class MoeSongRowStyle {
    Standard,
    Comfortable,
    Compact,
    Playlist,
    PlaylistCurrent,
    Queue,
    QueueCurrent,
}

@Composable
fun MoeSongRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    style: MoeSongRowStyle = MoeSongRowStyle.Standard,
    metadata: String? = null,
    enabled: Boolean = true,
    isPlaying: Boolean = false,
    showPlayingIndicator: Boolean = true,
    highlightTitleWhenPlaying: Boolean = true,
    shape: Shape = RectangleShape,
    containerColor: Color = Color.Transparent,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    metadataColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    playingColor: Color = MaterialTheme.colorScheme.primary,
    titleFontWeight: FontWeight = FontWeight.SemiBold,
    leading: @Composable RowScope.() -> Unit = {},
    artworkTrailing: @Composable RowScope.() -> Unit = {},
    titleLeading: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {},
    contentTrailing: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val layout = style.layout()
    val largeText = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    val titleLines = if (largeText) layout.largeTextTitleMaxLines else layout.titleMaxLines
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    layout.fixedHeight?.let(Modifier::height)
                        ?: Modifier.heightIn(min = if (largeText) layout.largeTextMinimumHeight else layout.minimumHeight),
                )
                .clip(shape)
                .background(containerColor)
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(enabled = enabled, onClick = onClick)
                    },
                )
                .alpha(if (enabled) 1f else DISABLED_CONTENT_ALPHA)
                .padding(
                    start = layout.contentStartPadding,
                    end = layout.contentEndPadding,
                    top = layout.verticalContentPadding,
                    bottom = layout.verticalContentPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPlaying && showPlayingIndicator) {
            Box(
                modifier = Modifier.padding(end = MoeKoeTheme.spacing.small).size(4.dp, 32.dp),
            ) {
                Box(Modifier.fillMaxSize().background(playingColor, RoundedCornerShape(2.dp)))
            }
        }
        leading()
        if (layout.decorateArtwork) {
            MoeArtwork(size = layout.artworkSize, shape = layout.artworkShape, content = artwork)
        } else {
            Box(
                modifier = Modifier.size(layout.artworkSize),
                contentAlignment = Alignment.Center,
                content = artwork,
            )
        }
        artworkTrailing()
        Column(
            modifier = Modifier.weight(1f).padding(start = layout.textStartPadding, end = layout.textEndPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            if (layout.inlineTitleContent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    titleLeading()
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = layout.titleStyle,
                        fontWeight = titleFontWeight,
                        color = if (isPlaying && highlightTitleWhenPlaying) playingColor else titleColor,
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
            } else {
                Text(
                    text = title,
                    style = layout.titleStyle,
                    fontWeight = titleFontWeight,
                    color = if (isPlaying && highlightTitleWhenPlaying) playingColor else titleColor,
                    maxLines = titleLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (largeText && layout.metadataInSubtitleOnLargeText) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.weight(1f).padding(top = layout.subtitleTopPadding),
                        style = layout.subtitleStyle,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    metadata?.let { text ->
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = MoeKoeTheme.spacing.small),
                            style = layout.metadataStyle,
                            color = metadataColor,
                            maxLines = 1,
                        )
                    }
                }
            } else {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = layout.subtitleTopPadding),
                    style = layout.subtitleStyle,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        contentTrailing()
        if (!largeText || !layout.metadataInSubtitleOnLargeText) {
            metadata?.let { text ->
                Text(
                    text = text,
                    modifier =
                        Modifier.padding(
                            start = layout.metadataStartPadding,
                            end = layout.metadataEndPadding,
                        ),
                    style = layout.metadataStyle,
                    color = metadataColor,
                    maxLines = 1,
                )
            }
        }
        trailing()
    }
}

@Composable
fun MoeSongMoreAction(
    contentDescription: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (onClick == null) {
        Box(modifier = modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(MoeKoeTheme.dimensions.iconSupporting),
            )
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(MoeKoeTheme.dimensions.iconSupporting),
            )
        }
    }
}

private data class SongRowLayout(
    val minimumHeight: Dp,
    val largeTextMinimumHeight: Dp,
    val fixedHeight: Dp?,
    val artworkSize: Dp,
    val artworkShape: Shape,
    val decorateArtwork: Boolean,
    val contentStartPadding: Dp,
    val contentEndPadding: Dp,
    val verticalContentPadding: Dp,
    val textStartPadding: Dp,
    val textEndPadding: Dp,
    val subtitleTopPadding: Dp,
    val metadataStartPadding: Dp,
    val metadataEndPadding: Dp,
    val metadataInSubtitleOnLargeText: Boolean,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val metadataStyle: TextStyle,
    val titleMaxLines: Int,
    val largeTextTitleMaxLines: Int,
    val inlineTitleContent: Boolean,
)

@Composable
private fun MoeSongRowStyle.layout(): SongRowLayout =
    when (this) {
        MoeSongRowStyle.Standard ->
            SongRowLayout(
                minimumHeight = 64.dp,
                largeTextMinimumHeight = 88.dp,
                fixedHeight = null,
                artworkSize = 52.dp,
                artworkShape = RoundedCornerShape(6.dp),
                decorateArtwork = true,
                contentStartPadding = 0.dp,
                contentEndPadding = 0.dp,
                verticalContentPadding = 6.dp,
                textStartPadding = 10.dp,
                textEndPadding = 10.dp,
                subtitleTopPadding = 0.dp,
                metadataStartPadding = 0.dp,
                metadataEndPadding = 8.dp,
                metadataInSubtitleOnLargeText = true,
                titleStyle = MaterialTheme.typography.labelLarge,
                subtitleStyle = MaterialTheme.typography.bodySmall,
                metadataStyle = MaterialTheme.typography.bodySmall,
                titleMaxLines = 1,
                largeTextTitleMaxLines = 2,
                inlineTitleContent = true,
            )
        MoeSongRowStyle.Comfortable ->
            SongRowLayout(
                minimumHeight = 69.dp,
                largeTextMinimumHeight = 96.dp,
                fixedHeight = null,
                artworkSize = 60.dp,
                artworkShape = RoundedCornerShape(6.dp),
                decorateArtwork = true,
                contentStartPadding = 0.dp,
                contentEndPadding = 0.dp,
                verticalContentPadding = 4.5.dp,
                textStartPadding = 14.dp,
                textEndPadding = 12.dp,
                subtitleTopPadding = 0.dp,
                metadataStartPadding = 0.dp,
                metadataEndPadding = 8.dp,
                metadataInSubtitleOnLargeText = true,
                titleStyle = MaterialTheme.typography.labelLarge,
                subtitleStyle = MaterialTheme.typography.bodySmall,
                metadataStyle = MaterialTheme.typography.bodySmall,
                titleMaxLines = 1,
                largeTextTitleMaxLines = 2,
                inlineTitleContent = true,
            )
        MoeSongRowStyle.Compact ->
            SongRowLayout(
                minimumHeight = 48.dp,
                largeTextMinimumHeight = 72.dp,
                fixedHeight = null,
                artworkSize = 48.dp,
                artworkShape = RoundedCornerShape(6.dp),
                decorateArtwork = true,
                contentStartPadding = 0.dp,
                contentEndPadding = 0.dp,
                verticalContentPadding = 0.dp,
                textStartPadding = 10.dp,
                textEndPadding = 10.dp,
                subtitleTopPadding = 0.dp,
                metadataStartPadding = 0.dp,
                metadataEndPadding = 8.dp,
                metadataInSubtitleOnLargeText = true,
                titleStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
                subtitleStyle = MaterialTheme.typography.labelSmall,
                metadataStyle = MaterialTheme.typography.bodySmall,
                titleMaxLines = 1,
                largeTextTitleMaxLines = 1,
                inlineTitleContent = false,
            )
        MoeSongRowStyle.Playlist,
        MoeSongRowStyle.PlaylistCurrent,
        ->
            SongRowLayout(
                minimumHeight = if (this == MoeSongRowStyle.PlaylistCurrent) 58.dp else 57.dp,
                largeTextMinimumHeight = 88.dp,
                fixedHeight = null,
                artworkSize = 44.dp,
                artworkShape = RoundedCornerShape(8.dp),
                decorateArtwork = false,
                contentStartPadding = 4.dp,
                contentEndPadding = 0.dp,
                verticalContentPadding = 0.dp,
                textStartPadding = 12.dp,
                textEndPadding = 12.dp,
                subtitleTopPadding = 2.dp,
                metadataStartPadding = 8.dp,
                metadataEndPadding = 0.dp,
                metadataInSubtitleOnLargeText = false,
                titleStyle = MaterialTheme.typography.titleSmall,
                subtitleStyle = MaterialTheme.typography.bodySmall,
                metadataStyle = MaterialTheme.typography.bodySmall,
                titleMaxLines = 1,
                largeTextTitleMaxLines = 2,
                inlineTitleContent = false,
            )
        MoeSongRowStyle.Queue,
        MoeSongRowStyle.QueueCurrent,
        -> {
            val current = this == MoeSongRowStyle.QueueCurrent
            SongRowLayout(
                minimumHeight = if (current) 58.dp else 56.dp,
                largeTextMinimumHeight = if (current) 58.dp else 56.dp,
                fixedHeight = if (current) 58.dp else 56.dp,
                artworkSize = 44.dp,
                artworkShape = RoundedCornerShape(7.dp),
                decorateArtwork = false,
                contentStartPadding = 9.dp,
                contentEndPadding = 0.dp,
                verticalContentPadding = 0.dp,
                textStartPadding = if (current) 9.dp else 15.dp,
                textEndPadding = 8.dp,
                subtitleTopPadding = 0.dp,
                metadataStartPadding = 0.dp,
                metadataEndPadding = 0.dp,
                metadataInSubtitleOnLargeText = false,
                titleStyle = MaterialTheme.typography.labelLarge.copy(lineHeight = 19.sp),
                subtitleStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                metadataStyle = MaterialTheme.typography.bodySmall,
                titleMaxLines = 1,
                largeTextTitleMaxLines = 1,
                inlineTitleContent = false,
            )
        }
    }

private const val LARGE_TEXT_SCALE = 1.3f
private const val DISABLED_CONTENT_ALPHA = 0.48f
