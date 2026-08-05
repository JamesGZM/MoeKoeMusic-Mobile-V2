package cn.james.music.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoeKoeStandardTopBar(
    title: String,
    navigationContentDescription: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = navigationContentDescription,
                    modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard),
                )
            }
        },
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            ),
        modifier = modifier,
    )
}

@Composable
fun MoeKoeImmersiveTopBar(
    navigationContentDescription: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    foregroundColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(MoeKoeTheme.dimensions.toolbarHeight)
                .padding(horizontal = MoeKoeTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoeKoeImmersiveIconButton(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = navigationContentDescription,
            onClick = onNavigateBack,
            foregroundColor = foregroundColor,
            containerColor = containerColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun MoeKoeImmersiveIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    foregroundColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
) {
    Box(
        modifier = modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(MoeKoeTheme.dimensions.immersiveActionContainer),
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
            shadowElevation = 2.dp,
        ) {}
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget),
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = foregroundColor,
                modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard),
            )
        }
    }
}
