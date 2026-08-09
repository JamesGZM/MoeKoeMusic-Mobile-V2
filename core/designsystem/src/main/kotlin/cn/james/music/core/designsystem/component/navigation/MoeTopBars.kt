package cn.james.music.core.designsystem.component.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdateAlt
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoePassiveOutline
import cn.james.music.core.designsystem.component.input.MoeSearchField

@Composable
fun MoeSearchTopBar(
    model: MoeSearchTopBarUiModel,
    onEvent: (MoeSearchTopBarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onEvent(MoeSearchTopBarEvent.NavigateBack) },
            modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget).offset { IntOffset(0, 4.dp.roundToPx()) },
        ) {
            MoeNavigateBackIcon(
                contentDescription = model.navigationContentDescription,
                modifier = Modifier.size(18.dp),
            )
        }
        MoeSearchField(
            value = model.query,
            placeholder = model.placeholder,
            onValueChange = { onEvent(MoeSearchTopBarEvent.QueryChanged(it)) },
            modifier = Modifier.weight(1f).offset { IntOffset(0, 4.dp.roundToPx()) },
            clearContentDescription = model.clearContentDescription,
            onSearch = { onEvent(MoeSearchTopBarEvent.Submit) },
        )
        Row(Modifier.offset { IntOffset(0, 4.dp.roundToPx()) }) {
            model.actions.forEach { action ->
                MoeSearchTopBarAction(action = action, onEvent = onEvent)
            }
        }
    }
}

enum class MoeTopBarTitleEmphasis {
    Standard,
    Strong,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoeStandardTopBar(
    model: MoeStandardTopBarUiModel,
    onEvent: (MoeStandardTopBarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleStyle: TextStyle =
        when (model.titleEmphasis) {
            MoeTopBarTitleEmphasis.Standard ->
                MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            MoeTopBarTitleEmphasis.Strong ->
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
        }
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = model.title,
                modifier = Modifier.offset(y = (-4).dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = titleStyle,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { onEvent(MoeStandardTopBarEvent.NavigateBack) },
                modifier =
                    Modifier
                        .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                        .offset(y = (-2).dp),
            ) {
                MoeStandardTopBarNavigation(
                    navigation = model.navigation,
                    contentDescription = model.navigationContentDescription,
                )
            }
        },
        actions = {
            model.actions.forEachIndexed { index, action ->
                MoeStandardTopBarAction(
                    action = action,
                    hasLeadingAction = model.actions.size == 2 && index == 0,
                    onEvent = onEvent,
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            ),
        modifier = modifier,
    )
}

@Composable
private fun MoeStandardTopBarAction(
    action: MoeTopBarActionUiModel,
    hasLeadingAction: Boolean,
    onEvent: (MoeStandardTopBarEvent) -> Unit,
) {
    IconButton(
        onClick = { onEvent(MoeStandardTopBarEvent.Action(action.action)) },
        modifier =
            Modifier
                .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                .offset(y = (-3).dp),
    ) {
        Icon(
            imageVector = action.action.imageVector(),
            contentDescription = action.contentDescription,
            modifier =
                Modifier
                    .size(MoeKoeTheme.dimensions.iconSupporting)
                    .offset(x = if (hasLeadingAction) 8.dp else 0.dp),
        )
    }
}

@Composable
private fun MoeSearchTopBarAction(
    action: MoeTopBarActionUiModel,
    onEvent: (MoeSearchTopBarEvent) -> Unit,
) {
    IconButton(onClick = { onEvent(MoeSearchTopBarEvent.Action(action.action)) }) {
        Icon(
            imageVector = action.action.imageVector(),
            contentDescription = action.contentDescription,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MoeStandardTopBarNavigation(
    navigation: MoeTopBarNavigation,
    contentDescription: String,
) {
    when (navigation) {
        MoeTopBarNavigation.Back ->
            MoeNavigateBackIcon(
                contentDescription = contentDescription,
                modifier = Modifier.size(MoeKoeTheme.dimensions.iconSmall),
            )

        MoeTopBarNavigation.CaptchaClose ->
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(MoeKoeTheme.dimensions.iconSupporting),
                    )
                }
            }
    }
}

private fun MoeTopBarAction.imageVector(): ImageVector =
    when (this) {
        MoeTopBarAction.Import -> Icons.Default.SystemUpdateAlt
        MoeTopBarAction.Search -> Icons.Default.Search
        MoeTopBarAction.Share -> Icons.Default.Share
        MoeTopBarAction.More -> Icons.Default.MoreVert
        MoeTopBarAction.Voice -> Icons.Default.Mic
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
