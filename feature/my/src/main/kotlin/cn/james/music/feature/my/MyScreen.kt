package cn.james.music.feature.my

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialog
import cn.james.music.core.model.account.UserProfileError
import cn.james.music.core.model.auth.AuthError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyScreen(
    state: MyUiState,
    onRefresh: () -> Unit,
    onLogin: () -> Unit,
    onLocalMusic: () -> Unit,
    onSettings: () -> Unit,
    onRequestLogout: () -> Unit,
    onDismissLogout: () -> Unit,
    onConfirmLogout: () -> Unit,
    onFoundationLab: () -> Unit,
    showFoundationLab: Boolean,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = MoeKoeTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.mediumLarge),
        ) {
            item {
                AccountSection(
                    account = state.account,
                    loggingOut = state.loggingOut,
                    onLogin = onLogin,
                    onRetry = onRefresh,
                    onSettings = onSettings,
                    onRequestLogout = onRequestLogout,
                )
            }
            state.refreshError?.let { error -> item { InlineFailure(error.profileMessage(), onRefresh) } }
            state.logoutError?.let { error -> item { InlineFailure(error.logoutMessage(), onRequestLogout) } }
            item { QuickEntries(state.account, state.library, onLogin, onLocalMusic) }
            item { CollectionSection(state.account, state.library, onLogin) }
            item { PlaylistSection(state.account, state.library.playlists, onLogin) }
            if (showFoundationLab) {
                item { TextButton(onClick = onFoundationLab) { Text(stringResource(R.string.my_open_foundation_lab)) } }
            }
        }
    }

    if (state.showLogoutConfirmation) {
        MoeAlertDialog(
            title = stringResource(R.string.my_logout_title),
            message = stringResource(R.string.my_logout_message),
            confirmLabel = stringResource(R.string.my_logout_confirm),
            dismissLabel = stringResource(R.string.my_cancel),
            onConfirm = onConfirmLogout,
            onDismissRequest = onDismissLogout,
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
        )
    }
}

@Composable
private fun AccountSection(
    account: MyAccountUiState,
    loggingOut: Boolean,
    onLogin: () -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onRequestLogout: () -> Unit,
) {
    when (account) {
        MyAccountUiState.Loading -> LoadingAccountCard()
        MyAccountUiState.Anonymous -> AnonymousAccountCard(onLogin, onSettings)
        is MyAccountUiState.Failure -> FailureAccountCard(account.error.profileMessage(), onRetry)
        is MyAccountUiState.Authenticated ->
            AuthenticatedAccountCard(
                profile = account.profile,
                loggingOut = loggingOut,
                onSettings = onSettings,
                onRequestLogout = onRequestLogout,
            )
    }
}

@Composable
private fun AuthenticatedAccountCard(
    profile: MyProfileUi,
    loggingOut: Boolean,
    onSettings: () -> Unit,
    onRequestLogout: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    var accountMenuExpanded by rememberSaveable(profile.userId) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(28.dp)),
    ) {
        Column(Modifier.padding(MoeKoeTheme.spacing.mediumLarge)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(profile.avatarUrl, if (largeText) 64.dp else 76.dp)
                Spacer(Modifier.width(MoeKoeTheme.spacing.medium))
                Box(Modifier.weight(1f)) {
                    Column(
                        Modifier.fillMaxWidth().clickable(enabled = !loggingOut) { accountMenuExpanded = true },
                        verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
                            Text(
                                text = profile.nickname ?: stringResource(R.string.my_user_fallback, profile.userId),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            profile.vipLabel?.let { VipBadge(it) }
                        }
                        Text(
                            text = stringResource(R.string.my_welcome_back),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (profile.vipUnavailable) {
                            Text(
                                text = stringResource(R.string.my_vip_unavailable),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    AccountDropdownMenu(
                        expanded = accountMenuExpanded,
                        loggingOut = loggingOut,
                        onDismiss = { accountMenuExpanded = false },
                        onRequestLogout = onRequestLogout,
                    )
                }
                Box(Modifier.size(width = 48.dp, height = 76.dp)) {
                    SettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopCenter))
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
            Spacer(Modifier.height(MoeKoeTheme.spacing.mediumLarge))
            Row(horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.compact)) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null)
                    Spacer(Modifier.width(MoeKoeTheme.spacing.small))
                    Text(stringResource(R.string.my_sign_in))
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MoeKoeTheme.extraColors.vipGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MoeKoeTheme.extraColors.vipGold),
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                    Spacer(Modifier.width(MoeKoeTheme.spacing.small))
                    Text(stringResource(R.string.my_claim_vip))
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    url: String?,
    size: androidx.compose.ui.unit.Dp,
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
private fun VipBadge(label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier.padding(horizontal = MoeKoeTheme.spacing.small, vertical = MoeKoeTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(MoeKoeTheme.dimensions.iconSmall),
                tint = MoeKoeTheme.extraColors.vipGold,
            )
            Spacer(Modifier.width(MoeKoeTheme.spacing.space4))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.my_settings))
        }
    }
}

@Composable
private fun AccountDropdownMenu(
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

@Composable
private fun AnonymousAccountCard(
    onLogin: () -> Unit,
    onSettings: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(MoeKoeTheme.spacing.mediumLarge)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f).clickable(onClick = onLogin),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.my_anonymous_avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(76.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape),
                    )
                    Spacer(Modifier.width(MoeKoeTheme.spacing.medium))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4)) {
                        Text(stringResource(R.string.my_login_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.my_login_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(Modifier.size(width = 48.dp, height = 76.dp)) {
                    SettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopCenter))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.my_login_action),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            Spacer(Modifier.height(MoeKoeTheme.spacing.mediumLarge))
            Row(horizontalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.compact)) {
                OutlinedButton(
                    onClick = onLogin,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null)
                    Spacer(Modifier.width(MoeKoeTheme.spacing.small))
                    Text(stringResource(R.string.my_sign_in))
                }
                OutlinedButton(
                    onClick = onLogin,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, MoeKoeTheme.extraColors.vipGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MoeKoeTheme.extraColors.vipGold),
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                    Spacer(Modifier.width(MoeKoeTheme.spacing.small))
                    Text(stringResource(R.string.my_claim_vip))
                }
            }
        }
    }
}

@Composable
private fun LoadingAccountCard() {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().height(156.dp).padding(MoeKoeTheme.spacing.mediumLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(MoeKoeTheme.spacing.compact))
            Text(stringResource(R.string.my_loading_profile), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FailureAccountCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.62f)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MoeKoeTheme.spacing.mediumLarge),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.compact),
        ) {
            Text(stringResource(R.string.my_profile_failed), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = onRetry) { Text(stringResource(R.string.my_retry)) }
        }
    }
}

@Composable
private fun InlineFailure(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MoeKoeTheme.spacing.medium, vertical = MoeKoeTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.my_retry)) }
        }
    }
}

@Composable
private fun QuickEntries(
    account: MyAccountUiState,
    library: MyLibraryUi,
    onLogin: () -> Unit,
    onLocalMusic: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    val accountAction = if (account is MyAccountUiState.Anonymous) onLogin else null
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (largeText) {
            Column(Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.medium)) {
                Row(Modifier.fillMaxWidth()) {
                    QuickEntry(Icons.Default.Favorite, R.string.my_liked, library.likedCount, accountAction, Modifier.weight(1f))
                    QuickEntry(Icons.Default.History, R.string.my_recent, library.recentCount, accountAction, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) {
                    QuickEntry(Icons.Default.LibraryMusic, R.string.my_local_music, library.localCount, onLocalMusic, Modifier.weight(1f))
                    QuickEntry(Icons.Default.Cloud, R.string.my_cloud, library.cloudSize, accountAction, Modifier.weight(1f))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.medium),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickEntry(Icons.Default.Favorite, R.string.my_liked, library.likedCount, accountAction)
                EntryDivider()
                QuickEntry(Icons.Default.History, R.string.my_recent, library.recentCount, accountAction)
                EntryDivider()
                QuickEntry(Icons.Default.LibraryMusic, R.string.my_local_music, library.localCount, onLocalMusic)
                EntryDivider()
                QuickEntry(Icons.Default.Cloud, R.string.my_cloud, library.cloudSize, accountAction)
            }
        }
    }
}

