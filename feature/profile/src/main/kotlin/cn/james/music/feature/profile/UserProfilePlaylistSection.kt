package cn.james.music.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.MoeArtwork

internal fun LazyListScope.userProfilePlaylistSection(
    playlists: List<UserPlaylistUi>,
    onAction: (UserProfileAction) -> Unit,
) {
    item(key = "playlist-title") {
        PlaylistSectionTitle(
            showViewAll = playlists.isNotEmpty(),
            onViewAll = { onAction(UserProfileAction.ViewAllPlaylists) },
        )
    }
    if (playlists.isEmpty()) {
        item(key = "playlist-empty") { EmptyPlaylistSection() }
    } else {
        item(key = "playlist-list") {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .userProfileLayoutProbe(PROBE_PLAYLIST_LIST),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)),
            ) {
                Column {
                    playlists.forEachIndexed { index, playlist ->
                        UserPlaylistRow(
                            playlist = playlist,
                            onClick = { onAction(UserProfileAction.OpenPlaylist(playlist.id)) },
                            onMore = { onAction(UserProfileAction.OpenPlaylistMore(playlist.id)) },
                            probeName =
                                when (index) {
                                    1 -> PROBE_PLAYLIST_ROW_2
                                    2 -> PROBE_PLAYLIST_ROW_3
                                    else -> null
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistSectionTitle(
    showViewAll: Boolean,
    onViewAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.profile_created_playlists),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).align(Alignment.Bottom).padding(bottom = 5.dp),
        )
        if (showViewAll) {
            Box(
                modifier = Modifier.height(48.dp).clickable(role = Role.Button, onClick = onViewAll),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 2.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.profile_view_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserPlaylistRow(
    playlist: UserPlaylistUi,
    onClick: () -> Unit,
    onMore: () -> Unit,
    probeName: String?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 63.dp)
                .then(if (probeName == null) Modifier else Modifier.userProfileLayoutProbe(probeName))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoeArtwork(size = 52.dp, shape = RoundedCornerShape(12.dp)) {
            Image(
                painter = painterResource(playlist.artworkRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            Modifier.weight(1f).padding(start = 14.dp, top = 16.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                playlist.title,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                playlist.songCountLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
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
