package cn.james.music.feature.login

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal val LocalLoginLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.loginLayoutProbe(name: String): Modifier =
    composed {
        val color = LocalLoginLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }
