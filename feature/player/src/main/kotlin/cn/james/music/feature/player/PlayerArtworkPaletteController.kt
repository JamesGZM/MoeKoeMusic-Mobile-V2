package cn.james.music.feature.player

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.palette.graphics.Palette
import coil3.Image
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberPlayerArtworkPalette(
    item: PlayerItemUiModel?,
    dynamicCoverColors: Boolean,
): PlayerArtworkPaletteState {
    val scope = rememberCoroutineScope()
    var palette by remember { mutableStateOf(PlayerPalette.Static) }
    var extractionJob by remember { mutableStateOf<Job?>(null) }
    var decodedImage by remember { mutableStateOf<Image?>(null) }
    var decodedImageKey by remember { mutableStateOf<String?>(null) }
    val coordinator = remember { PlayerPaletteCoordinator() }

    val key = item?.paletteKey()

    fun extractFor(
        successKey: String,
        image: Image,
    ) {
        extractionJob?.cancel()
        extractionJob =
            scope.launch {
                val resolved =
                    withContext(Dispatchers.Default) {
                        extractPalette(image)
                    }
                if (coordinator.mayApply(successKey)) palette = resolved
            }
    }

    LaunchedEffect(key, dynamicCoverColors) {
        extractionJob?.cancel()
        extractionJob = null
        if (decodedImageKey != key) {
            decodedImage = null
            decodedImageKey = null
        }
        palette = coordinator.activate(key, dynamicCoverColors)
        if (dynamicCoverColors && key != null && decodedImageKey == key) {
            decodedImage?.takeIf { coordinator.shouldExtract(key) }?.let { extractFor(key, it) }
        }
    }

    return remember(scope, key, dynamicCoverColors) {
        PlayerArtworkPaletteState(
            palette = { palette },
            onArtworkSuccess = onSuccess@{ image ->
                val successKey = key ?: return@onSuccess
                decodedImage = image
                decodedImageKey = successKey
                if (!coordinator.shouldExtract(successKey)) return@onSuccess
                extractFor(successKey, image)
            },
        )
    }
}

internal class PlayerArtworkPaletteState(
    private val palette: () -> PlayerPalette,
    val onArtworkSuccess: (Image) -> Unit,
) {
    val current: PlayerPalette get() = palette()
}

internal fun extractPalette(image: Image): PlayerPalette =
    try {
        val bitmap = image.toBitmap()
        val sampled = bitmap.sampledAtMost(96)
        PlayerPaletteResolver.resolve(Palette.from(sampled).maximumColorCount(12).generate())
    } catch (error: Throwable) {
        if (error is kotlinx.coroutines.CancellationException) throw error
        PlayerPalette.Static
    }

private fun Bitmap.sampledAtMost(maxSide: Int): Bitmap {
    val largest = maxOf(width, height)
    if (largest <= maxSide) return this
    val scale = maxSide.toFloat() / largest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
}

private fun PlayerItemUiModel.paletteKey(): String = "$id:$artwork"
