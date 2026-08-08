package cn.james.music.feature.settings

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal const val SETTINGS_PROBE_TOOLBAR = "settingsToolbar"
internal const val SETTINGS_PROBE_APPEARANCE = "settingsAppearance"
internal const val SETTINGS_PROBE_PLAYBACK = "settingsPlayback"
internal const val SETTINGS_PROBE_LYRICS = "settingsLyrics"
internal const val SETTINGS_PROBE_STORAGE = "settingsStorage"

internal val LocalSettingsLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.settingsLayoutProbe(name: String): Modifier =
    composed {
        val color = LocalSettingsLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }
