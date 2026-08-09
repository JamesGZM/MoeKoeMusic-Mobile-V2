package cn.james.music.core.designsystem.component.overlay

import androidx.compose.runtime.Immutable

@Immutable
enum class MoeDialogDismissPolicy {
    BackAndOutside,
    BackOnly,
    OutsideOnly,
    Locked,
}

@Immutable
enum class MoeAlertDialogIcon {
    None,
    Account,
    Warning,
}

@Immutable
enum class MoeAlertDialogTone {
    Standard,
    Destructive,
}

@Immutable
enum class MoeDialogConfirmState {
    Enabled,
    Disabled,
    Loading,
}

@Immutable
data class MoeDialogActionsUiModel(
    val confirmLabel: String,
    val dismissLabel: String,
    val confirmState: MoeDialogConfirmState = MoeDialogConfirmState.Enabled,
    val tone: MoeAlertDialogTone = MoeAlertDialogTone.Standard,
)

@Immutable
data class MoeAlertDialogUiModel(
    val title: String,
    val message: String,
    val icon: MoeAlertDialogIcon = MoeAlertDialogIcon.None,
    val actions: MoeDialogActionsUiModel,
)

sealed interface MoeAlertDialogEvent {
    data object Confirm : MoeAlertDialogEvent

    data object Dismiss : MoeAlertDialogEvent
}
