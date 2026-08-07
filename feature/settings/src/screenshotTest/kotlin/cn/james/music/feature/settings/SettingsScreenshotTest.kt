package cn.james.music.feature.settings

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.settings.AppThemePreference
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Light", widthDp = 390, heightDp = 844)
@Composable
fun SettingsLightScreenshot() = SettingsScreenshotContent(ThemeMode.Light, AppThemePreference.System)

@PreviewTest
@Preview(name = "Dark", widthDp = 390, heightDp = 844)
@Composable
fun SettingsDarkScreenshot() = SettingsScreenshotContent(ThemeMode.Dark, AppThemePreference.Dark)

@PreviewTest
@Preview(name = "Amoled", widthDp = 390, heightDp = 844)
@Composable
fun SettingsAmoledScreenshot() = SettingsScreenshotContent(ThemeMode.Amoled, AppThemePreference.Amoled)

@PreviewTest
@Preview(name = "LargeText15", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun SettingsLargeText15Screenshot() = SettingsScreenshotContent(ThemeMode.Light, AppThemePreference.System)

@PreviewTest
@Preview(name = "LargeText20", widthDp = 390, heightDp = 844, fontScale = 2.0f)
@Composable
fun SettingsLargeText20Screenshot() = SettingsScreenshotContent(ThemeMode.Light, AppThemePreference.System)

@PreviewTest
@Preview(name = "LongContent", widthDp = 390, heightDp = 1040)
@Composable
fun SettingsLongContentScreenshot() = SettingsScreenshotContent(ThemeMode.Light, AppThemePreference.System)

@Composable
private fun SettingsScreenshotContent(
    themeMode: ThemeMode,
    preference: AppThemePreference,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
            SettingsScreen(
                state = SettingsUiState(theme = preference),
                onBack = {},
                onThemeSelected = {},
                onRetry = {},
                onDismissProblem = {},
            )
        }
    }
}
