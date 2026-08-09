package cn.james.music.feature.settings

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Light", widthDp = 390, heightDp = 844)
@Composable
fun SettingsLightScreenshot() = SettingsScreenshotContent(ThemeMode.Light, SettingsThemeUi.System, autoSkipFailedPlayback = true)

@PreviewTest
@Preview(name = "LayoutProbe", widthDp = 390, heightDp = 844)
@Composable
fun SettingsLayoutProbeScreenshot() =
    CompositionLocalProvider(
        LocalSettingsLayoutProbeColors provides
            mapOf(
                SETTINGS_PROBE_TOOLBAR to Color.Magenta,
                SETTINGS_PROBE_APPEARANCE to Color.Cyan,
                SETTINGS_PROBE_PLAYBACK to Color.Green,
                SETTINGS_PROBE_LYRICS to Color.Blue,
                SETTINGS_PROBE_STORAGE to Color.Red,
            ),
    ) {
        SettingsScreenshotContent(ThemeMode.Light, SettingsThemeUi.System, autoSkipFailedPlayback = true)
    }

@PreviewTest
@Preview(name = "Dark", widthDp = 390, heightDp = 844)
@Composable
fun SettingsDarkScreenshot() = SettingsScreenshotContent(ThemeMode.Dark, SettingsThemeUi.Dark, autoSkipFailedPlayback = true)

@PreviewTest
@Preview(name = "Amoled", widthDp = 390, heightDp = 844)
@Composable
fun SettingsAmoledScreenshot() = SettingsScreenshotContent(ThemeMode.Amoled, SettingsThemeUi.Amoled, autoSkipFailedPlayback = true)

@PreviewTest
@Preview(name = "LargeText15", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun SettingsLargeText15Screenshot() = SettingsScreenshotContent(ThemeMode.Light, SettingsThemeUi.System, autoSkipFailedPlayback = true)

@PreviewTest
@Preview(name = "LargeText20", widthDp = 390, heightDp = 844, fontScale = 2.0f)
@Composable
fun SettingsLargeText20Screenshot() = SettingsScreenshotContent(ThemeMode.Light, SettingsThemeUi.System, autoSkipFailedPlayback = true)

@PreviewTest
@Preview(name = "LongContent", widthDp = 390, heightDp = 1040)
@Composable
fun SettingsLongContentScreenshot() = SettingsScreenshotContent(ThemeMode.Light, SettingsThemeUi.System, autoSkipFailedPlayback = true)

@PreviewTest
@Preview(name = "LyricsTextSizeDialog", widthDp = 390, heightDp = 844)
@Composable
fun SettingsLyricsTextSizeDialogScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Surface {
            SettingsScreen(
                state =
                    SettingsUiState(
                        overlay = SettingsOverlay.LyricsTextSizeSelection,
                        groups = settingsGroups(SettingsThemeUi.System, autoSkipFailedPlayback = true),
                    ),
                onAction = {},
            )
        }
    }
}

@Composable
private fun SettingsScreenshotContent(
    themeMode: ThemeMode,
    preference: SettingsThemeUi,
    autoSkipFailedPlayback: Boolean,
) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface {
            SettingsScreen(
                state =
                    SettingsUiState(
                        theme = preference,
                        autoSkipFailedPlayback = autoSkipFailedPlayback,
                        groups = settingsGroups(preference, autoSkipFailedPlayback),
                    ),
                onAction = {},
            )
        }
    }
}
