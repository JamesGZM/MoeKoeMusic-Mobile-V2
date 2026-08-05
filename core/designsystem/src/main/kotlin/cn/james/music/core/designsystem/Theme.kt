package cn.james.music.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    System,
    Light,
    Dark,
    Amoled,
}

@Immutable
data class MoeKoeExtraColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val accentPurple: Color,
    val accentMint: Color,
    val accentPink: Color,
    val vipGold: Color,
)

@Immutable
data class MoeKoeSpacing(
    val space4: Dp = 4.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space16: Dp = 16.dp,
    val space20: Dp = 20.dp,
    val space24: Dp = 24.dp,
    val space32: Dp = 32.dp,
    val space40: Dp = 40.dp,
) {
    val extraSmall: Dp get() = space4
    val small: Dp get() = space8
    val compact: Dp get() = space12
    val medium: Dp get() = space16
    val mediumLarge: Dp get() = space20
    val large: Dp get() = space24
    val extraLarge: Dp get() = space32
    val huge: Dp get() = space40
}

@Immutable
data class MoeKoeDimensions(
    val iconSmall: Dp = 16.dp,
    val iconSupporting: Dp = 20.dp,
    val iconStandard: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val minimumTouchTarget: Dp = 48.dp,
    val immersiveActionContainer: Dp = 40.dp,
    val buttonHeight: Dp = 48.dp,
    val largeButtonHeight: Dp = 56.dp,
    val inputHeight: Dp = 56.dp,
    val toolbarHeight: Dp = 64.dp,
    val miniPlayerHeight: Dp = 72.dp,
    val bottomNavigationHeight: Dp = 80.dp,
)

internal val LightColors =
    lightColorScheme(
        primary = Color(0xFF1677F2),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8F2FF),
        onPrimaryContainer = Color(0xFF0B3B79),
        secondary = Color(0xFF596579),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDEE8F7),
        onSecondaryContainer = Color(0xFF172234),
        tertiary = Color(0xFF7557E8),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFEDE7FF),
        onTertiaryContainer = Color(0xFF2B176C),
        error = Color(0xFFD9304F),
        onError = Color.White,
        errorContainer = Color(0xFFFFE8ED),
        onErrorContainer = Color(0xFF6D0A22),
        background = Color(0xFFFBFCFE),
        onBackground = Color(0xFF1A1C20),
        surface = Color.White,
        onSurface = Color(0xFF1A1C20),
        surfaceVariant = Color(0xFFF5F8FC),
        onSurfaceVariant = Color(0xFF6E7580),
        outline = Color(0xFFBFC7D2),
        outlineVariant = Color(0xFFE1E7EF),
        surfaceDim = Color(0xFFE8EDF4),
        surfaceBright = Color.White,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF9FBFD),
        surfaceContainer = Color(0xFFF5F8FC),
        surfaceContainerHigh = Color(0xFFEEF3F8),
        surfaceContainerHighest = Color(0xFFE8EDF4),
    )

internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8EC4FF),
        onPrimary = Color(0xFF003258),
        primaryContainer = Color(0xFF154F8E),
        onPrimaryContainer = Color(0xFFD6E8FF),
        secondary = Color(0xFFBBC7D9),
        onSecondary = Color(0xFF263141),
        secondaryContainer = Color(0xFF3D485A),
        onSecondaryContainer = Color(0xFFD7E3F6),
        tertiary = Color(0xFFCBB8FF),
        onTertiary = Color(0xFF3D258A),
        tertiaryContainer = Color(0xFF523BA0),
        onTertiaryContainer = Color(0xFFE9E0FF),
        error = Color(0xFFFFB2BE),
        onError = Color(0xFF67001E),
        errorContainer = Color(0xFF8E1534),
        onErrorContainer = Color(0xFFFFD9E0),
        background = Color(0xFF0F1218),
        onBackground = Color(0xFFE6E9EF),
        surface = Color(0xFF1A1F29),
        onSurface = Color(0xFFE6E9EF),
        surfaceVariant = Color(0xFF212836),
        onSurfaceVariant = Color(0xFFAAB2BE),
        outline = Color(0xFF707987),
        outlineVariant = Color(0xFF39414D),
        surfaceDim = Color(0xFF0F1218),
        surfaceBright = Color(0xFF303743),
        surfaceContainerLowest = Color(0xFF0A0D12),
        surfaceContainerLow = Color(0xFF151A22),
        surfaceContainer = Color(0xFF212836),
        surfaceContainerHigh = Color(0xFF29313E),
        surfaceContainerHighest = Color(0xFF333B48),
    )

internal val AmoledColors =
    DarkColors.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF1A1A1A),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF050505),
        surfaceContainer = Color(0xFF111111),
        surfaceContainerHigh = Color(0xFF171717),
        surfaceContainerHighest = Color(0xFF202020),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFA3A3A3),
        outlineVariant = Color(0xFF333333),
    )

internal val LightExtraColors =
    MoeKoeExtraColors(
        success = Color(0xFF0B9B66),
        onSuccess = Color.White,
        successContainer = Color(0xFFDDF8EB),
        onSuccessContainer = Color(0xFF003824),
        warning = Color(0xFFB77800),
        onWarning = Color.White,
        warningContainer = Color(0xFFFFF0CD),
        onWarningContainer = Color(0xFF3D2800),
        accentPurple = Color(0xFF7557E8),
        accentMint = Color(0xFF10AE74),
        accentPink = Color(0xFFF14465),
        vipGold = Color(0xFFF2B52A),
    )

private val DarkExtraColors =
    MoeKoeExtraColors(
        success = Color(0xFF9DD7AE),
        onSuccess = Color(0xFF00391B),
        successContainer = Color(0xFF005229),
        onSuccessContainer = Color(0xFFB9F3C9),
        warning = Color(0xFFFFB951),
        onWarning = Color(0xFF442B00),
        warningContainer = Color(0xFF614000),
        onWarningContainer = Color(0xFFFFDDAE),
        accentPurple = Color(0xFFCBB8FF),
        accentMint = Color(0xFF69E5B4),
        accentPink = Color(0xFFFFB2C1),
        vipGold = Color(0xFFFFD36A),
    )

private val LocalSpacing = staticCompositionLocalOf { MoeKoeSpacing() }
private val LocalDimensions = staticCompositionLocalOf { MoeKoeDimensions() }
private val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }
private val LocalExtraShapes = staticCompositionLocalOf { MoeKoeExtraShapes() }

object MoeKoeTheme {
    val spacing: MoeKoeSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val extraColors: MoeKoeExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current

    val dimensions: MoeKoeDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current

    val extraShapes: MoeKoeExtraShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraShapes.current

    @Composable
    operator fun invoke(
        themeMode: ThemeMode = ThemeMode.System,
        dynamicColor: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val systemDark = isSystemInDarkTheme()
        val darkTheme =
            when (themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark, ThemeMode.Amoled -> true
            }
        val colorScheme =
            when {
                themeMode == ThemeMode.Amoled -> {
                    AmoledColors
                }

                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }

                darkTheme -> {
                    DarkColors
                }

                else -> {
                    LightColors
                }
            }

        CompositionLocalProvider(
            LocalSpacing provides MoeKoeSpacing(),
            LocalDimensions provides MoeKoeDimensions(),
            LocalExtraColors provides if (darkTheme) DarkExtraColors else LightExtraColors,
            LocalExtraShapes provides MoeKoeExtraShapes(),
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = MoeKoeTypography,
                shapes = MoeKoeShapes,
                content = content,
            )
        }
    }
}
