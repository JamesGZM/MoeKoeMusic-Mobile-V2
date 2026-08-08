package cn.james.music.feature.playlist

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal const val PLAYLIST_PROBE_TOOLBAR = "playlistToolbar"
internal const val PLAYLIST_PROBE_SUMMARY = "playlistSummary"
internal const val PLAYLIST_PROBE_COVER = "playlistCover"
internal const val PLAYLIST_PROBE_ACTIONS = "playlistActions"
internal const val PLAYLIST_PROBE_SONGS_HEADER = "playlistSongsHeader"
internal const val PLAYLIST_PROBE_FIRST_TRACK = "playlistFirstTrack"
internal const val PLAYLIST_PROBE_CURRENT_TRACK = "playlistCurrentTrack"

internal val LocalPlaylistDetailLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.playlistDetailLayoutProbe(name: String): Modifier =
    composed {
        val color = LocalPlaylistDetailLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }
