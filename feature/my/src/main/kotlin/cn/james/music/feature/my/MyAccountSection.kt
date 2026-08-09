package cn.james.music.feature.my

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme

@Composable
internal fun MyAccountSection(
    account: MyAccountUiState,
    loggingOut: Boolean,
    onAction: (MyAction) -> Unit,
) {
    when (account) {
        MyAccountUiState.Loading -> MyLoadingAccountCard()
        MyAccountUiState.Anonymous -> MyAnonymousAccountCard(onAction)
        is MyAccountUiState.Failure ->
            MyFailureAccountCard(
                message = account.problem.profileMessage(),
                onRetry = { onAction(MyAction.RefreshProfile) },
            )
        is MyAccountUiState.Authenticated ->
            MyAuthenticatedAccountCard(
                profile = account.profile,
                loggingOut = loggingOut,
                onAction = onAction,
            )
    }
}

@Composable
private fun MyAuthenticatedAccountCard(
    profile: MyProfileUi,
    loggingOut: Boolean,
    onAction: (MyAction) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    var accountMenuExpanded by rememberSaveable(profile.userId) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                .myLayoutProbe(MY_PROBE_ACCOUNT_CARD),
    ) {
        Column(
            Modifier.padding(
                start = MoeKoeTheme.spacing.mediumLarge,
                top = MoeKoeTheme.spacing.mediumLarge,
                end = MoeKoeTheme.spacing.mediumLarge,
                bottom = MoeKoeTheme.spacing.small,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MyProfileAvatar(profile.avatarUrl, if (largeText) 64.dp else 80.dp)
                Spacer(Modifier.width(MoeKoeTheme.spacing.medium))
                Box(Modifier.weight(1f)) {
                    Column(
                        Modifier.fillMaxWidth().clickable(enabled = !loggingOut) { onAction(MyAction.OpenProfile) },
                        verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small),
                        ) {
                            Text(
                                text = profile.nickname ?: stringResource(R.string.my_user_fallback, profile.userId),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            profile.vipLabel?.let { MyVipBadge(it) }
                        }
                        Text(
                            text = stringResource(R.string.my_welcome_back),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (profile.vipUnavailable) {
                            Text(
                                text = stringResource(R.string.my_vip_unavailable),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    MyAccountDropdownMenu(
                        expanded = accountMenuExpanded,
                        loggingOut = loggingOut,
                        onDismiss = { accountMenuExpanded = false },
                        onRequestLogout = { onAction(MyAction.RequestLogout) },
                    )
                }
                Box(Modifier.size(width = 48.dp, height = 76.dp).clickable(enabled = !loggingOut) { accountMenuExpanded = true }) {
                    MySettingsButton(
                        onClick = { onAction(MyAction.OpenSettings) },
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    if (loggingOut) {
                        CircularProgressIndicator(Modifier.align(Alignment.BottomCenter).size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.my_account_menu),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
            Spacer(Modifier.height(MoeKoeTheme.spacing.compact))
            MyAccountActionButtons(onLogin = null)
        }
    }
}

@Composable
private fun MyAnonymousAccountCard(onAction: (MyAction) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    start = MoeKoeTheme.spacing.mediumLarge,
                    top = MoeKoeTheme.spacing.mediumLarge,
                    end = MoeKoeTheme.spacing.mediumLarge,
                    bottom = MoeKoeTheme.spacing.compact,
                ),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f).clickable { onAction(MyAction.OpenLogin) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.my_anonymous_avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape),
                    )
                    Spacer(Modifier.width(MoeKoeTheme.spacing.medium))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4)) {
                        Text(stringResource(R.string.my_login_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.my_login_subtitle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Box(Modifier.size(width = 48.dp, height = 76.dp)) {
                    MySettingsButton(
                        onClick = { onAction(MyAction.OpenSettings) },
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.my_login_action),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            Spacer(Modifier.height(MoeKoeTheme.spacing.compact))
            MyAccountActionButtons(onLogin = { onAction(MyAction.OpenLogin) })
        }
    }
}

@Composable
private fun MyAccountActionButtons(onLogin: (() -> Unit)?) {
    val buttonHeight = if (onLogin == null) Modifier else Modifier.height(48.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.compact)) {
        OutlinedButton(
            onClick = onLogin ?: {},
            modifier = Modifier.weight(1f).then(buttonHeight),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(MoeKoeTheme.spacing.small))
            Text(stringResource(R.string.my_sign_in))
        }
        OutlinedButton(
            onClick = onLogin ?: {},
            modifier = Modifier.weight(1f).then(buttonHeight),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MoeKoeTheme.extraColors.vipGold.copy(alpha = 0.58f)),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MoeKoeTheme.extraColors.vipGold.copy(alpha = 0.06f),
                    contentColor = MoeKoeTheme.extraColors.vipGold,
                ),
        ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(MoeKoeTheme.spacing.small))
            Text(stringResource(R.string.my_claim_vip))
        }
    }
}

@Composable
private fun MyProfileAvatar(
    url: String?,
    size: Dp,
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(size)) {
        if (url == null) {
            Image(
                painter = painterResource(R.drawable.my_authenticated_avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.my_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

@Composable
private fun MyVipBadge(label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier.padding(horizontal = MoeKoeTheme.spacing.small, vertical = MoeKoeTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MoeKoeTheme.extraColors.vipGold,
            )
            Spacer(Modifier.width(MoeKoeTheme.spacing.space4))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MySettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.my_settings),
                    modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard),
                )
            }
        }
    }
}

@Composable
private fun MyAccountDropdownMenu(
    expanded: Boolean,
    loggingOut: Boolean,
    onDismiss: () -> Unit,
    onRequestLogout: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.my_logout_action)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            enabled = !loggingOut,
            onClick = {
                onDismiss()
                onRequestLogout()
            },
        )
    }
}
