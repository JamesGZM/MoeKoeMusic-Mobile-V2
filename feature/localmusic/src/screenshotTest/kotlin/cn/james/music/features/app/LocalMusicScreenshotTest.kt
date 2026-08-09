package cn.james.music.features.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.feature.localmusic.DeviceCandidateArtistUi
import cn.james.music.feature.localmusic.DeviceCandidateRowUiModel
import cn.james.music.feature.localmusic.DeviceImportAction
import cn.james.music.feature.localmusic.DeviceImportUiState
import cn.james.music.feature.localmusic.LocalArtworkUiModel
import cn.james.music.feature.localmusic.LocalImportProgressUiModel
import cn.james.music.feature.localmusic.LocalMusicImportScreen
import cn.james.music.feature.localmusic.LocalMusicAction
import cn.james.music.feature.localmusic.LocalMusicContentUi
import cn.james.music.feature.localmusic.LocalLocalMusicLayoutProbeColors
import cn.james.music.feature.localmusic.LocalMusicScreen
import cn.james.music.feature.localmusic.LocalMusicUiState
import cn.james.music.feature.localmusic.LocalSongRowUiModel
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
    LocalMusicScreenshotContent(
        LocalMusicUiState(
            songs = previewMusic,
            content = LocalMusicContentUi.Songs,
            playingSongId = previewMusic.first().id,
        ),
    )
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
        LocalMusicScreenshotContent(
            LocalMusicUiState(
                songs = previewMusic,
                content = LocalMusicContentUi.Songs,
                playingSongId = previewMusic.first().id,
            ),
        )
    }
}

@PreviewTest
@Preview(name = "ImportingLargeText", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LocalMusicImportingLargeTextScreenshot() {
    LocalMusicScreenshotContent(
        LocalMusicUiState(
            songs = previewMusic.map { song -> song.copy(isPlaying = false) },
            content = LocalMusicContentUi.Songs,
            activeImport = LocalImportProgressUiModel(id = "preview-import", totalCount = 5, completedCount = 2),
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
                onAction = { _: LocalMusicAction -> },
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
                onAction = { _: DeviceImportAction -> },
            )
        }
    }
}

private val previewMusic =
    listOf(
        previewSong("local-1", "夏日旋律", "MoeKoe", "3:38", isPlaying = true),
        previewSong("local-2", "Two Faced", "Linkin Park", "3:06"),
        previewSong("local-3", "Whose Blue", "Y 2025", "4:01"),
        previewSong("local-4", "夜间电台", "Miku", "4:35"),
        previewSong("local-5", "城市漫游", "MoeKoe", "2:48"),
        previewSong("local-6", "林间微风", "Lofi Girl", "3:22"),
        previewSong("local-7", "午后咖啡", "Re:Plus", "2:57"),
        previewSong("local-8", "星空下的梦", "MoeKoe", "5:12"),
        previewSong("local-9", "海岸线", "MoeKoe", "3:41"),
        previewSong("local-10", "樱花落", "Axis", "4:03"),
    )

private val previewCandidates =
    listOf(
        previewCandidate(1, "夏日旋律", "MoeKoe", "3:38"),
        previewCandidate(2, "Two Faced", "Linkin Park", "3:06"),
        DeviceCandidateRowUiModel(3, "night-radio", DeviceCandidateArtistUi.Unknown, "4:01"),
        previewCandidate(4, "城市漫游", "MoeKoe", "2:48"),
        previewCandidate(5, "林间微风", "Lofi Girl", "3:22"),
        previewCandidate(6, "午后咖啡", "Re:Plus", "2:57"),
        previewCandidate(7, "星空下的梦", "MoeKoe", "5:12"),
        previewCandidate(8, "海岸线", "MoeKoe", "3:41"),
        previewCandidate(9, "樱花落", "Axis", "4:03"),
    )

private val selectionPreviewState =
    DeviceImportUiState.Selection(
        candidates = previewCandidates,
        selectedIds = setOf(1, 3, 4, 7),
    )

private fun previewSong(
    id: String,
    title: String,
    artist: String,
    durationLabel: String,
    isPlaying: Boolean = false,
) = LocalSongRowUiModel(
    id = id,
    title = title,
    artist = artist,
    durationLabel = durationLabel,
    artwork = LocalArtworkUiModel.None,
    isPlaying = isPlaying,
)

private fun previewCandidate(
    id: Long,
    title: String,
    artist: String,
    durationLabel: String,
) = DeviceCandidateRowUiModel(
    id = id,
    title = title,
    artist = DeviceCandidateArtistUi.Known(artist),
    durationLabel = durationLabel,
)
