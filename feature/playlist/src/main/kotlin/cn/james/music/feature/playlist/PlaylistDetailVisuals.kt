package cn.james.music.feature.playlist

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

@Composable
internal fun playlistDetailBackground(): Brush {
    val colors = MaterialTheme.colorScheme
    val isLight = colors.background.luminance() > 0.5f
    return if (isLight) {
        Brush.verticalGradient(
            listOf(
                colors.primaryContainer.copy(alpha = 0.32f),
                colors.background,
                colors.background,
            ),
        )
    } else {
        Brush.verticalGradient(listOf(colors.background, colors.background))
    }
}
