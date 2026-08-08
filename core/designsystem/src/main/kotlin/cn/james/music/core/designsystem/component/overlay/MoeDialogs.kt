package cn.james.music.core.designsystem.component.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.action.MoeButton
import cn.james.music.core.designsystem.component.action.MoeDestructiveButton
import cn.james.music.core.designsystem.component.action.MoeTonalButton

@Immutable
enum class MoeDialogSize {
    Confirm,
    Input,
}

@Composable
fun MoeDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    size: MoeDialogSize = MoeDialogSize.Confirm,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                dismissOnClickOutside = dismissOnClickOutside,
                usePlatformDefaultWidth = false,
            ),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.setDimAmount(MOE_DIALOG_DIM_AMOUNT)
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MoeKoeTheme.spacing.space32),
            contentAlignment = Alignment.Center,
        ) {
            MoeDialogSurface(modifier = modifier, size = size, content = content)
        }
    }
}

@Composable
fun MoeAlertDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false,
    dismissOnBackPress: Boolean = !loading,
    dismissOnClickOutside: Boolean = !loading,
) {
    MoeDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
    ) {
        MoeAlertDialogContent(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            dismissLabel = dismissLabel,
            onConfirm = onConfirm,
            onDismiss = onDismissRequest,
            icon = icon,
            confirmEnabled = confirmEnabled,
            loading = loading,
            destructive = destructive,
        )
    }
}

@Composable
fun MoeDialogActions(
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space12),
    ) {
        MoeTonalButton(
            onClick = onDismiss,
            enabled = !loading,
            modifier = Modifier.weight(1f),
        ) {
            DialogActionLabel(dismissLabel)
        }
        if (destructive) {
            MoeDestructiveButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                loading = loading,
                modifier = Modifier.weight(1f),
            ) {
                DialogActionLabel(confirmLabel)
            }
        } else {
            MoeButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                loading = loading,
                modifier = Modifier.weight(1f),
            ) {
                DialogActionLabel(confirmLabel)
            }
        }
    }
}

@Composable
internal fun MoeDialogSurface(
    size: MoeDialogSize,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.widthIn(max = size.maximumWidth).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(MoeKoeTheme.spacing.space24),
            content = content,
        )
    }
}

@Composable
internal fun MoeAlertDialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false,
) {
    icon?.let {
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = MoeKoeTheme.spacing.space16),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides
                    if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            ) {
                it()
            }
        }
    }
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = message,
        modifier = Modifier.padding(top = MoeKoeTheme.spacing.space16),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
    MoeDialogActions(
        confirmLabel = confirmLabel,
        dismissLabel = dismissLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = Modifier.padding(top = MoeKoeTheme.spacing.space24),
        confirmEnabled = confirmEnabled,
        loading = loading,
        destructive = destructive,
    )
}

@Composable
private fun RowScope.DialogActionLabel(label: String) {
    Text(text = label, maxLines = 1)
}

private val MoeDialogSize.maximumWidth: Dp
    get() =
        when (this) {
            MoeDialogSize.Confirm -> 304.dp
            MoeDialogSize.Input -> 320.dp
        }

private const val MOE_DIALOG_DIM_AMOUNT = 0.32f
