package cn.james.music.core.designsystem.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme

@Composable
fun MoeSearchTopBar(
    query: String,
    placeholder: String,
    navigationContentDescription: String,
    clearContentDescription: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget).offset { IntOffset(0, 4.dp.roundToPx()) },
        ) {
            MoeNavigateBackIcon(contentDescription = navigationContentDescription)
        }
        Surface(
            modifier = Modifier.weight(1f).height(40.dp).offset { IntOffset(0, 4.dp.roundToPx()) },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = clearContentDescription)
                    }
                }
            }
        }
        Box(Modifier.offset { IntOffset(0, 4.dp.roundToPx()) }) { trailingAction?.invoke() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoeStandardTopBar(
    title: String,
    navigationContentDescription: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {
        MoeNavigateBackIcon(
            contentDescription = navigationContentDescription,
            modifier = Modifier.size(MoeKoeTheme.dimensions.iconSmall),
        )
    },
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                modifier = Modifier.offset(y = (-4).dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier =
                    Modifier
                        .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                        .offset(y = (-2).dp),
            ) {
                navigationIcon()
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

/** A standard Toolbar action with the product's fixed visible size and touch target. */
@Composable
fun MoeStandardTopBarAction(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalVisualOffset: Dp = 0.dp,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                .offset(y = (-3).dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier =
                Modifier
                    .size(MoeKoeTheme.dimensions.iconSupporting)
                    .graphicsLayer { translationX = horizontalVisualOffset.toPx() },
        )
    }
}

@Composable
fun MoeImmersiveTopBar(
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
                .padding(horizontal = MoeKoeTheme.spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoeImmersiveNavigateBackButton(
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
fun MoeImmersiveIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    foregroundColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
) {
    MoeImmersiveIconButtonFrame(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = foregroundColor,
            modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard),
        )
    }
}

@Composable
fun MoeImmersiveNavigateBackButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    foregroundColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
) {
    MoeImmersiveIconButtonFrame(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
    ) {
        MoeNavigateBackIcon(
            contentDescription = contentDescription,
            tint = foregroundColor,
            modifier = Modifier.size(MoeKoeTheme.dimensions.iconSupporting),
        )
    }
}

@Composable
private fun MoeImmersiveIconButtonFrame(
    onClick: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    icon: @Composable () -> Unit,
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
            icon()
        }
    }
}
