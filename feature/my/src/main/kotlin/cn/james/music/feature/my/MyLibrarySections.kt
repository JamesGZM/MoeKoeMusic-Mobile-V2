package cn.james.music.feature.my

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoePassiveOutline
import cn.james.music.core.designsystem.component.MoeVerticalDivider

@Composable
internal fun MyQuickEntries(
    account: MyAccountUiState,
    library: MyLibraryUi,
    onAction: (MyAction) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val accountAction = if (account is MyAccountUiState.Anonymous) ({ onAction(MyAction.OpenLogin) }) else null
    Surface(
        modifier = Modifier.myLayoutProbe(MY_PROBE_QUICK_ENTRIES),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = MoePassiveOutline(),
    ) {
        if (largeText) {
            Column(Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.medium)) {
                Row(Modifier.fillMaxWidth()) {
                    MyQuickEntry(Icons.Default.Favorite, R.string.my_liked, library.likedCount, accountAction, Modifier.weight(1f))
                    MyQuickEntry(Icons.Default.History, R.string.my_recent, library.recentCount, accountAction, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) {
                    MyQuickEntry(
                        Icons.Default.LibraryMusic,
                        R.string.my_local_music,
                        library.localCount,
                        { onAction(MyAction.OpenLocalMusic) },
                        Modifier.weight(1f),
                    )
                    MyQuickEntry(Icons.Default.Cloud, R.string.my_cloud, library.cloudSize, accountAction, Modifier.weight(1f))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(112.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MyQuickEntry(Icons.Default.Favorite, R.string.my_liked, library.likedCount, accountAction)
                MyEntryDivider()
                MyQuickEntry(Icons.Default.History, R.string.my_recent, library.recentCount, accountAction)
                MyEntryDivider()
                MyQuickEntry(
                    Icons.Default.LibraryMusic,
                    R.string.my_local_music,
                    library.localCount,
                    { onAction(MyAction.OpenLocalMusic) },
                )
                MyEntryDivider()
                MyQuickEntry(Icons.Default.Cloud, R.string.my_cloud, library.cloudSize, accountAction)
            }
        }
    }
}

@Composable
private fun MyQuickEntry(
    icon: ImageVector,
    titleRes: Int,
    supportingText: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val actionModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier.then(actionModifier).widthIn(min = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = stringResource(titleRes), tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            stringResource(titleRes),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        supportingText?.let {
            Text(
                it,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MyCollectionSection(
    account: MyAccountUiState,
    library: MyLibraryUi,
    onAction: (MyAction) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val accountAction = if (account is MyAccountUiState.Anonymous) ({ onAction(MyAction.OpenLogin) }) else null
    Column(verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small)) {
        Text(
            stringResource(R.string.my_collection_title),
            modifier = Modifier.padding(top = 3.dp),
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            modifier = Modifier.myLayoutProbe(MY_PROBE_COLLECTION_GRID),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = MoePassiveOutline(),
        ) {
            if (largeText) {
                Column(Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.medium)) {
                    Row(Modifier.fillMaxWidth()) {
                        MyCollectionEntry(Icons.Default.FavoriteBorder, R.string.my_saved_playlists, library.savedPlaylistCount, Color(0xFFF14465), accountAction, Modifier.weight(1f))
                        MyCollectionEntry(Icons.Default.Album, R.string.my_saved_albums, library.savedAlbumCount, MaterialTheme.colorScheme.tertiary, accountAction, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        MyCollectionEntry(
                            Icons.Default.PersonSearch,
                            R.string.my_followed_artists,
                            library.followedArtistCount,
                            MoeKoeTheme.extraColors.accentMint,
                            accountAction,
                            Modifier.weight(1f),
                        )
                        MyCollectionEntry(Icons.Default.Group, R.string.my_followed_friends, library.followedFriendCount, MaterialTheme.colorScheme.error, accountAction, Modifier.weight(1f))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(103.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MyCollectionEntry(Icons.Default.FavoriteBorder, R.string.my_saved_playlists, library.savedPlaylistCount, Color(0xFFF14465), accountAction)
                    MyEntryDivider()
                    MyCollectionEntry(Icons.Default.Album, R.string.my_saved_albums, library.savedAlbumCount, MaterialTheme.colorScheme.tertiary, accountAction)
                    MyEntryDivider()
                    MyCollectionEntry(Icons.Default.PersonSearch, R.string.my_followed_artists, library.followedArtistCount, MoeKoeTheme.extraColors.accentMint, accountAction)
                    MyEntryDivider()
                    MyCollectionEntry(Icons.Default.Group, R.string.my_followed_friends, library.followedFriendCount, MaterialTheme.colorScheme.error, accountAction)
                }
            }
        }
    }
}

@Composable
private fun MyCollectionEntry(
    icon: ImageVector,
    titleRes: Int,
    supportingText: String?,
    tint: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val actionModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier.then(actionModifier).widthIn(min = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Text(
            stringResource(titleRes),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        supportingText?.let {
            Text(
                it,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MyEntryDivider() {
    MoeVerticalDivider(modifier = Modifier.height(64.dp))
}
