package cn.james.music.feature.my

internal sealed interface MyAction {
    data object RefreshProfile : MyAction

    data object OpenLogin : MyAction

    data object OpenLocalMusic : MyAction

    data object OpenSettings : MyAction

    data object OpenProfile : MyAction

    data object RequestLogout : MyAction

    data object DismissLogout : MyAction

    data object ConfirmLogout : MyAction

    data object OpenFoundationLab : MyAction
}
