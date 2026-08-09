package cn.james.music.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class DesignTokensTest {
    @Test
    fun skyBlueReturnsTheApprovedLightDarkAndAmoledBaselines() {
        assertSame(
            LightColors,
            moeKoeColorScheme(
                themeMode = ThemeMode.Light,
                brandThemeColor = BrandThemeColor.SkyBlue,
                systemDark = false,
            ),
        )
        assertSame(
            DarkColors,
            moeKoeColorScheme(
                themeMode = ThemeMode.Dark,
                brandThemeColor = BrandThemeColor.SkyBlue,
                systemDark = true,
            ),
        )
        assertSame(
            AmoledColors,
            moeKoeColorScheme(
                themeMode = ThemeMode.Amoled,
                brandThemeColor = BrandThemeColor.SkyBlue,
                systemDark = true,
            ),
        )
    }

    @Test
    fun everyBrandKeepsThemeModeAndNonPrimaryRolesIndependent() {
        BrandThemeColor.entries.forEach { brandThemeColor ->
            assertPrimaryRolesOnlyChange(
                baseline = LightColors,
                actual = moeKoeColorScheme(ThemeMode.Light, brandThemeColor, systemDark = false),
            )
            assertPrimaryRolesOnlyChange(
                baseline = DarkColors,
                actual = moeKoeColorScheme(ThemeMode.Dark, brandThemeColor, systemDark = true),
            )
            assertPrimaryRolesOnlyChange(
                baseline = AmoledColors,
                actual = moeKoeColorScheme(ThemeMode.Amoled, brandThemeColor, systemDark = true),
            )
        }
    }

    @Test
    fun everyBrandPrimaryPairMeetsTextContrastInEachThemeMode() {
        BrandThemeColor.entries
            .filterNot { it == BrandThemeColor.SkyBlue }
            .forEach { brandThemeColor ->
                listOf(
                    moeKoeColorScheme(ThemeMode.Light, brandThemeColor, systemDark = false),
                    moeKoeColorScheme(ThemeMode.Dark, brandThemeColor, systemDark = true),
                    moeKoeColorScheme(ThemeMode.Amoled, brandThemeColor, systemDark = true),
                ).forEach { colorScheme ->
                    assertTrue(contrastRatio(colorScheme.primary, colorScheme.onPrimary) >= 4.5)
                    assertTrue(contrastRatio(colorScheme.primaryContainer, colorScheme.onPrimaryContainer) >= 4.5)
                }
            }
    }

    @Test
    fun `light theme matches approved brand colors`() {
        assertEquals(Color(0xFF1677F2), LightColors.primary)
        assertEquals(Color(0xFFE8F2FF), LightColors.primaryContainer)
        assertEquals(Color(0xFFFBFCFE), LightColors.background)
        assertEquals(Color(0xFF1A1C20), LightColors.onSurface)
        assertEquals(Color(0xFFE1E7EF), LightColors.outlineVariant)
    }

    @Test
    fun `light semantic colors match the approved feedback palette`() {
        assertEquals(Color(0xFF0B9B66), LightExtraColors.success)
        assertEquals(Color(0xFFDDF8EB), LightExtraColors.successContainer)
        assertEquals(Color(0xFFB77800), LightExtraColors.warning)
        assertEquals(Color(0xFFFFF0CD), LightExtraColors.warningContainer)
    }

    @Test
    fun `dark and amoled themes keep distinct surface hierarchy`() {
        assertEquals(Color(0xFF0F1218), DarkColors.background)
        assertEquals(Color(0xFF1A1F29), DarkColors.surface)
        assertEquals(Color.Black, AmoledColors.background)
        assertEquals(Color.Black, AmoledColors.surface)
        assertEquals(Color(0xFF111111), AmoledColors.surfaceContainer)
    }

    @Test
    fun `spacing and dimensions expose the locked mobile scale`() {
        val spacing = MoeKoeSpacing()
        val dimensions = MoeKoeDimensions()

        assertEquals(
            listOf(4.dp, 8.dp, 12.dp, 16.dp, 20.dp, 24.dp, 32.dp, 40.dp),
            listOf(
                spacing.space4,
                spacing.space8,
                spacing.space12,
                spacing.space16,
                spacing.space20,
                spacing.space24,
                spacing.space32,
                spacing.space40,
            ),
        )
        assertEquals(48.dp, dimensions.minimumTouchTarget)
        assertEquals(56.dp, dimensions.inputHeight)
        assertEquals(64.dp, dimensions.toolbarHeight)
    }

    @Test
    fun `typography keeps body copy readable on phones`() {
        assertEquals(28.sp, MoeKoeTypography.headlineLarge.fontSize)
        assertEquals(18.sp, MoeKoeTypography.titleMedium.fontSize)
        assertEquals(16.sp, MoeKoeTypography.bodyLarge.fontSize)
        assertEquals(14.sp, MoeKoeTypography.bodyMedium.fontSize)
        assertEquals(22.sp, MoeKoeTypography.bodyMedium.lineHeight)
        assertEquals(12.sp, MoeKoeTypography.bodySmall.fontSize)
    }

    private fun assertPrimaryRolesOnlyChange(
        baseline: androidx.compose.material3.ColorScheme,
        actual: androidx.compose.material3.ColorScheme,
    ) {
        assertEquals(baseline.secondary, actual.secondary)
        assertEquals(baseline.onSecondary, actual.onSecondary)
        assertEquals(baseline.secondaryContainer, actual.secondaryContainer)
        assertEquals(baseline.onSecondaryContainer, actual.onSecondaryContainer)
        assertEquals(baseline.tertiary, actual.tertiary)
        assertEquals(baseline.onTertiary, actual.onTertiary)
        assertEquals(baseline.tertiaryContainer, actual.tertiaryContainer)
        assertEquals(baseline.onTertiaryContainer, actual.onTertiaryContainer)
        assertEquals(baseline.background, actual.background)
        assertEquals(baseline.surface, actual.surface)
        assertEquals(baseline.error, actual.error)
        assertEquals(baseline.onError, actual.onError)
        assertEquals(baseline.errorContainer, actual.errorContainer)
        assertEquals(baseline.onErrorContainer, actual.onErrorContainer)
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Double {
        val lighter = maxOf(first.relativeLuminance(), second.relativeLuminance())
        val darker = minOf(first.relativeLuminance(), second.relativeLuminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun Color.relativeLuminance(): Double =
        0.2126 * red.linearized() +
            0.7152 * green.linearized() + 0.0722 * blue.linearized()

    private fun Float.linearized(): Double =
        if (this <= 0.04045f) {
            (this / 12.92f).toDouble()
        } else {
            ((this + 0.055f) / 1.055f).toDouble().pow(2.4)
        }
}
