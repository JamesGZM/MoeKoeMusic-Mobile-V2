package cn.james.music.feature.player

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

internal const val PLAYER_PROBE_TOP_BAR = "player.topBar"
internal const val PLAYER_PROBE_ARTWORK = "player.artwork"
internal const val PLAYER_PROBE_VIEWPORT = "player.lyricsViewport"
internal const val PLAYER_PROBE_SETTINGS = "player.lyricsSettings"
internal const val PLAYER_PROBE_INDICATOR = "player.indicator"
internal const val PLAYER_PROBE_INFO = "player.info"
internal const val PLAYER_PROBE_PROGRESS = "player.progress"
internal const val PLAYER_PROBE_CORE_CONTROLS = "player.coreControls"
internal const val PLAYER_PROBE_SECONDARY = "player.secondary"
internal const val PLAYER_PROBE_QUEUE_SHEET = "player.queue.sheet"
internal const val PLAYER_PROBE_QUEUE_HANDLE = "player.queue.handle"
internal const val PLAYER_PROBE_QUEUE_HEADER = "player.queue.header"
internal const val PLAYER_PROBE_QUEUE_SOURCE = "player.queue.source"
internal const val PLAYER_PROBE_QUEUE_FIRST_ITEM = "player.queue.firstItem"
internal const val PLAYER_PROBE_QUEUE_DISMISS = "player.queue.dismiss"

internal val LocalPlayerLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.playerLayoutProbe(
    name: String,
    horizontalFraction: Float = 0.5f,
): Modifier =
    composed {
        val color = LocalPlayerLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawIntoCanvas {
                drawRect(
                    color = color,
                    topLeft = Offset(size.width * horizontalFraction - 4f, 0f),
                    size =
                        androidx.compose.ui.geometry
                            .Size(8f, 8f),
                )
            }
        }
    }
