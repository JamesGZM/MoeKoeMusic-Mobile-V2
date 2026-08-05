package cn.james.music.feature.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.playback.PlaybackConnectionState
import cn.james.music.playback.PlaybackStatus
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FoundationShowcaseRoute(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaybackLabViewModel = hiltViewModel(),
) {
    val playbackState by viewModel.uiState.collectAsStateWithLifecycle()
    DesignSystemShowcaseScreen(
        selectedTheme = selectedTheme,
        onThemeSelected = onThemeSelected,
        playbackLabState = playbackState,
        onPlaybackAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun DesignSystemShowcaseScreen(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    playbackLabState: PlaybackLabUiState? = null,
    onPlaybackAction: (PlaybackLabAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("showcase-list"),
            contentPadding = PaddingValues(MoeKoeTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.large),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                    Text(
                        text = "MoeKoe Music",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("showcase-title"),
                    )
                    Text(
                        text = "播放内核阶段 · Foundation Showcase",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                ShowcaseSection(title = "主题") {
                    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                        ThemeMode.entries.chunked(2).forEach { rowModes ->
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(MoeKoeTheme.spacing.small),
                            ) {
                                rowModes.forEach { mode ->
                                    FilterChip(
                                        selected = selectedTheme == mode,
                                        onClick = { onThemeSelected(mode) },
                                        label = { Text(mode.displayName) },
                                        modifier =
                                            Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("theme-${mode.name}"),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ShowcaseSection(title = "品牌与语义色") {
                    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                        ColorSample(
                            label = "Primary",
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        ColorSample(
                            label = "Success",
                            container = MoeKoeTheme.extraColors.successContainer,
                            content = MoeKoeTheme.extraColors.onSuccessContainer,
                        )
                        ColorSample(
                            label = "Warning",
                            container = MoeKoeTheme.extraColors.warningContainer,
                            content = MoeKoeTheme.extraColors.onWarningContainer,
                        )
                        ColorSample(
                            label = "Error",
                            container = MaterialTheme.colorScheme.errorContainer,
                            content = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            item {
                ShowcaseSection(title = "排版与操作") {
                    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                        Text("标题层级", style = MaterialTheme.typography.titleLarge)
                        Text("正文保持真实手机上的清晰可读性。", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "说明文字原则上不低于 14sp。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {},
                            modifier = Modifier.heightIn(min = 48.dp).testTag("primary-action"),
                        ) {
                            Text("主要操作")
                        }
                    }
                }
            }

            playbackLabState?.let { state ->
                item {
                    PlaybackLabSection(
                        state = state,
                        onAction = onPlaybackAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackLabSection(
    state: PlaybackLabUiState,
    onAction: (PlaybackLabAction) -> Unit,
) {
    ShowcaseSection(title = "播放内核工程实验台") {
        Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
            Text(
                text = "连接：${state.playback.connection.displayName} · 状态：${state.playback.status.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("playback-status"),
            )
            Text(
                text = state.playback.currentItem?.title ?: "尚未载入测试队列",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("playback-current-title"),
            )
            Text(
                text = state.playback.currentItem?.artist ?: "测试音频由 App 本地生成，不访问网络",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.playback.queue.isEmpty()) {
                Button(
                    onClick = { onAction(PlaybackLabAction.LoadDemo) },
                    enabled = state.playback.connection == PlaybackConnectionState.Connected,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("load-demo-queue"),
                ) {
                    Text("载入测试队列")
                }
            } else {
                PlaybackProgressControl(state, onAction)
                Row(horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                    OutlinedButton(
                        onClick = { onAction(PlaybackLabAction.Previous) },
                        modifier = Modifier.heightIn(min = 48.dp).weight(1f),
                    ) {
                        Text("上一首")
                    }
                    Button(
                        onClick = { onAction(PlaybackLabAction.TogglePlayback) },
                        modifier = Modifier.heightIn(min = 48.dp).weight(1f).testTag("playback-toggle"),
                    ) {
                        Text(if (state.playback.isPlaying) "暂停" else "播放")
                    }
                    OutlinedButton(
                        onClick = { onAction(PlaybackLabAction.Next) },
                        modifier = Modifier.heightIn(min = 48.dp).weight(1f),
                    ) {
                        Text("下一首")
                    }
                }

                Text("播放模式", style = MaterialTheme.typography.titleSmall)
                PlaybackMode.entries.chunked(2).forEach { modes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                        modes.forEach { mode ->
                            FilterChip(
                                selected = state.playback.mode == mode,
                                onClick = { onAction(PlaybackLabAction.SetMode(mode)) },
                                label = { Text(mode.displayName) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }

                Text("当前队列", style = MaterialTheme.typography.titleSmall)
                state.playback.queue.forEachIndexed { index, item ->
                    Surface(
                        color =
                            if (index == state.playback.currentIndex) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    item.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = { onAction(PlaybackLabAction.Move(index, index - 1)) },
                                enabled = index > 0,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("上移") }
                            TextButton(
                                onClick = { onAction(PlaybackLabAction.Remove(index)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("删除") }
                        }
                    }
                }
                TextButton(
                    onClick = { onAction(PlaybackLabAction.Clear) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("清空队列")
                }
            }
        }
    }
}

@Composable
private fun PlaybackProgressControl(
    state: PlaybackLabUiState,
    onAction: (PlaybackLabAction) -> Unit,
) {
    val duration = state.progress.durationMs.coerceAtLeast(1)
    var sliderPosition by remember(state.playback.currentItem?.id, state.progress.positionMs) {
        mutableFloatStateOf(state.progress.positionMs.coerceIn(0, duration).toFloat())
    }
    Slider(
        value = sliderPosition,
        onValueChange = { sliderPosition = it },
        onValueChangeFinished = { onAction(PlaybackLabAction.SeekTo(sliderPosition.toLong())) },
        valueRange = 0f..duration.toFloat(),
        modifier = Modifier.fillMaxWidth().testTag("playback-progress"),
    )
    Text(
        text = "${state.progress.positionMs.formatDuration()} / ${state.progress.durationMs.formatDuration()}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val PlaybackConnectionState.displayName: String
    get() =
        when (this) {
            PlaybackConnectionState.Connecting -> "连接中"
            PlaybackConnectionState.Connected -> "已连接"
            PlaybackConnectionState.Disconnected -> "已断开"
        }

private val PlaybackStatus.displayName: String
    get() =
        when (this) {
            PlaybackStatus.Idle -> "空闲"
            PlaybackStatus.Buffering -> "缓冲中"
            PlaybackStatus.Ready -> "就绪"
            PlaybackStatus.Ended -> "已结束"
        }

private val PlaybackMode.displayName: String
    get() =
        when (this) {
            PlaybackMode.Sequential -> "顺序播放"
            PlaybackMode.RepeatAll -> "列表循环"
            PlaybackMode.RepeatOne -> "单曲循环"
            PlaybackMode.Shuffle -> "随机播放"
        }

private fun Long.formatDuration(): String {
    val totalSeconds = (this / 1_000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun ShowcaseSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MoeKoeTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.medium),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ColorSample(
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(container, MaterialTheme.shapes.medium)
                .padding(MoeKoeTheme.spacing.medium),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, color = content, style = MaterialTheme.typography.bodyLarge)
    }
}

private val ThemeMode.displayName: String
    get() =
        when (this) {
            ThemeMode.System -> "跟随系统"
            ThemeMode.Light -> "浅色"
            ThemeMode.Dark -> "深色"
            ThemeMode.Amoled -> "纯黑"
        }

@Preview(name = "Light", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun DesignSystemShowcaseLightPreview() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        DesignSystemShowcaseScreen(selectedTheme = ThemeMode.Light, onThemeSelected = {})
    }
}

@Preview(name = "Dark", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun DesignSystemShowcaseDarkPreview() {
    MoeKoeTheme(themeMode = ThemeMode.Dark) {
        DesignSystemShowcaseScreen(selectedTheme = ThemeMode.Dark, onThemeSelected = {})
    }
}

@Preview(name = "Amoled", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun DesignSystemShowcaseAmoledPreview() {
    MoeKoeTheme(themeMode = ThemeMode.Amoled) {
        DesignSystemShowcaseScreen(selectedTheme = ThemeMode.Amoled, onThemeSelected = {})
    }
}
