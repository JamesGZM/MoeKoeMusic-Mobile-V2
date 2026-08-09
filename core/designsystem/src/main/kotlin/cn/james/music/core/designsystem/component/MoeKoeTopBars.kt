package cn.james.music.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cn.james.music.core.designsystem.component.navigation.MoeImmersiveIconButton
import cn.james.music.core.designsystem.component.navigation.MoeImmersiveTopBar

@Deprecated("Use MoeImmersiveTopBar from component.navigation")
@Composable
fun MoeKoeImmersiveTopBar(
    navigationContentDescription: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    foregroundColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    actions: @Composable RowScope.() -> Unit = {},
) {
    MoeImmersiveTopBar(
        navigationContentDescription = navigationContentDescription,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
        foregroundColor = foregroundColor,
        containerColor = containerColor,
        actions = actions,
    )
}

@Deprecated("Use MoeImmersiveIconButton from component.navigation")
@Composable
fun MoeKoeImmersiveIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    foregroundColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
) {
    MoeImmersiveIconButton(
        imageVector = imageVector,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        foregroundColor = foregroundColor,
        containerColor = containerColor,
    )
}
