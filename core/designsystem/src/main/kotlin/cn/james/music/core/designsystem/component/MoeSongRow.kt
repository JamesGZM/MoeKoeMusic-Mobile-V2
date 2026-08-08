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
import cn.james.music.core.designsystem.MoeKoeTheme

@Composable
fun MoeSongRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    minimumHeight: Dp = 72.dp,
    largeTextMinimumHeight: Dp = 96.dp,
    fixedHeight: Dp? = null,
    artworkSize: Dp = 52.dp,
    artworkShape: Shape = MaterialTheme.shapes.small,
    decorateArtwork: Boolean = true,
    horizontalContentPadding: Dp = 0.dp,
    contentStartPadding: Dp = horizontalContentPadding,
    contentEndPadding: Dp = horizontalContentPadding,
    verticalContentPadding: Dp = MoeKoeTheme.spacing.small,
    textHorizontalPadding: Dp = MoeKoeTheme.spacing.compact,
    textStartPadding: Dp = textHorizontalPadding,
    textEndPadding: Dp = textHorizontalPadding,
    subtitleTopPadding: Dp = 0.dp,
    metadata: String? = null,
    metadataStartPadding: Dp = 0.dp,
    metadataEndPadding: Dp = MoeKoeTheme.spacing.small,
    metadataInSubtitleOnLargeText: Boolean = true,
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
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    metadataStyle: TextStyle = MaterialTheme.typography.bodySmall,
    titleFontWeight: FontWeight = FontWeight.Medium,
    titleMaxLines: Int = 1,
    largeTextTitleMaxLines: Int = 2,
    inlineTitleContent: Boolean = true,
    leading: @Composable RowScope.() -> Unit = {},
    artworkTrailing: @Composable RowScope.() -> Unit = {},
    titleLeading: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {},
    contentTrailing: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val largeText = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    val titleLines = if (largeText) largeTextTitleMaxLines else titleMaxLines
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    fixedHeight?.let(Modifier::height)
                        ?: Modifier.heightIn(min = if (largeText) largeTextMinimumHeight else minimumHeight),
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
                    start = contentStartPadding,
                    end = contentEndPadding,
                    top = verticalContentPadding,
                    bottom = verticalContentPadding,
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
        if (decorateArtwork) {
            MoeArtwork(size = artworkSize, shape = artworkShape, content = artwork)
        } else {
            Box(
                modifier = Modifier.size(artworkSize),
                contentAlignment = Alignment.Center,
                content = artwork,
            )
        }
        artworkTrailing()
        Column(
            modifier = Modifier.weight(1f).padding(start = textStartPadding, end = textEndPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            if (inlineTitleContent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    titleLeading()
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = titleStyle,
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
                    style = titleStyle,
                    fontWeight = titleFontWeight,
                    color = if (isPlaying && highlightTitleWhenPlaying) playingColor else titleColor,
                    maxLines = titleLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (largeText && metadataInSubtitleOnLargeText) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.weight(1f).padding(top = subtitleTopPadding),
                        style = subtitleStyle,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    metadata?.let { text ->
                        Text(
                            text = text,
                            modifier = Modifier.padding(start = MoeKoeTheme.spacing.small),
                            style = metadataStyle,
                            color = metadataColor,
                            maxLines = 1,
                        )
                    }
                }
            } else {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = subtitleTopPadding),
                    style = subtitleStyle,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        contentTrailing()
        if (!largeText || !metadataInSubtitleOnLargeText) {
            metadata?.let { text ->
                Text(
                    text = text,
                    modifier =
                        Modifier.padding(
                            start = metadataStartPadding,
                            end = metadataEndPadding,
                        ),
                    style = metadataStyle,
                    color = metadataColor,
                    maxLines = 1,
                )
            }
        }
        trailing()
    }
}

private const val LARGE_TEXT_SCALE = 1.3f
private const val DISABLED_CONTENT_ALPHA = 0.48f
