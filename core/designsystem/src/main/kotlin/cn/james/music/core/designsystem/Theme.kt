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
)

@Immutable
data class MoeKoeSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF2769C9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD9E6FF),
        onPrimaryContainer = Color(0xFF001A41),
        secondary = Color(0xFF575E71),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE2F9),
        onSecondaryContainer = Color(0xFF141B2C),
        tertiary = Color(0xFF735573),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFED7FB),
        onTertiaryContainer = Color(0xFF2A122D),
        background = Color(0xFFFFFBFF),
        onBackground = Color(0xFF1B1B1F),
        surface = Color(0xFFFFFBFF),
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFE1E2EC),
        onSurfaceVariant = Color(0xFF44464F),
        outline = Color(0xFF74777F),
        outlineVariant = Color(0xFFC4C6D0),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFADC7FF),
        onPrimary = Color(0xFF002F67),
        primaryContainer = Color(0xFF08478F),
        onPrimaryContainer = Color(0xFFD9E6FF),
        secondary = Color(0xFFC0C6DC),
        onSecondary = Color(0xFF293041),
        secondaryContainer = Color(0xFF3F4759),
        onSecondaryContainer = Color(0xFFDCE2F9),
        tertiary = Color(0xFFE1BBDD),
        onTertiary = Color(0xFF412742),
        tertiaryContainer = Color(0xFF593D59),
        onTertiaryContainer = Color(0xFFFED7FB),
        background = Color(0xFF121317),
        onBackground = Color(0xFFE3E2E6),
        surface = Color(0xFF121317),
        onSurface = Color(0xFFE3E2E6),
        surfaceVariant = Color(0xFF44464F),
        onSurfaceVariant = Color(0xFFC4C6D0),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44464F),
    )

private val AmoledColors =
    DarkColors.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF202124),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF08090B),
        surfaceContainer = Color(0xFF0D0E11),
        surfaceContainerHigh = Color(0xFF15161A),
        surfaceContainerHighest = Color(0xFF1D1E22),
    )

private val LightExtraColors =
    MoeKoeExtraColors(
        success = Color(0xFF176B3A),
        onSuccess = Color.White,
        successContainer = Color(0xFFB9F3C9),
        onSuccessContainer = Color(0xFF00210D),
        warning = Color(0xFF805600),
        onWarning = Color.White,
        warningContainer = Color(0xFFFFDDAE),
        onWarningContainer = Color(0xFF291800),
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
    )

private val LocalSpacing = staticCompositionLocalOf { MoeKoeSpacing() }
private val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }

object MoeKoeTheme {
    val spacing: MoeKoeSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val extraColors: MoeKoeExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current

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
            LocalExtraColors provides if (darkTheme) DarkExtraColors else LightExtraColors,
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
