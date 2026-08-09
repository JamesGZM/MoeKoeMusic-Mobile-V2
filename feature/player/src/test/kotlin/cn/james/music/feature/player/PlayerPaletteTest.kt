package cn.james.music.feature.player

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPaletteTest {
    @Test
    fun resolverFallsBackForMissingAndExtremeCandidates() {
        assertEquals(PlayerPalette.Static, PlayerPaletteResolver.resolve(null))
        assertEquals(PlayerPalette.Static, PlayerPaletteResolver.resolve(Color(0xFF020202)))
        assertEquals(PlayerPalette.Static, PlayerPaletteResolver.resolve(Color(0xFFF6F6F6)))
    }

    @Test
    fun resolverKeepsMiddleGrayReadableOrUsesTheWholeStaticFallback() {
        val palette = PlayerPaletteResolver.resolve(Color(0xFF777777))

        assertPaletteReadable(palette)
    }

    @Test
    fun resolverKeepsBrightDarkAndHighSaturationCandidatesReadable() {
        listOf(Color(0xFFF5A623), Color(0xFF173B69), Color(0xFFEA245C)).forEach { candidate ->
            val palette = PlayerPaletteResolver.resolve(candidate)
            assertFalse(palette == PlayerPalette.Static)
            assertPaletteReadable(palette)
        }
    }

    @Test
    fun resolverUsesCumulativeCanvasBackdropRatherThanTheBrightestSingleGlow() {
        val palette = PlayerPaletteResolver.resolve(Color(0xFFEA245C))
        val layers =
            listOf(
                palette.glowPrimary,
                palette.glowSecondary,
                palette.glowTertiary,
                palette.glowQuaternary,
            )
        val brightestSingleGlow = layers.maxOf { it.compositeOver(palette.base).luminance() }

        assertTrue(palette.backgroundBrightest.luminance() > brightestSingleGlow)
        assertEquals(brightestCanvasBackdrop(palette.base, layers), palette.backgroundBrightest)
        assertPaletteReadable(palette)
    }

    @Test
    fun coordinatorDiscardsLateArtworkAndDoesNotExtractSameItemTwice() {
        val coordinator = PlayerPaletteCoordinator()
        coordinator.activate("one:art", dynamicCoverColors = true)
        assertTrue(coordinator.shouldExtract("one:art"))
        assertFalse(coordinator.shouldExtract("one:art"))

        coordinator.activate("two:art", dynamicCoverColors = true)
        assertFalse(coordinator.mayApply("one:art"))
        assertTrue(coordinator.mayApply("two:art"))
    }

    @Test
    fun coordinatorDisablesCurrentPaletteImmediatelyAndRequiresNewSuccessWhenReenabled() {
        val coordinator = PlayerPaletteCoordinator()
        coordinator.activate("one:art", dynamicCoverColors = true)
        assertTrue(coordinator.shouldExtract("one:art"))

        assertEquals(PlayerPalette.Static, coordinator.activate("one:art", dynamicCoverColors = false))
        assertFalse(coordinator.shouldExtract("one:art"))
        coordinator.activate("one:art", dynamicCoverColors = true)
        assertTrue(coordinator.shouldExtract("one:art"))
    }

    private fun assertPaletteReadable(palette: PlayerPalette) {
        assertTrue(palette.accent.contrastRatio(palette.backgroundBrightest) >= 3f)
        assertTrue(palette.onAccent.contrastRatio(palette.accent) >= 4.5f)
    }
}
