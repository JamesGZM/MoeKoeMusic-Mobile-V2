package cn.james.music.features.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode

@Composable
fun DesignSystemShowcaseScreen(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(MoeKoeTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.large),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                    Text(
                        text = "MoeKoe Music",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("showcase-title"),
                    )
                    Text(
                        text = "工程基础阶段 · Design System Showcase",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                ShowcaseSection(title = "主题") {
                    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                        ThemeMode.entries.chunked(2).forEach { rowModes ->
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(MoeKoeTheme.spacing.small),
                            ) {
                                rowModes.forEach { mode ->
                                    FilterChip(
                                        selected = selectedTheme == mode,
                                        onClick = { onThemeSelected(mode) },
                                        label = { Text(mode.displayName) },
                                        modifier =
                                            Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("theme-${mode.name}"),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ShowcaseSection(title = "品牌与语义色") {
                    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                        ColorSample(
                            label = "Primary",
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        ColorSample(
                            label = "Success",
                            container = MoeKoeTheme.extraColors.successContainer,
                            content = MoeKoeTheme.extraColors.onSuccessContainer,
                        )
                        ColorSample(
                            label = "Warning",
                            container = MoeKoeTheme.extraColors.warningContainer,
                            content = MoeKoeTheme.extraColors.onWarningContainer,
                        )
                        ColorSample(
                            label = "Error",
                            container = MaterialTheme.colorScheme.errorContainer,
                            content = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            item {
                ShowcaseSection(title = "排版与操作") {
                    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                        Text("标题层级", style = MaterialTheme.typography.titleLarge)
                        Text("正文保持真实手机上的清晰可读性。", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "说明文字原则上不低于 14sp。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {},
                            modifier = Modifier.heightIn(min = 48.dp).testTag("primary-action"),
                        ) {
                            Text("主要操作")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowcaseSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MoeKoeTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.medium),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ColorSample(
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(container, MaterialTheme.shapes.medium)
                .padding(MoeKoeTheme.spacing.medium),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, color = content, style = MaterialTheme.typography.bodyLarge)
    }
}

private val ThemeMode.displayName: String
    get() =
        when (this) {
            ThemeMode.System -> "跟随系统"
            ThemeMode.Light -> "浅色"
            ThemeMode.Dark -> "深色"
            ThemeMode.Amoled -> "纯黑"
        }

@Preview(name = "Light", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun DesignSystemShowcaseLightPreview() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        DesignSystemShowcaseScreen(selectedTheme = ThemeMode.Light, onThemeSelected = {})
    }
}

@Preview(name = "Dark", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun DesignSystemShowcaseDarkPreview() {
    MoeKoeTheme(themeMode = ThemeMode.Dark) {
        DesignSystemShowcaseScreen(selectedTheme = ThemeMode.Dark, onThemeSelected = {})
    }
}

@Preview(name = "Amoled", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun DesignSystemShowcaseAmoledPreview() {
    MoeKoeTheme(themeMode = ThemeMode.Amoled) {
        DesignSystemShowcaseScreen(selectedTheme = ThemeMode.Amoled, onThemeSelected = {})
    }
}
