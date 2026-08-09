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
    entries: List<MyEntryUi>,
    onAction: (MyAction) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.myLayoutProbe(MY_PROBE_QUICK_ENTRIES),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = MoePassiveOutline(),
    ) {
        if (largeText) {
            Column(Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.medium)) {
                entries.take(4).chunked(2).forEach { rowEntries ->
                    Row(Modifier.fillMaxWidth()) {
                        rowEntries.forEach { entry ->
                            MyQuickEntry(entry, onAction, Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(112.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                entries.take(4).forEachIndexed { index, entry ->
                    if (index > 0) MyEntryDivider()
                    MyQuickEntry(entry, onAction)
                }
            }
        }
    }
}

@Composable
private fun MyQuickEntry(
    entry: MyEntryUi,
    onAction: (MyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = entry.id.quickVisual()
    val actionModifier = if (entry.available) Modifier.clickable { onAction(MyAction.ActivateEntry(entry.id)) } else Modifier
    Column(
        modifier = modifier.then(actionModifier).widthIn(min = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(visual.icon, contentDescription = stringResource(visual.titleRes), tint = MaterialTheme.colorScheme.primary)
        }
        EntryText(entry = entry, titleRes = visual.titleRes)
    }
}

@Composable
internal fun MyCollectionSection(
    entries: List<MyEntryUi>,
    onAction: (MyAction) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
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
                    entries.take(4).chunked(2).forEach { rowEntries ->
                        Row(Modifier.fillMaxWidth()) {
                            rowEntries.forEach { entry ->
                                MyCollectionEntry(entry, onAction, Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(103.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    entries.take(4).forEachIndexed { index, entry ->
                        if (index > 0) MyEntryDivider()
                        MyCollectionEntry(entry, onAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun MyCollectionEntry(
    entry: MyEntryUi,
    onAction: (MyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = entry.id.collectionVisual()
    val actionModifier = if (entry.available) Modifier.clickable { onAction(MyAction.ActivateEntry(entry.id)) } else Modifier
    Column(
        modifier = modifier.then(actionModifier).widthIn(min = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(visual.tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(visual.icon, contentDescription = null, tint = visual.tint)
        }
        EntryText(entry = entry, titleRes = visual.titleRes)
    }
}

@Composable
private fun EntryText(
    entry: MyEntryUi,
    titleRes: Int,
) {
    Text(
        stringResource(titleRes),
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
    entry.supportingText?.let {
        Text(it, fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class EntryVisual(
    val icon: ImageVector,
    val titleRes: Int,
    val tint: Color,
)

@Composable
private fun MyEntryId.quickVisual(): EntryVisual =
    when (this) {
        MyEntryId.Liked -> EntryVisual(Icons.Default.Favorite, R.string.my_liked, MaterialTheme.colorScheme.primary)
        MyEntryId.Recent -> EntryVisual(Icons.Default.History, R.string.my_recent, MaterialTheme.colorScheme.primary)
        MyEntryId.LocalMusic -> EntryVisual(Icons.Default.LibraryMusic, R.string.my_local_music, MaterialTheme.colorScheme.primary)
        MyEntryId.Cloud -> EntryVisual(Icons.Default.Cloud, R.string.my_cloud, MaterialTheme.colorScheme.primary)
        else -> error("Collection entry used in quick section: $this")
    }

@Composable
private fun MyEntryId.collectionVisual(): EntryVisual =
    when (this) {
        MyEntryId.SavedPlaylists -> EntryVisual(Icons.Default.FavoriteBorder, R.string.my_saved_playlists, Color(0xFFF14465))
        MyEntryId.SavedAlbums -> EntryVisual(Icons.Default.Album, R.string.my_saved_albums, MaterialTheme.colorScheme.tertiary)
        MyEntryId.FollowedArtists -> EntryVisual(Icons.Default.PersonSearch, R.string.my_followed_artists, MoeKoeTheme.extraColors.accentMint)
        MyEntryId.FollowedFriends -> EntryVisual(Icons.Default.Group, R.string.my_followed_friends, MaterialTheme.colorScheme.error)
        else -> error("Quick entry used in collection section: $this")
    }

@Composable
private fun MyEntryDivider() {
    MoeVerticalDivider(modifier = Modifier.height(64.dp))
}
