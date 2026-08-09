package cn.james.music.core.designsystem.component.navigation

import androidx.compose.runtime.Immutable

@Immutable
data class MoeStandardTopBarUiModel(
    val title: String,
    val navigationContentDescription: String,
    val titleEmphasis: MoeTopBarTitleEmphasis = MoeTopBarTitleEmphasis.Standard,
    val navigation: MoeTopBarNavigation = MoeTopBarNavigation.Back,
    val actions: List<MoeTopBarActionUiModel> = emptyList(),
)

@Immutable
data class MoeSearchTopBarUiModel(
    val query: String,
    val placeholder: String,
    val navigationContentDescription: String,
    val clearContentDescription: String,
    val actions: List<MoeTopBarActionUiModel> = emptyList(),
)

@Immutable
data class MoeTopBarActionUiModel(
    val action: MoeTopBarAction,
    val contentDescription: String,
)

enum class MoeTopBarAction {
    Import,
    Search,
    Share,
    More,
    Voice,
}

enum class MoeTopBarNavigation {
    Back,
    CaptchaClose,
}

sealed interface MoeStandardTopBarEvent {
    data object NavigateBack : MoeStandardTopBarEvent

    data class Action(
        val action: MoeTopBarAction,
    ) : MoeStandardTopBarEvent
}

sealed interface MoeSearchTopBarEvent {
    data object NavigateBack : MoeSearchTopBarEvent

    data class QueryChanged(
        val value: String,
    ) : MoeSearchTopBarEvent

    data object Submit : MoeSearchTopBarEvent

    data class Action(
        val action: MoeTopBarAction,
    ) : MoeSearchTopBarEvent
}