@Composable
private fun QuickEntry(
    icon: ImageVector,
    titleRes: Int,
    supportingText: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val actionModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier.then(actionModifier).widthIn(min = 72.dp).padding(vertical = MoeKoeTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small),
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = stringResource(titleRes),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(stringResource(titleRes), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CollectionSection(
    account: MyAccountUiState,
    library: MyLibraryUi,
    onLogin: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    val accountAction = if (account is MyAccountUiState.Anonymous) onLogin else null
    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.compact)) {
        Text(stringResource(R.string.my_collection_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            if (largeText) {
                Column(Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.medium)) {
                    Row(Modifier.fillMaxWidth()) {
                        CollectionEntry(Icons.Default.FavoriteBorder, R.string.my_saved_playlists, library.savedPlaylistCount, Color(0xFFF14465), accountAction, Modifier.weight(1f))
                        CollectionEntry(Icons.Default.Album, R.string.my_saved_albums, library.savedAlbumCount, MaterialTheme.colorScheme.tertiary, accountAction, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        CollectionEntry(
                            Icons.Default.PersonSearch,
                            R.string.my_followed_artists,
                            library.followedArtistCount,
                            MoeKoeTheme.extraColors.accentMint,
                            accountAction,
                            Modifier.weight(1f),
                        )
                        CollectionEntry(Icons.Default.Group, R.string.my_followed_friends, library.followedFriendCount, MaterialTheme.colorScheme.error, accountAction, Modifier.weight(1f))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.medium),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CollectionEntry(Icons.Default.FavoriteBorder, R.string.my_saved_playlists, library.savedPlaylistCount, Color(0xFFF14465), accountAction)
                    EntryDivider()
                    CollectionEntry(Icons.Default.Album, R.string.my_saved_albums, library.savedAlbumCount, MaterialTheme.colorScheme.tertiary, accountAction)
                    EntryDivider()
                    CollectionEntry(Icons.Default.PersonSearch, R.string.my_followed_artists, library.followedArtistCount, MoeKoeTheme.extraColors.accentMint, accountAction)
                    EntryDivider()
                    CollectionEntry(Icons.Default.Group, R.string.my_followed_friends, library.followedFriendCount, MaterialTheme.colorScheme.error, accountAction)
                }
            }
        }
    }
}

@Composable
private fun CollectionEntry(
    icon: ImageVector,
    titleRes: Int,
    supportingText: String?,
    tint: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val actionModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier.then(actionModifier).widthIn(min = 72.dp).padding(vertical = MoeKoeTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small),
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Text(stringResource(titleRes), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaylistSection(
    account: MyAccountUiState,
    playlists: List<MyPlaylistUi>,
    onLogin: () -> Unit,
) {
    val anonymous = account is MyAccountUiState.Anonymous
    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.compact)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.my_created_playlists),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (!anonymous) {
                TextButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.my_create_playlist))
                }
            }
        }
        if (anonymous) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onLogin),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.compact),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        )
                    }
                    Text(
                        text = stringResource(R.string.my_playlists_login),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else if (playlists.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Text(
                    text = stringResource(R.string.my_playlists_empty),
                    modifier = Modifier.fillMaxWidth().padding(MoeKoeTheme.spacing.large),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            playlists.forEach { playlist ->
                PlaylistRow(playlist)
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: MyPlaylistUi) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(playlist.artwork.drawableRes()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(MoeKoeTheme.spacing.medium))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4)) {
            Text(playlist.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(playlist.supportingText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.my_playlist_menu, playlist.title))
        }
    }
}

private fun MyPlaylistArtwork.drawableRes(): Int =
    when (this) {
        MyPlaylistArtwork.Liked -> R.drawable.my_playlist_liked
        MyPlaylistArtwork.Acg -> R.drawable.my_playlist_acg
        MyPlaylistArtwork.NightRadio -> R.drawable.my_playlist_night_radio
    }

@Composable
private fun EntryDivider() {
    VerticalDivider(
        modifier = Modifier.height(76.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun UserProfileError.profileMessage(): String =
    stringResource(
        when (this) {
            UserProfileError.SessionInitialization -> R.string.my_error_session
            UserProfileError.Offline -> R.string.my_error_offline
            UserProfileError.Timeout -> R.string.my_error_timeout
            UserProfileError.Connection -> R.string.my_error_connection
            UserProfileError.ServiceUnavailable -> R.string.my_error_service
            UserProfileError.VerificationRequired -> R.string.my_error_verification
            UserProfileError.Rejected -> R.string.my_error_rejected
            UserProfileError.Protocol -> R.string.my_error_protocol
        },
    )

@Composable
private fun AuthError.logoutMessage(): String =
    stringResource(
        when (this) {
            AuthError.Storage, AuthError.SessionInitialization -> R.string.my_logout_error_storage
            AuthError.Offline, AuthError.Timeout, AuthError.Connection, AuthError.ServiceUnavailable -> R.string.my_logout_error_retry
            AuthError.Rejected, AuthError.Protocol -> R.string.my_logout_error_retry
        },
    )

private const val LARGE_TEXT_SCALE = 1.3f
