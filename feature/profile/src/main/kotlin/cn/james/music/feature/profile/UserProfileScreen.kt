package cn.james.music.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarAction

@Composable
internal fun UserProfileScreen(
    state: UserProfileUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
    onEdit: () -> Unit,
    onFollowing: () -> Unit,
    onFollowers: () -> Unit,
    onFriends: () -> Unit,
    onViewAll: () -> Unit,
    onPlaylist: (String) -> Unit,
    onPlaylistMore: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MoeStandardTopBar(
                title = stringResource(R.string.profile_title),
                navigationContentDescription = stringResource(R.string.profile_back),
                onNavigateBack = onBack,
                actions = {
                    MoeStandardTopBarAction(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.profile_share),
                        onClick = onShare,
                    )
                    MoeStandardTopBarAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.profile_more),
                        onClick = onMore,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val pageWidth = if (maxWidth >= 600.dp) 480.dp else maxWidth
            Surface(modifier = Modifier.width(pageWidth).height(maxHeight), color = Color.Transparent) {
                when (state) {
                    UserProfileUiState.Loading -> UserProfileLoading()
                    UserProfileUiState.Error ->
                        UserProfileMessage(
                            title = stringResource(R.string.profile_error_title),
                            message = stringResource(R.string.profile_error_message),
                            onRetry = onRetry,
                        )
                    UserProfileUiState.Offline ->
                        UserProfileMessage(
                            title = stringResource(R.string.profile_offline_title),
                            message = stringResource(R.string.profile_offline_message),
                            onRetry = onRetry,
                        )
                    is UserProfileUiState.Content ->
                        UserProfileContent(
                            profile = state.value,
                            onEdit = onEdit,
                            onFollowing = onFollowing,
                            onFollowers = onFollowers,
                            onFriends = onFriends,
                            onViewAll = onViewAll,
                            onPlaylist = onPlaylist,
                            onPlaylistMore = onPlaylistMore,
                        )
                }
            }
        }
    }
}

@Composable
private fun UserProfileContent(
    profile: UserProfileUi,
    onEdit: () -> Unit,
    onFollowing: () -> Unit,
    onFollowers: () -> Unit,
    onFriends: () -> Unit,
    onViewAll: () -> Unit,
    onPlaylist: (String) -> Unit,
    onPlaylistMore: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(USER_PROFILE_CONTENT_TAG),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item(key = "hero") {
            ProfileHero(profile, onEdit, onFollowing, onFollowers, onFriends)
        }
        item(key = "overview-title") { SectionTitle(stringResource(R.string.profile_listening_overview)) }
        item(key = "overview") { ListeningOverview(profile) }
        item(key = "playlist-title") {
            PlaylistSectionTitle(showViewAll = profile.playlists.isNotEmpty(), onViewAll = onViewAll)
        }
        if (profile.playlists.isEmpty()) {
            item(key = "playlist-empty") { EmptyPlaylistSection() }
        } else {
            item(key = "playlist-list") {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)),
                ) {
                    Column {
                        profile.playlists.forEach { playlist ->
                            UserPlaylistRow(
                                playlist = playlist,
                                onClick = { onPlaylist(playlist.id) },
                                onMore = { onPlaylistMore(playlist.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHero(
    profile: UserProfileUi,
    onEdit: () -> Unit,
    onFollowing: () -> Unit,
    onFollowers: () -> Unit,
    onFriends: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    ),
                ),
            ),
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(104.dp),
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                if (largeText) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        ProfileAvatar(profile)
                        ProfileIdentity(profile, Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(profile)
                        ProfileIdentity(profile, Modifier.weight(1f).padding(start = 16.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                ProfileRelations(profile, onFollowing, onFollowers, onFriends)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_edit), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfileUi) {
    Surface(
        modifier = Modifier.size(84.dp),
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Image(
            painter = painterResource(profile.avatarRes),
            contentDescription = stringResource(R.string.profile_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
    }
}

@Composable
private fun ProfileIdentity(
    profile: UserProfileUi,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = profile.nickname,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            profile.vipLabel?.let { ProfileBadge(it, vip = true) }
            ProfileBadge(profile.levelLabel, vip = false)
        }
        Text(
            text = profile.signature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileBadge(
    label: String,
    vip: Boolean,
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vip) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MoeKoeTheme.extraColors.vipGold,
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProfileRelations(
    profile: UserProfileUi,
    onFollowing: () -> Unit,
    onFollowers: () -> Unit,
    onFriends: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
        RelationItem(profile.followingCount, stringResource(R.string.profile_following), onFollowing, Modifier.weight(1f))
        VerticalDivider(Modifier.height(34.dp), color = MaterialTheme.colorScheme.outlineVariant)
        RelationItem(profile.followerCount, stringResource(R.string.profile_followers), onFollowers, Modifier.weight(1f))
        VerticalDivider(Modifier.height(34.dp), color = MaterialTheme.colorScheme.outlineVariant)
        RelationItem(profile.friendCount, stringResource(R.string.profile_friends), onFriends, Modifier.weight(1f))
    }
}

@Composable
private fun RelationItem(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun ListeningOverview(profile: UserProfileUi) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)),
    ) {
        if (largeText) {
            Column(Modifier.padding(vertical = 10.dp)) {
                OverviewItem(profile.levelLabel, stringResource(R.string.profile_level), Modifier.fillMaxWidth())
                OverviewItem(profile.listeningDuration, stringResource(R.string.profile_listening_duration), Modifier.fillMaxWidth())
                OverviewItem(profile.musicAge, stringResource(R.string.profile_music_age), Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(86.dp).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OverviewItem(profile.levelLabel, stringResource(R.string.profile_level), Modifier.weight(1f))
                VerticalDivider(Modifier.height(38.dp), color = MaterialTheme.colorScheme.outlineVariant)
                OverviewItem(profile.listeningDuration, stringResource(R.string.profile_listening_duration), Modifier.weight(1f))
                VerticalDivider(Modifier.height(38.dp), color = MaterialTheme.colorScheme.outlineVariant)
                OverviewItem(profile.musicAge, stringResource(R.string.profile_music_age), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverviewItem(
    value: String,
    label: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlaylistSectionTitle(
    showViewAll: Boolean,
    onViewAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.profile_created_playlists),
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (showViewAll) {
            TextButton(onClick = onViewAll) {
                Text(stringResource(R.string.profile_view_all))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun UserPlaylistRow(
    playlist: UserPlaylistUi,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(playlist.artworkRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                playlist.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                playlist.songCountLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.profile_open_playlist, playlist.title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.profile_playlist_more, playlist.title),
            )
        }
    }
}

@Composable
private fun EmptyPlaylistSection() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.profile_empty_title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.profile_empty_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun UserProfileLoading() {
    Column(
        Modifier.fillMaxSize().semantics { contentDescription = "profile-loading" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.profile_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UserProfileMessage(
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.profile_retry))
        }
    }
}

internal const val USER_PROFILE_CONTENT_TAG = "user_profile_content"
