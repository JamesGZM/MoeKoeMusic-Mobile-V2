package cn.james.music

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.feature.foundation.FoundationDestination
import cn.james.music.feature.foundation.FoundationShowcaseRoute
import cn.james.music.feature.foundation.foundationDestination

internal fun foundationContent(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
): (@Composable () -> Unit) =
    {
        FoundationShowcaseRoute(selectedTheme, onThemeSelected)
    }

internal fun NavGraphBuilder.addFoundationDestination(content: @Composable () -> Unit) {
    foundationDestination(content)
}

internal fun NavHostController.navigateToFoundation() {
    navigate(FoundationDestination)
}
