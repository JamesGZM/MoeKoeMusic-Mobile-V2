package cn.james.music.feature.my

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialog
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialogEvent
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialogIcon
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialogUiModel
import cn.james.music.core.designsystem.component.overlay.MoeDialogActionsUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyScreen(
    state: MyUiState,
    onAction: (MyAction) -> Unit,
    showFoundationLab: Boolean,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { onAction(MyAction.RefreshProfile) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 14.dp,
                    top = 28.dp,
                    end = 14.dp,
                    bottom = MoeKoeTheme.spacing.mediumLarge,
                ),
            verticalArrangement = Arrangement.Top,
        ) {
            item(key = "account") {
                MyAccountSection(
                    account = state.account,
                    loggingOut = state.loggingOut,
                    onAction = onAction,
                )
            }
            state.refreshProblem?.let { problem ->
                item(key = "refresh-error") {
                    MyInlineFailure(
                        message = problem.profileMessage(),
                        onRetry = { onAction(MyAction.RefreshProfile) },
                    )
                }
            }
            state.logoutProblem?.let { problem ->
                item(key = "logout-error") {
                    MyInlineFailure(
                        message = problem.logoutMessage(),
                        onRetry = { onAction(MyAction.RequestLogout) },
                    )
                }
            }
            item(key = "quick-entries") {
                Box(Modifier.padding(top = 12.dp)) {
                    MyQuickEntries(
                        entries = state.library.quickEntries,
                        onAction = onAction,
                    )
                }
            }
            item(key = "collection") {
                Box(Modifier.padding(top = 8.dp)) {
                    MyCollectionSection(
                        entries = state.library.collectionEntries,
                        onAction = onAction,
                    )
                }
            }
            item(key = "playlists") {
                MyPlaylistSection(
                    account = state.account,
                    playlists = state.library.playlists,
                    onAction = onAction,
                )
            }
            if (showFoundationLab) {
                item(key = "foundation-lab") {
                    TextButton(onClick = { onAction(MyAction.OpenFoundationLab) }) {
                        Text(stringResource(R.string.my_open_foundation_lab))
                    }
                }
            }
        }
    }

    if (state.showLogoutConfirmation) {
        MoeAlertDialog(
            model =
                MoeAlertDialogUiModel(
                    title = stringResource(R.string.my_logout_title),
                    message = stringResource(R.string.my_logout_message),
                    icon = MoeAlertDialogIcon.Account,
                    actions =
                        MoeDialogActionsUiModel(
                            confirmLabel = stringResource(R.string.my_logout_confirm),
                            dismissLabel = stringResource(R.string.my_cancel),
                        ),
                ),
            onEvent = { event ->
                when (event) {
                    MoeAlertDialogEvent.Confirm -> onAction(MyAction.ConfirmLogout)
                    MoeAlertDialogEvent.Dismiss -> onAction(MyAction.DismissLogout)
                }
            },
        )
    }
}
