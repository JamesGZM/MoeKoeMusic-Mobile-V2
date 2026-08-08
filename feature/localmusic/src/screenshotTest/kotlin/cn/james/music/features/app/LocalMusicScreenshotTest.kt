package cn.james.music.features.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.feature.localmusic.DeviceImportUiState
import cn.james.music.feature.localmusic.LocalMusicImportScreen
import cn.james.music.feature.localmusic.LocalLocalMusicLayoutProbeColors
import cn.james.music.feature.localmusic.LocalMusicScreen
import cn.james.music.feature.localmusic.LocalMusicUiState
import cn.james.music.feature.localmusic.PROBE_LIBRARY_LIST
import cn.james.music.feature.localmusic.PROBE_LIBRARY_SEARCH
import cn.james.music.feature.localmusic.PROBE_PERMISSION_ACTION
import cn.james.music.feature.localmusic.PROBE_PERMISSION_HERO
import cn.james.music.feature.localmusic.PROBE_SCAN_HEADER
import cn.james.music.feature.localmusic.PROBE_SCAN_LIST
import cn.james.music.feature.localmusic.PROBE_SELECTION_ACTION
import cn.james.music.feature.localmusic.PROBE_SELECTION_HEADER
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Empty", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicEmptyScreenshot() {
    LocalMusicScreenshotContent(LocalMusicUiState())
}

