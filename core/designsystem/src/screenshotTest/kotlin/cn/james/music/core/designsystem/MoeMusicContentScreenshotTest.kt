package cn.james.music.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoeMediaBadge
import cn.james.music.core.designsystem.component.MoeMediaBadgeTone
import cn.james.music.core.designsystem.component.MoeMiniPlayer
import cn.james.music.core.designsystem.component.MoeSectionHeader
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.designsystem.component.MoeSongRowStyle
import cn.james.music.core.designsystem.test.R as ScreenshotTestR
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "MusicContentLight", widthDp = 390, heightDp = 720)
@Composable
fun MoeMusicContentLightScreenshot() {
    MusicContentPreview(themeMode = ThemeMode.Light)
}

@PreviewTest
@Preview(name = "MusicContentDark", widthDp = 390, heightDp = 720)
@Composable
fun MoeMusicContentDarkScreenshot() {
    MusicContentPreview(themeMode = ThemeMode.Dark)
}

@PreviewTest
@Preview(name = "MusicContentLargeText", widthDp = 390, heightDp = 760, fontScale = 1.5f)
@Composable
fun MoeMusicContentLargeTextScreenshot() {
    MusicContentPreview(themeMode = ThemeMode.Light)
}

@PreviewTest
@Preview(name = "SongRowContract", widthDp = 360, heightDp = 64)
@Composable
fun MoeSongRowContractScreenshot() {
    SongRowContractPreview(showProbe = false)
}

@PreviewTest
@Preview(name = "SongRowContractLayoutProbe", widthDp = 360, heightDp = 64)
@Composable
fun MoeSongRowContractLayoutProbeScreenshot() {
    SongRowContractPreview(showProbe = true)
}

@PreviewTest
@Preview(name = "MiniPlayerLight", widthDp = 390, heightDp = 72)
@Composable
fun MoeMiniPlayerLightScreenshot() {
    MiniPlayerPreview(themeMode = ThemeMode.Light)
}

@PreviewTest
@Preview(name = "MiniPlayerDark", widthDp = 390, heightDp = 72)
@Composable
fun MoeMiniPlayerDarkScreenshot() {
    MiniPlayerPreview(themeMode = ThemeMode.Dark)
}

@PreviewTest
@Preview(name = "MiniPlayerLargeText", widthDp = 390, heightDp = 104, fontScale = 1.5f)
@Composable
fun MoeMiniPlayerLargeTextScreenshot() {
    MiniPlayerPreview(themeMode = ThemeMode.Light)
}

@Composable
private fun MusicContentPreview(themeMode: ThemeMode) {
    MoeKoeTheme(themeMode = themeMode) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MoeSectionHeader(
                title = "每日推荐",
                supportingText = "为你挑选的新鲜音乐",
                action = { Text("更多", color = MaterialTheme.colorScheme.primary) },
            )
            MoeSongRow(
                title = "コレカラ（从今以后）",
                subtitle = "Machico",
                metadata = "4:28",
                onClick = {},
                artwork = artwork(0),
                badges = badge("HQ"),
            )
            HorizontalDivider()
            MoeSongRow(
                title = "这首歌的名字比较长会换行显示但不会挤压操作",
                subtitle = "LiSA",
                metadata = "4:15",
                isPlaying = true,
                onClick = {},
                artwork = artwork(1),
                badges = badge("MV", MoeMediaBadgeTone.Error),
            )
            HorizontalDivider()
            MoeSongRow(
                title = "IGNITE",
                subtitle = "蓝井艾露",
                metadata = "4:36",
                enabled = false,
                onClick = {},
                artwork = artwork(2),
                badges = badge("HQ", MoeMediaBadgeTone.Neutral),
            )
            MoeMiniPlayer(
                title = "コレカラ（从今以后）",
                artist = "Machico",
                isPlaying = true,
                progress = 0.38f,
                positionLabel = "1:24",
                durationLabel = "4:28",
                badgeLabel = "标准",
                playContentDescription = "播放",
                pauseContentDescription = "暂停",
                previousContentDescription = "上一首",
                nextContentDescription = "下一首",
                queueContentDescription = "播放队列",
                onTogglePlayback = {},
                onPrevious = {},
                onNext = {},
                onOpenQueue = {},
                artwork = artwork(0),
            )
            MoeSongRow(
                title = "コレカラ（从今以后）",
                subtitle = "Machico",
                metadata = "3:04",
                isPlaying = true,
                onClick = {},
                style = MoeSongRowStyle.Queue,
                showPlayingIndicator = false,
                artwork = artwork(0),
                leading = {
                    Box(
                        modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "♪",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
                trailing = {
                    IconButton(onClick = {}, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "从队列移除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SongRowContractPreview(showProbe: Boolean) {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        MoeSongRow(
            title = "コレカラ（从今以后）",
            subtitle = "Machico",
            metadata = "4:28",
            onClick = {},
            modifier = Modifier.songRowContractProbe(showProbe, Color.Magenta),
            artwork = {
                Box(Modifier.fillMaxSize().songRowContractProbe(showProbe, Color.Cyan)) {
                    Image(
                        painter = painterResource(ScreenshotTestR.drawable.mini_player_artwork),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            },
            badges = {
                MoeMediaBadge(text = "HQ")
                MoeMediaBadge(text = "MV", tone = MoeMediaBadgeTone.Error)
            },
            trailing = {
                cn.james.music.core.designsystem.component.MoeSongMoreAction(
                    contentDescription = "更多",
                    onClick = {},
                    modifier = Modifier.songRowContractProbe(showProbe, Color.Green),
                )
            },
        )
    }
}

private fun Modifier.songRowContractProbe(
    enabled: Boolean,
    color: Color,
): Modifier =
    if (!enabled) {
        this
    } else {
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }

@Composable
private fun MiniPlayerPreview(themeMode: ThemeMode) {
    MoeKoeTheme(themeMode = themeMode) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.Center,
        ) {
            PreviewMiniPlayer()
        }
    }
}

@Composable
private fun PreviewMiniPlayer() {
    MoeMiniPlayer(
        title = "コレカラ（从今以后）",
        artist = "Machico",
        isPlaying = true,
        progress = 0.38f,
        positionLabel = "1:24",
        durationLabel = "4:28",
        badgeLabel = "标准",
        playContentDescription = "播放",
        pauseContentDescription = "暂停",
        previousContentDescription = "上一首",
        nextContentDescription = "下一首",
        queueContentDescription = "播放队列",
        onTogglePlayback = {},
        onPrevious = {},
        onNext = {},
        onOpenQueue = {},
        artwork = miniPlayerArtwork(),
    )
}

private fun miniPlayerArtwork(): @Composable BoxScope.() -> Unit = {
    Image(
        painter = painterResource(ScreenshotTestR.drawable.mini_player_artwork),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

private fun artwork(index: Int): @Composable BoxScope.() -> Unit = {
    val palettes =
        listOf(
            listOf(MaterialTheme.colorScheme.primaryContainer, MoeKoeTheme.extraColors.accentPink.copy(alpha = 0.72f)),
            listOf(MoeKoeTheme.extraColors.accentPurple.copy(alpha = 0.72f), MaterialTheme.colorScheme.primaryContainer),
            listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.outlineVariant),
        )
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(palettes[index])),
    )
}

private fun badge(
    text: String,
    tone: MoeMediaBadgeTone = MoeMediaBadgeTone.Primary,
): @Composable RowScope.() -> Unit = {
    MoeMediaBadge(text = text, tone = tone)
}
