package cn.james.music

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import cn.james.music.core.designsystem.ThemeMode

@Suppress("UNUSED_PARAMETER")
internal fun foundationContent(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
): (@Composable () -> Unit)? = null

@Suppress("UNUSED_PARAMETER")
internal fun NavGraphBuilder.addFoundationDestination(content: @Composable () -> Unit) = Unit

internal fun NavHostController.navigateToFoundation() = Unit
