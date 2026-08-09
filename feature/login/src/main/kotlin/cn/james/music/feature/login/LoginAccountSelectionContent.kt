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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.MoeHorizontalDivider
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
                    start = layout.dp(36f),
                    top = layout.page.contentTop + layout.dp(19f),
                    end = layout.dp(36f),
                    bottom = layout.page.contentBottom,
                ),
    ) {
        Text(
            text = stringResource(R.string.login_accounts_title),
            modifier = Modifier.fillMaxWidth().loginLayoutProbe("accountTitle"),
            style = accountTitleStyle(layout),
        )
        Spacer(Modifier.height(layout.dp(16f)))
        Text(
            stringResource(R.string.login_accounts_description),
            style = accountDescriptionStyle(layout),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(layout.dp(34f)))
        Surface(
            modifier = Modifier.loginLayoutProbe("accountList"),
            shape = RoundedCornerShape(layout.dp(24f)),
            border = BorderStroke(layout.dp(2f), MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.selectableGroup()) {
                state.accounts.forEachIndexed { index, account ->
                    AccountRow(
                        layout = layout,
                        account = account,
                        selected = state.selectedUserId == account.userId,
                        enabled = !state.isBusy,
                        onClick = { onSelectAccount(account.userId) },
                    )
                    if (index != state.accounts.lastIndex) MoeHorizontalDivider()
                }
            }
        }
        Spacer(Modifier.height(layout.dp(16f)))
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
                style = accountSecurityStyle(layout),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(layout.dp(29f)))
        if (accountFailure != null) {
            AccountFailureNotice(accountFailure)
        } else {
            LoginNoticeText(state.notice)
        }
        Spacer(Modifier.height(16.dp))
        MoeButton(
            onClick = if (requiresPhoneReverification) onChooseOtherAccount else onSubmit,
            enabled = if (requiresPhoneReverification) !state.isBusy else state.canSubmitMobileCode,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(layout.dp(112f))
                    .loginLayoutProbe("accountPrimaryAction"),
            loading = state.loggingIn,
            size = MoeButtonSize.Regular,
        ) {
            Text(
                stringResource(
                    if (requiresPhoneReverification) {
                        R.string.login_account_reverify_phone
                    } else {
                        R.string.login_account_submit
                    },
                ),
                style = accountActionStyle(layout),
            )
        }
        Spacer(Modifier.height(16.dp))
        MoeOutlinedButton(
            onClick = onChooseOtherAccount,
            enabled = !state.isBusy,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(layout.dp(104f))
                    .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.login_account_other), style = accountActionStyle(layout))
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
    layout: LoginLayoutSpec,
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
                .padding(horizontal = layout.dp(24f), vertical = layout.dp(22.5f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = LocalAccountAvatarModels.current[account.userId] ?: account.avatarUrl,
            contentDescription = stringResource(R.string.login_account_avatar, account.nickname.ifBlank { stringResource(R.string.login_account_unnamed) }),
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(layout.dp(104f))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column(Modifier.weight(1f).padding(horizontal = layout.dp(24f))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(layout.dp(12f))) {
                Text(
                    account.nickname.ifBlank { stringResource(R.string.login_account_unnamed) },
                    style = accountNicknameStyle(layout),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                account.grade?.takeIf(String::isNotBlank)?.let { badge ->
                    Surface(
                        shape = RoundedCornerShape(layout.dp(8f)),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(layout.dp(2f), MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = layout.dp(8f), vertical = layout.dp(1f)),
                            style = accountBadgeStyle(layout),
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.login_account_id, maskUserId(account.userId)),
                style = accountIdStyle(layout),
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

private fun accountTitleStyle(layout: LoginLayoutSpec): TextStyle =
    TextStyle(
        fontSize = layout.sp(50f),
        lineHeight = layout.sp(70f),
        fontWeight = FontWeight.Medium,
    )

private fun accountDescriptionStyle(layout: LoginLayoutSpec): TextStyle =
    TextStyle(
        fontSize = layout.sp(26f),
        lineHeight = layout.sp(48f),
        fontWeight = FontWeight.Normal,
    )

private fun accountNicknameStyle(layout: LoginLayoutSpec): TextStyle =
    TextStyle(
        fontSize = layout.sp(34f),
        lineHeight = layout.sp(44f),
        fontWeight = FontWeight.Medium,
    )

private fun accountBadgeStyle(layout: LoginLayoutSpec): TextStyle =
    TextStyle(
        fontSize = layout.sp(22f),
        lineHeight = layout.sp(30f),
        fontWeight = FontWeight.Medium,
    )

private fun accountIdStyle(layout: LoginLayoutSpec): TextStyle =
    TextStyle(
        fontSize = layout.sp(26f),
        lineHeight = layout.sp(38f),
        fontWeight = FontWeight.Normal,
    )

private fun accountSecurityStyle(layout: LoginLayoutSpec): TextStyle =
    TextStyle(
        fontSize = layout.sp(26f),
        lineHeight = layout.sp(38f),
        fontWeight = FontWeight.Normal,
    )

private fun accountActionStyle(layout: LoginLayoutSpec): TextStyle =
    TextStyle(
        fontSize = layout.sp(32f),
        lineHeight = layout.sp(44f),
        fontWeight = FontWeight.Medium,
    )

internal val LocalAccountAvatarModels = staticCompositionLocalOf<Map<String, Any>> { emptyMap() }

private fun maskUserId(value: String): String =
    when {
        value.length <= 4 -> "••••"
        else -> "${value.take(3)}****${value.takeLast(4)}"
    }
