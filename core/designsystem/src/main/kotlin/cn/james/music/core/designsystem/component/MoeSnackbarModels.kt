package cn.james.music.core.designsystem.component

import androidx.compose.runtime.Immutable

@Immutable
enum class MoeSnackbarTone {
    Success,
    Info,
    Error,
    Warning,
}

@Immutable
enum class MoeSnackbarIcon {
    ToneDefault,
    Confirmation,
    Removal,
    Warning,
    Information,
}

@Immutable
enum class MoeSnackbarActionId {
    Retry,
    Dismiss,
    Undo,
}

@Immutable
data class MoeSnackbarActionUiModel(
    val id: MoeSnackbarActionId,
    val label: String,
)

@Immutable
data class MoeSnackbarUiModel(
    val message: String,
    val tone: MoeSnackbarTone = MoeSnackbarTone.Info,
    val icon: MoeSnackbarIcon = MoeSnackbarIcon.ToneDefault,
    val action: MoeSnackbarActionUiModel? = null,
)

sealed interface MoeSnackbarEvent {
    data class Action(
        val id: MoeSnackbarActionId,
    ) : MoeSnackbarEvent
}
