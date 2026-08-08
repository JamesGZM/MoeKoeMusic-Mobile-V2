package cn.james.music.feature.profile

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal val LocalUserProfileLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.userProfileLayoutProbe(name: String): Modifier =
    composed {
        val color = LocalUserProfileLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }

internal fun Modifier.userProfileBoundsProbe(
    topName: String,
    bottomName: String,
): Modifier =
    composed {
        val topColor = LocalUserProfileLayoutProbeColors.current[topName]
        val bottomColor = LocalUserProfileLayoutProbeColors.current[bottomName]
        if (topColor == null && bottomColor == null) return@composed this
        drawWithContent {
            drawContent()
            topColor?.let {
                drawRect(
                    color = it,
                    topLeft = Offset(size.width / 2f - 4f, 0f),
                    size = Size(8f, 4f),
                )
            }
            bottomColor?.let {
                drawRect(
                    color = it,
                    topLeft = Offset(size.width / 2f - 4f, size.height - 4f),
                    size = Size(8f, 4f),
                )
            }
        }
    }
