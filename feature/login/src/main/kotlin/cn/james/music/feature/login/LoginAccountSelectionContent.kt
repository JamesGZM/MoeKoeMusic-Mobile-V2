package cn.james.music.feature.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.action.MoeButton
import cn.james.music.core.designsystem.component.action.MoeButtonSize
import cn.james.music.core.designsystem.component.action.MoeOutlinedButton
import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthError

@Composable
internal fun AccountSelectionContent(
    layout: LoginLayoutSpec,
    state: LoginUiState,
    onSelectAccount: (String) -> Unit,
    onSubmit: () -> Unit,
    onChooseOtherAccount: () -> Unit,
) {
    val accountFailure = (state.notice as? LoginNotice.Failure)?.error
    val requiresPhoneReverification = accountFailure == AuthError.Rejected
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = layout.page.contentStart,
                    top = layout.page.contentTop,
                    end = layout.page.contentEnd,
                    bottom = layout.page.contentBottom,
                ),
    ) {
        Text(stringResource(R.string.login_accounts_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.login_accounts_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(layout.dp(23f)))
        Surface(
            modifier = Modifier.loginLayoutProbe("accountList"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.selectableGroup()) {
                state.accounts.forEachIndexed { index, account ->
                    AccountRow(
                        account = account,
                        selected = state.selectedUserId == account.userId,
                        enabled = !state.isBusy,
                        onClick = { onSelectAccount(account.userId) },
                    )
                    if (index != state.accounts.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                stringResource(R.string.login_security),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        if (accountFailure != null) {
            AccountFailureNotice(accountFailure)
        } else {
            LoginNoticeText(state.notice)
        }
        Spacer(Modifier.height(16.dp))
        MoeButton(
            onClick = if (requiresPhoneReverification) onChooseOtherAccount else onSubmit,
            enabled = if (requiresPhoneReverification) !state.isBusy else state.canSubmitMobileCode,
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.largeButtonHeight),
            loading = state.loggingIn,
            size = MoeButtonSize.Large,
        ) {
            Text(
                stringResource(
                    if (requiresPhoneReverification) {
                        R.string.login_account_reverify_phone
                    } else {
                        R.string.login_account_submit
                    },
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
        MoeOutlinedButton(
            onClick = onChooseOtherAccount,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.buttonHeight),
        ) {
            Text(stringResource(R.string.login_account_other))
        }
    }
}

@Composable
private fun AccountFailureNotice(error: AuthError) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Error, contentDescription = null)
            Text(
                if (error == AuthError.Rejected) {
                    stringResource(R.string.login_account_verification_expired)
                } else {
                    error.message()
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AccountRow(
    account: AuthAccountOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = LocalAccountAvatarModels.current[account.userId] ?: account.avatarUrl,
            contentDescription = stringResource(R.string.login_account_avatar, account.nickname.ifBlank { stringResource(R.string.login_account_unnamed) }),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    account.nickname.ifBlank { stringResource(R.string.login_account_unnamed) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                account.grade?.takeIf(String::isNotBlank)?.let { badge ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
                    ) {
                        Text(badge, modifier = Modifier.padding(horizontal = 5.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                stringResource(R.string.login_account_id, maskUserId(account.userId)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = null,
            modifier = Modifier.size(48.dp),
        )
    }
}

internal val LocalAccountAvatarModels = staticCompositionLocalOf<Map<String, Any>> { emptyMap() }

private fun maskUserId(value: String): String =
    when {
        value.length <= 4 -> "••••"
        else -> "${value.take(3)}****${value.takeLast(4)}"
    }
