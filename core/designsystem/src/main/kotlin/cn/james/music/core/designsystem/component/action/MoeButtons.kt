package cn.james.music.core.designsystem.component.action

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import cn.james.music.core.designsystem.MoeKoeTheme

@Immutable
enum class MoeButtonSize {
    Regular,
    Large,
}

@Composable
fun MoeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: MoeButtonSize = MoeButtonSize.Regular,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.moeButtonSize(size),
        enabled = enabled && !loading,
        shape = size.shape,
        colors =
            ButtonDefaults.buttonColors(
                disabledContainerColor =
                    if (loading) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    },
                disabledContentColor =
                    if (loading) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                    },
            ),
        contentPadding = contentPadding,
    ) {
        MoeButtonContent(loading = loading, size = size, content = content)
    }
}

@Composable
fun MoeTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: MoeButtonSize = MoeButtonSize.Regular,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.moeButtonSize(size),
        enabled = enabled && !loading,
        shape = size.shape,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor =
                    if (loading) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                disabledContentColor =
                    if (loading) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
    ) {
        MoeButtonContent(loading = loading, size = size, content = content)
    }
}

@Composable
fun MoeOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: MoeButtonSize = MoeButtonSize.Regular,
    content: @Composable RowScope.() -> Unit,
) {
    val interactive = enabled && !loading
    val outlineColor =
        if (interactive || loading) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.moeButtonSize(size),
        enabled = interactive,
        shape = size.shape,
        border = BorderStroke(1.dp, outlineColor),
        colors =
            ButtonDefaults.outlinedButtonColors(
                disabledContentColor =
                    if (loading) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
    ) {
        MoeButtonContent(loading = loading, size = size, content = content)
    }
}

@Composable
fun MoeTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: MoeButtonSize = MoeButtonSize.Regular,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.moeButtonSize(size),
        enabled = enabled && !loading,
        shape = size.shape,
        colors =
            ButtonDefaults.textButtonColors(
                disabledContentColor =
                    if (loading) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
    ) {
        MoeButtonContent(loading = loading, size = size, content = content)
    }
}

@Composable
fun MoeDestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: MoeButtonSize = MoeButtonSize.Regular,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.moeButtonSize(size),
        enabled = enabled && !loading,
        shape = size.shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                disabledContainerColor =
                    if (loading) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                disabledContentColor =
                    if (loading) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
            ),
    ) {
        MoeButtonContent(loading = loading, size = size, content = content)
    }
}

@Composable
private fun RowScope.MoeButtonContent(
    loading: Boolean,
    size: MoeButtonSize,
    content: @Composable RowScope.() -> Unit,
) {
    if (loading) {
        val transition = rememberInfiniteTransition(label = "MoeButtonLoading")
        val rotation by
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
                label = "MoeButtonLoadingRotation",
            )
        CircularProgressIndicator(
            progress = { 0.72f },
            modifier = Modifier.size(size.progressSize).rotate(rotation).clearAndSetSemantics {},
            color = LocalContentColor.current,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(MoeKoeTheme.spacing.space8))
    }
    content()
}

@Composable
private fun Modifier.moeButtonSize(size: MoeButtonSize): Modifier =
    defaultMinSize(minHeight = size.minimumHeight)

private val MoeButtonSize.minimumHeight: Dp
    @Composable
    get() =
        when (this) {
            MoeButtonSize.Regular -> MoeKoeTheme.dimensions.buttonHeight
            MoeButtonSize.Large -> MoeKoeTheme.dimensions.largeButtonHeight
        }

private val MoeButtonSize.progressSize: Dp
    get() =
        when (this) {
            MoeButtonSize.Regular -> 18.dp
            MoeButtonSize.Large -> 20.dp
        }

private val MoeButtonSize.shape
    @Composable
    get() =
        when (this) {
            MoeButtonSize.Regular -> MaterialTheme.shapes.small
            MoeButtonSize.Large -> MaterialTheme.shapes.medium
        }
