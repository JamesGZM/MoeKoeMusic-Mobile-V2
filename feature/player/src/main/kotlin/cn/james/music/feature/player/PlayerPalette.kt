package cn.james.music.feature.player

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Immutable
internal data class PlayerPalette(
    val base: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val glowTertiary: Color,
    val glowQuaternary: Color,
    val backgroundBrightest: Color,
    val accent: Color,
    val onAccent: Color,
    val secondaryContainer: Color,
    val secondaryContent: Color,
) {
    companion object {
        val Static =
            PlayerPalette(
                base = Color(0xFF07101F),
                glowPrimary = Color(0x8C6A2C5C),
                glowSecondary = Color(0xC06A2058),
                glowTertiary = Color(0x7A173B69),
                glowQuaternary = Color(0x66552A58),
                backgroundBrightest = Color(0xFF32253E),
                accent = Color(0xFF9488FF),
                onAccent = Color(0xFF003258),
                secondaryContainer = Color(0xFF252A43),
                secondaryContent = Color(0xFFC3BCFF),
            )
    }
}

internal val LocalPlayerPalette = staticCompositionLocalOf { PlayerPalette.Static }

internal object PlayerPaletteResolver {
    fun resolve(palette: Palette): PlayerPalette {
        val candidate =
            palette.swatches
                .asSequence()
                .map { Color(it.rgb) }
                .firstOrNull { it.isCandidate() }
                ?: return PlayerPalette.Static
        return resolve(candidate)
    }

    fun resolve(candidate: Color?): PlayerPalette {
        if (candidate == null || !candidate.isCandidate()) return PlayerPalette.Static
        val backdrop = candidate.mix(PlayerPalette.Static.base, 0.72f)
        val glowPrimary = backdrop.withAlpha(0.58f)
        val glowSecondary = backdrop.withAlpha(0.75f)
        val glowTertiary = backdrop.mix(Color(0xFF173B69), 0.45f).withAlpha(0.48f)
        val glowQuaternary = backdrop.mix(Color(0xFF552A58), 0.45f).withAlpha(0.40f)
        val backgroundBrightest =
            brightestCanvasBackdrop(
                base = PlayerPalette.Static.base,
                layers = listOf(glowPrimary, glowSecondary, glowTertiary, glowQuaternary),
            )
        val accent =
            listOf(candidate, candidate.mix(Color.White, 0.45f), candidate.mix(Color.Black, 0.6f), Color.White)
                .firstOrNull { color ->
                    color.contrastRatio(backgroundBrightest) >= 3f &&
                        (Color.White.contrastRatio(color) >= 4.5f || Color(0xFF07101F).contrastRatio(color) >= 4.5f)
                }
                ?: PlayerPalette.Static.accent
        val onAccent = if (Color.White.contrastRatio(accent) >= 4.5f) Color.White else Color(0xFF07101F)
        return PlayerPalette(
            base = PlayerPalette.Static.base,
            glowPrimary = glowPrimary,
            glowSecondary = glowSecondary,
            glowTertiary = glowTertiary,
            glowQuaternary = glowQuaternary,
            backgroundBrightest = backgroundBrightest,
            accent = accent,
            onAccent = onAccent,
            secondaryContainer = backdrop.mix(PlayerPalette.Static.secondaryContainer, 0.58f),
            secondaryContent = Color.White.copy(alpha = 0.82f),
        )
    }
}

internal fun brightestCanvasBackdrop(
    base: Color,
    layers: List<Color>,
): Color {
    var accumulated = base
    var brightest = base
    layers.forEach { layer ->
        accumulated = layer.compositeOver(accumulated)
        if (accumulated.luminance() > brightest.luminance()) brightest = accumulated
    }
    return brightest
}

internal fun Color.contrastRatio(other: Color): Float {
    val lighter = max(luminance(), other.luminance())
    val darker = min(luminance(), other.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun Color.isCandidate(): Boolean = alpha > 0f && luminance() in 0.035f..0.9f

internal fun Color.luminance(): Float {
    fun channel(value: Float): Float = if (value <= 0.03928f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

private fun Float.pow(power: Float): Float = toDouble().pow(power.toDouble()).toFloat()

private fun Color.mix(
    other: Color,
    otherFraction: Float,
): Color {
    val amount = otherFraction.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * amount,
        green = green + (other.green - green) * amount,
        blue = blue + (other.blue - blue) * amount,
        alpha = alpha + (other.alpha - alpha) * amount,
    )
}

private fun Color.withAlpha(value: Float): Color = copy(alpha = value.coerceIn(0f, 1f))

internal fun Color.compositeOver(background: Color): Color {
    val sourceAlpha = alpha
    val outputAlpha = sourceAlpha + background.alpha * (1f - sourceAlpha)
    if (outputAlpha == 0f) return Color.Transparent
    return Color(
        red = (red * sourceAlpha + background.red * background.alpha * (1f - sourceAlpha)) / outputAlpha,
        green = (green * sourceAlpha + background.green * background.alpha * (1f - sourceAlpha)) / outputAlpha,
        blue = (blue * sourceAlpha + background.blue * background.alpha * (1f - sourceAlpha)) / outputAlpha,
        alpha = outputAlpha,
    )
}
