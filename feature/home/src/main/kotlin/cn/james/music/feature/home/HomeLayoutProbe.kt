package cn.james.music.feature.home

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal const val HOME_PROBE_HERO = "homeHero"
internal const val HOME_PROBE_QUICK_ENTRIES = "homeQuickEntries"
internal const val HOME_PROBE_DAILY_HEADER = "homeDailyHeader"
internal const val HOME_PROBE_FIRST_SONG = "homeFirstSong"
internal const val HOME_PROBE_PLAYLIST_HEADER = "homePlaylistHeader"
internal const val HOME_PROBE_PLAYLIST_GRID = "homePlaylistGrid"

internal val LocalHomeLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.homeLayoutProbe(name: String): Modifier =
    composed {
        val color = LocalHomeLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }
