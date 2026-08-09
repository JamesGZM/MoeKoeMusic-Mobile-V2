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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
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
    dismissPolicy: MoeDialogDismissPolicy = MoeDialogDismissPolicy.BackAndOutside,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                dismissOnBackPress = dismissPolicy.dismissOnBackPress,
                dismissOnClickOutside = dismissPolicy.dismissOnClickOutside,
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
    model: MoeAlertDialogUiModel,
    onEvent: (MoeAlertDialogEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    MoeDialog(
        onDismissRequest = { onEvent(MoeAlertDialogEvent.Dismiss) },
        modifier = modifier,
        dismissPolicy = model.actions.confirmState.alertDismissPolicy,
    ) {
        MoeAlertDialogContent(
            model = model,
            onEvent = onEvent,
        )
    }
}

@Composable
fun MoeDialogActions(
    model: MoeDialogActionsUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loading = model.confirmState == MoeDialogConfirmState.Loading
    val confirmEnabled = model.confirmState == MoeDialogConfirmState.Enabled
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space12),
    ) {
        MoeTonalButton(
            onClick = onDismiss,
            enabled = !loading,
            modifier = Modifier.weight(1f),
        ) {
            DialogActionLabel(model.dismissLabel)
        }
        if (model.tone == MoeAlertDialogTone.Destructive) {
            MoeDestructiveButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                loading = loading,
                modifier = Modifier.weight(1f),
            ) {
                DialogActionLabel(model.confirmLabel)
            }
        } else {
            MoeButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                loading = loading,
                modifier = Modifier.weight(1f),
            ) {
                DialogActionLabel(model.confirmLabel)
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
    model: MoeAlertDialogUiModel,
    onEvent: (MoeAlertDialogEvent) -> Unit,
) {
    if (model.icon != MoeAlertDialogIcon.None) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = MoeKoeTheme.spacing.space16),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides
                    if (model.actions.tone == MoeAlertDialogTone.Destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            ) {
                when (model.icon) {
                    MoeAlertDialogIcon.None -> Unit
                    MoeAlertDialogIcon.Account -> Icon(Icons.Default.AccountCircle, contentDescription = null)
                    MoeAlertDialogIcon.Warning -> Icon(Icons.Outlined.Warning, contentDescription = null)
                }
            }
        }
    }
    Text(
        text = model.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = model.message,
        modifier = Modifier.padding(top = MoeKoeTheme.spacing.space16),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
    MoeDialogActions(
        model = model.actions,
        onConfirm = { onEvent(MoeAlertDialogEvent.Confirm) },
        onDismiss = { onEvent(MoeAlertDialogEvent.Dismiss) },
        modifier = Modifier.padding(top = MoeKoeTheme.spacing.space24),
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

private val MoeDialogDismissPolicy.dismissOnBackPress: Boolean
    get() = this == MoeDialogDismissPolicy.BackAndOutside || this == MoeDialogDismissPolicy.BackOnly

private val MoeDialogDismissPolicy.dismissOnClickOutside: Boolean
    get() = this == MoeDialogDismissPolicy.BackAndOutside || this == MoeDialogDismissPolicy.OutsideOnly

private val MoeDialogConfirmState.alertDismissPolicy: MoeDialogDismissPolicy
    get() =
        if (this == MoeDialogConfirmState.Loading) {
            MoeDialogDismissPolicy.Locked
        } else {
            MoeDialogDismissPolicy.BackAndOutside
        }

private const val MOE_DIALOG_DIM_AMOUNT = 0.32f
