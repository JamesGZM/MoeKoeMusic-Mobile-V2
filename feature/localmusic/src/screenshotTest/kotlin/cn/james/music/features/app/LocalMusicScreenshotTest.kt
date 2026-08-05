package cn.james.music.features.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.feature.localmusic.LocalMusicScreen
import cn.james.music.feature.localmusic.LocalMusicUiState
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

@Composable
private fun LocalMusicScreenshotContent(state: LocalMusicUiState) {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier) {
            LocalMusicScreen(
                state = state,
                onBack = {},
                onChooseFiles = {},
                onScan = {},
                onPlay = { _, _ -> },
                onDelete = {},
                onCancelImport = {},
            )
        }
    }
}

private val previewMusic =
    listOf(
        LocalMusic("local-1", "夏日旋律", "MoeKoe", "蓝色时刻", 218_000, 8_400_000, "audio/flac", null, 3),
        LocalMusic("local-2", "Two Faced", "Linkin Park", null, 186_000, 5_200_000, "audio/mpeg", null, 2),
        LocalMusic("local-3", "Whose Blue", "Y 2025", "Whose Blue", 241_000, 7_100_000, "audio/mp4", null, 1),
    )
