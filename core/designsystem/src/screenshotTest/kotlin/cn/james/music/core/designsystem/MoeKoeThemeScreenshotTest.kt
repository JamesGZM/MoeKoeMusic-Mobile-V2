package cn.james.music.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Light", widthDp = 390, heightDp = 844)
@Composable
fun MoeKoeLightThemeScreenshot() {
    ThemeScreenshotContent(ThemeMode.Light)
}

@PreviewTest
@Preview(name = "Dark", widthDp = 390, heightDp = 844)
@Composable
fun MoeKoeDarkThemeScreenshot() {
    ThemeScreenshotContent(ThemeMode.Dark)
}

@PreviewTest
@Preview(name = "Amoled", widthDp = 390, heightDp = 844)
@Composable
fun MoeKoeAmoledThemeScreenshot() {
    ThemeScreenshotContent(ThemeMode.Amoled)
}

@Composable
private fun ThemeScreenshotContent(themeMode: ThemeMode) {
    MoeKoeTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.padding(MoeKoeTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.medium),
            ) {
                Text("MoeKoe Music", style = MaterialTheme.typography.headlineLarge)
                Text("Material 3 Design System", style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "清透、清晰、适合真实手机阅读",
                        modifier = Modifier.padding(MoeKoeTheme.spacing.medium),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MoeKoeTheme.extraColors.successContainer,
                ) {
                    Text(
                        text = "成功语义色",
                        modifier = Modifier.padding(MoeKoeTheme.spacing.medium),
                        color = MoeKoeTheme.extraColors.onSuccessContainer,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MoeKoeTheme.extraColors.warningContainer,
                ) {
                    Text(
                        text = "警告语义色",
                        modifier = Modifier.padding(MoeKoeTheme.spacing.medium),
                        color = MoeKoeTheme.extraColors.onWarningContainer,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
