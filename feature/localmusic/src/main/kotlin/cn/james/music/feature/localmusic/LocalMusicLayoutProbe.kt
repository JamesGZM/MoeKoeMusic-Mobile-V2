package cn.james.music.feature.localmusic

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal const val PROBE_LIBRARY_SEARCH = "librarySearch"
internal const val PROBE_LIBRARY_LIST = "libraryList"
internal const val PROBE_PERMISSION_HERO = "permissionHero"
internal const val PROBE_PERMISSION_ACTION = "permissionAction"
internal const val PROBE_SCAN_HEADER = "scanHeader"
internal const val PROBE_SCAN_LIST = "scanList"
internal const val PROBE_SELECTION_HEADER = "selectionHeader"
internal const val PROBE_SELECTION_ACTION = "selectionAction"

internal val LocalLocalMusicLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.localMusicLayoutProbe(name: String): Modifier =
    composed {
        val color = LocalLocalMusicLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }
