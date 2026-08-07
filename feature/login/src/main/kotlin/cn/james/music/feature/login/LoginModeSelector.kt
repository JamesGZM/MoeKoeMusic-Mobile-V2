package cn.james.music.feature.login

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

internal const val LOGIN_MODE_INDICATOR_TAG = "login_mode_indicator"

@Composable
internal fun LoginModeSelector(
    layout: LoginLayoutSpec,
    selectedMode: LoginMode,
    onModeChange: (LoginMode) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val modes = listOf(LoginMode.MobileCode, LoginMode.Password, LoginMode.QrCode)
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)
    val selectorContainerColor =
        if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
            Color(0xFFF6F6F8)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    Surface(
        modifier = modifier,
        color = selectorContainerColor,
        shape = RoundedCornerShape(layout.form.selectorCornerRadius),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(layout.form.selectorHeight)
                .selectableGroup(),
        ) {
            val itemWidth =
                (maxWidth - layout.form.selectorPadding * 2 - layout.form.selectorItemGap * 2) / modes.size
            val indicatorOffset =
                layout.form.selectorPadding +
                    (itemWidth + layout.form.selectorItemGap) * selectedIndex
            val animatedIndicatorOffset =
                androidx.compose.animation.core.animateDpAsState(
                    targetValue = indicatorOffset,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "login mode indicator offset",
                )
            val indicatorShape = RoundedCornerShape(layout.form.selectorItemCornerRadius)
            Surface(
                modifier =
                    Modifier
                        .offset(x = animatedIndicatorOffset.value, y = layout.form.selectorPadding)
                        .width(itemWidth)
                        .height(layout.form.selectorHeight - layout.form.selectorPadding * 2)
                        .shadow(
                            elevation = layout.form.selectorIndicatorShadowElevation,
                            shape = indicatorShape,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        )
                        .testTag(LOGIN_MODE_INDICATOR_TAG),
                shape = indicatorShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(layout.dp(1f), MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                shadowElevation = layout.dp(0f),
            ) {}
            Row(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(layout.form.selectorPadding),
                horizontalArrangement = Arrangement.spacedBy(layout.form.selectorItemGap),
            ) {
                LoginModeItem(
                    layout = layout,
                    label = stringResource(R.string.login_mode_code),
                    icon = { Icon(painterResource(R.drawable.ic_login_mode_code), contentDescription = null) },
                    selected = selectedMode == LoginMode.MobileCode,
                    enabled = enabled,
                    onClick = { onModeChange(LoginMode.MobileCode) },
                    modifier = Modifier.weight(1f),
                )
                LoginModeItem(
                    layout = layout,
                    label = stringResource(R.string.login_mode_password),
                    icon = { Icon(painterResource(R.drawable.ic_login_mode_password), contentDescription = null) },
                    selected = selectedMode == LoginMode.Password,
                    enabled = enabled,
                    onClick = { onModeChange(LoginMode.Password) },
                    modifier = Modifier.weight(1f),
                )
                LoginModeItem(
                    layout = layout,
                    label = stringResource(R.string.login_mode_qr),
                    icon = { Icon(painterResource(R.drawable.ic_login_mode_qr), contentDescription = null) },
                    selected = selectedMode == LoginMode.QrCode,
                    enabled = enabled,
                    onClick = { onModeChange(LoginMode.QrCode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LoginModeItem(
    layout: LoginLayoutSpec,
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Tab,
                    onClick = { if (!selected) onClick() },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val targetContentColor =
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                }
            val contentColor =
                animateColorAsState(
                    targetValue = targetContentColor,
                    animationSpec = tween(durationMillis = 180),
                    label = "login mode content color",
                )
            CompositionLocalProvider(LocalContentColor provides contentColor.value) {
                Box(Modifier.size(layout.form.selectorIconSize), contentAlignment = Alignment.Center) { icon() }
                Spacer(Modifier.width(layout.form.selectorLabelGap))
                Text(
                    label,
                    style =
                        TextStyle(
                            fontSize = layout.typography.modeLabelSize,
                            lineHeight = layout.typography.modeLabelLineHeight,
                            fontWeight = FontWeight.Medium,
                        ),
                    maxLines = 1,
                )
            }
        }
    }
}