@PreviewTest
@Preview(name = "Content", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicContentScreenshot() {
    LocalMusicScreenshotContent(LocalMusicUiState(music = previewMusic))
}

@PreviewTest
@Preview(name = "ContentLayoutProbe", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicContentLayoutProbeScreenshot() {
    CompositionLocalProvider(
        LocalLocalMusicLayoutProbeColors provides
            mapOf(
                PROBE_LIBRARY_SEARCH to Color.Magenta,
                PROBE_LIBRARY_LIST to Color.Cyan,
            ),
    ) {
        LocalMusicScreenshotContent(LocalMusicUiState(music = previewMusic))
    }
}

@PreviewTest
@Preview(name = "ImportingLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LocalMusicImportingLargeTextScreenshot() {
    LocalMusicScreenshotContent(
        LocalMusicUiState(
            music = previewMusic,
            imports =
                listOf(
                    LocalImportProgress(
                        batchId = "preview-import",
                        state = LocalImportBatchState.Running,
                        totalCount = 5,
                        completedCount = 2,
                        failedCount = 0,
                        currentDisplayName = "夏日旋律.flac",
                        copiedBytes = 4_200_000,
                        totalBytes = 8_400_000,
                    ),
                ),
        ),
    )
}

@PreviewTest
@Preview(name = "ImportPermission", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicImportPermissionScreenshot() {
    LocalMusicImportScreenshotContent(DeviceImportUiState.PermissionRequired)
}

@PreviewTest
@Preview(name = "ImportPermissionLayoutProbe", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicImportPermissionLayoutProbeScreenshot() {
    ImportLayoutProbe(
        state = DeviceImportUiState.PermissionRequired,
        colors = mapOf(PROBE_PERMISSION_HERO to Color.Magenta, PROBE_PERMISSION_ACTION to Color.Cyan),
    )
}

@PreviewTest
@Preview(name = "ImportScanning", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicImportScanningScreenshot() {
    LocalMusicImportScreenshotContent(DeviceImportUiState.Scanning(previewCandidates))
}

@PreviewTest
@Preview(name = "ImportScanningLayoutProbe", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicImportScanningLayoutProbeScreenshot() {
    ImportLayoutProbe(
        state = DeviceImportUiState.Scanning(previewCandidates),
        colors = mapOf(PROBE_SCAN_HEADER to Color.Magenta, PROBE_SCAN_LIST to Color.Cyan),
    )
}

@PreviewTest
@Preview(name = "ImportSelection", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicImportSelectionScreenshot() {
    LocalMusicImportScreenshotContent(selectionPreviewState)
}

@PreviewTest
@Preview(name = "ImportSelectionLayoutProbe", widthDp = 390, heightDp = 844)
@Composable
fun LocalMusicImportSelectionLayoutProbeScreenshot() {
    ImportLayoutProbe(
        state = selectionPreviewState,
        colors = mapOf(PROBE_SELECTION_HEADER to Color.Magenta, PROBE_SELECTION_ACTION to Color.Cyan),
    )
}

@Composable
private fun ImportLayoutProbe(
    state: DeviceImportUiState,
    colors: Map<String, Color>,
) {
    CompositionLocalProvider(LocalLocalMusicLayoutProbeColors provides colors) {
        LocalMusicImportScreenshotContent(state)
    }
}

@Composable
private fun LocalMusicScreenshotContent(state: LocalMusicUiState) {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier) {
            LocalMusicScreen(
                state = state,
                onBack = {},
                onImport = {},
                onPlay = { _, _ -> },
                onDelete = {},
                onCancelImport = {},
            )
        }
    }
}

@Composable
private fun LocalMusicImportScreenshotContent(state: DeviceImportUiState) {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier) {
            LocalMusicImportScreen(
                state = state,
                onBack = {},
                onGrantPermission = {},
                onRetry = {},
                onToggleCandidate = {},
                onToggleAll = {},
                onImport = {},
            )
        }
    }
}

private val previewMusic =
    listOf(
        LocalMusic("local-1", "夏日旋律", "MoeKoe", "蓝色时刻", 218_000, 8_400_000, "audio/flac", null, 3),
        LocalMusic("local-2", "Two Faced", "Linkin Park", null, 186_000, 5_200_000, "audio/mpeg", null, 2),
        LocalMusic("local-3", "Whose Blue", "Y 2025", "Whose Blue", 241_000, 7_100_000, "audio/mp4", null, 1),
        LocalMusic("local-4", "夜间电台", "Miku", null, 275_000, 6_800_000, "audio/ogg", null, 0),
        LocalMusic("local-5", "城市漫游", "MoeKoe", null, 168_000, 5_100_000, "audio/mpeg", null, -1),
        LocalMusic("local-6", "林间微风", "Lofi Girl", null, 202_000, 7_600_000, "audio/flac", null, -2),
        LocalMusic("local-7", "午后咖啡", "Re:Plus", null, 177_000, 5_900_000, "audio/mpeg", null, -3),
        LocalMusic("local-8", "星空下的梦", "MoeKoe", null, 312_000, 9_200_000, "audio/flac", null, -4),
        LocalMusic("local-9", "海岸线", "MoeKoe", null, 221_000, 6_200_000, "audio/mp4", null, -5),
        LocalMusic("local-10", "樱花落", "Axis", null, 243_000, 7_800_000, "audio/flac", null, -6),
    )

private val previewCandidates =
    listOf(
        DeviceAudioCandidate(1, "夏日旋律.flac", "MoeKoe", 218_000, 8_400_000),
        DeviceAudioCandidate(2, "Two Faced.mp3", "Linkin Park", 186_000, 5_200_000),
        DeviceAudioCandidate(3, "night-radio.ogg", null, 241_000, 7_100_000),
        DeviceAudioCandidate(4, "城市漫游.mp3", "MoeKoe", 168_000, 5_100_000),
        DeviceAudioCandidate(5, "林间微风.flac", "Lofi Girl", 202_000, 7_600_000),
        DeviceAudioCandidate(6, "午后咖啡.mp3", "Re:Plus", 177_000, 5_900_000),
        DeviceAudioCandidate(7, "星空下的梦.flac", "MoeKoe", 312_000, 9_200_000),
        DeviceAudioCandidate(8, "海岸线.m4a", "MoeKoe", 221_000, 6_200_000),
        DeviceAudioCandidate(9, "樱花落.flac", "Axis", 243_000, 7_800_000),
    )

private val selectionPreviewState =
    DeviceImportUiState.Selection(
        candidates = previewCandidates,
        selectedIds = setOf(1, 3, 4, 7),
    )
