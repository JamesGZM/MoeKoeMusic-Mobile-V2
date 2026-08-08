package cn.james.music.feature.playlist

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import cn.james.music.core.designsystem.component.MoeMediaBadge
import cn.james.music.core.designsystem.component.MoeMediaBadgeTone
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarAction

@Composable
internal fun PlaylistDetailScreen(
    state: PlaylistDetailUiState,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMore: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSort: () -> Unit,
    onTrack: (Int) -> Unit,
    onTrackMore: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = (state as? PlaylistDetailUiState.Content)?.value?.title.orEmpty()
    Scaffold(
        modifier = modifier,
        topBar = {
            MoeStandardTopBar(
                title = title,
                navigationContentDescription = stringResource(R.string.playlist_detail_back),
                onNavigateBack = onBack,
                actions = {
                    MoeStandardTopBarAction(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.playlist_detail_search),
                        onClick = onSearch,
                        horizontalVisualOffset = 8.dp,
                    )
                    MoeStandardTopBarAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.playlist_detail_more),
                        onClick = onMore,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .background(playlistDetailBackground()),
            contentAlignment = Alignment.TopCenter,
        ) {
            val pageWidth = if (maxWidth >= 600.dp) 480.dp else maxWidth
            Surface(
                modifier = Modifier.width(pageWidth).height(maxHeight),
                color = Color.Transparent,
            ) {
                when (state) {
                    PlaylistDetailUiState.Loading -> PlaylistDetailLoading()
                    PlaylistDetailUiState.Empty ->
                        PlaylistDetailMessage(
                            title = stringResource(R.string.playlist_detail_empty_title),
                            message = stringResource(R.string.playlist_detail_empty_message),
                            onRetry = null,
                        )
                    PlaylistDetailUiState.Error ->
                        PlaylistDetailMessage(
                            title = stringResource(R.string.playlist_detail_error_title),
                            message = stringResource(R.string.playlist_detail_error_message),
                            onRetry = onRetry,
                        )
                    PlaylistDetailUiState.Offline ->
                        PlaylistDetailMessage(
                            title = stringResource(R.string.playlist_detail_offline_title),
                            message = stringResource(R.string.playlist_detail_offline_message),
                            onRetry = onRetry,
                        )
                    is PlaylistDetailUiState.Content ->
                        PlaylistDetailContent(
                            playlist = state.value,
                            onPlayAll = onPlayAll,
                            onShuffle = onShuffle,
                            onFavorite = onFavorite,
                            onDownload = onDownload,
                            onMore = onMore,
                            onSort = onSort,
                            onTrack = onTrack,
                            onTrackMore = onTrackMore,
                        )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailContent(
    playlist: PlaylistDetailUi,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
    onSort: () -> Unit,
    onTrack: (Int) -> Unit,
    onTrackMore: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(PLAYLIST_DETAIL_CONTENT_TAG),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item(key = "summary") { PlaylistSummary(playlist) }
        item(key = "actions") {
            PlaylistActions(
                isFavorite = playlist.isFavorite,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle,
                onFavorite = onFavorite,
                onDownload = onDownload,
                onMore = onMore,
            )
        }
        item(key = "songs-header") { PlaylistSongsHeader(onSort) }
        itemsIndexed(
            items = playlist.songs,
            key = { _, song -> song.id },
            contentType = { _, _ -> "playlist-track" },
        ) { index, song ->
            PlaylistTrackRow(
                index = index,
                song = song,
                isCurrent = song.id == playlist.currentTrackId,
                onClick = { onTrack(index) },
                onMore = { onTrackMore(index) },
            )
        }
    }
}

@Composable
private fun PlaylistSummary(playlist: PlaylistDetailUi) {
    val reflow = LocalDensity.current.fontScale >= 1.3f
    if (reflow) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlaylistCover(playlist, Modifier.size(184.dp))
            PlaylistSummaryText(
                playlist = playlist,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().height(188.dp).padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistCover(playlist, Modifier.width(150.dp).height(164.dp))
            PlaylistSummaryText(
                playlist = playlist,
                modifier = Modifier.weight(1f).padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun PlaylistCover(
    playlist: PlaylistDetailUi,
    modifier: Modifier,
) {
    Image(
        painter = painterResource(playlist.artworkRes),
        contentDescription = playlist.title,
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun PlaylistSummaryText(
    playlist: PlaylistDetailUi,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text(
            text = playlist.title,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(playlist.creatorAvatarRes),
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = playlist.creatorName,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (playlist.creatorVip) {
                MoeMediaBadge(
                    text = stringResource(R.string.playlist_detail_vip),
                    modifier = Modifier.padding(start = 8.dp),
                    tone = MoeMediaBadgeTone.Primary,
                )
            }
        }
        Text(
            text = stringResource(R.string.playlist_detail_statistics, playlist.songCount, playlist.durationLabel),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = playlist.description,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (LocalDensity.current.fontScale >= 1.3f) 4 else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaylistActions(
    isFavorite: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
) {
    val reflow = LocalDensity.current.fontScale >= 1.3f
    if (reflow) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlaylistPlayButton(onPlayAll, Modifier.weight(1f))
                PlaylistShuffleButton(onShuffle, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PlaylistCircleAction(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.playlist_detail_favorite),
                    onClick = onFavorite,
                )
                PlaylistCircleAction(
                    icon = Icons.Default.Download,
                    contentDescription = stringResource(R.string.playlist_detail_download),
                    onClick = onDownload,
                )
                PlaylistCircleAction(
                    icon = Icons.Default.MoreHoriz,
                    contentDescription = stringResource(R.string.playlist_detail_actions_more),
                    onClick = onMore,
                )
            }
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp).padding(start = 14.dp, top = 6.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistPlayButton(onPlayAll, Modifier.weight(1f))
        PlaylistShuffleButton(onShuffle, Modifier.weight(1f))
        PlaylistCircleAction(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(R.string.playlist_detail_favorite),
            onClick = onFavorite,
        )
        PlaylistCircleAction(
            icon = Icons.Default.Download,
            contentDescription = stringResource(R.string.playlist_detail_download),
            onClick = onDownload,
        )
        PlaylistCircleAction(
            icon = Icons.Default.MoreHoriz,
            contentDescription = stringResource(R.string.playlist_detail_actions_more),
            onClick = onMore,
        )
    }
}

@Composable
private fun PlaylistPlayButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.height(48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.playlist_detail_play_all),
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlaylistShuffleButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.height(48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.playlist_detail_shuffle),
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlaylistCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ) {}
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun PlaylistSongsHeader(onSort: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 18.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.playlist_detail_songs),
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onSort, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.playlist_detail_sort),
                modifier = Modifier.padding(start = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    index: Int,
    song: PlaylistTrackUi,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Row(
        modifier =
            Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .heightIn(min = if (largeText) 88.dp else if (isCurrent) 58.dp else 57.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f) else Color.Transparent)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCurrent) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        } else {
            Spacer(Modifier.width(3.dp))
        }
        Text(
            text = "${index + 1}",
            modifier = Modifier.width(30.dp),
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Image(
            painter = painterResource(song.artworkRes),
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = if (largeText) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        song.badge?.let { badge ->
            MoeMediaBadge(
                text = if (badge == PlaylistTrackBadgeUi.HighQuality) "HQ" else "MV",
                tone = if (badge == PlaylistTrackBadgeUi.HighQuality) MoeMediaBadgeTone.Primary else MoeMediaBadgeTone.Error,
            )
        }
        Text(
            text = song.durationLabel,
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        IconButton(onClick = onMore, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.playlist_detail_track_more, song.title),
            )
        }
    }
}

@Composable
private fun PlaylistDetailLoading() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp).semantics {
            contentDescription = "playlist-detail-loading"
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(154.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                repeat(4) { index ->
                    Box(
                        Modifier
                            .fillMaxWidth(if (index == 3) 0.82f else 1f)
                            .padding(bottom = 12.dp)
                            .height(if (index == 0) 26.dp else 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.playlist_detail_loading),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaylistDetailMessage(
    title: String,
    message: String,
    onRetry: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp),
            )
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp).widthIn(max = 320.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        onRetry?.let {
            Button(onClick = it, modifier = Modifier.padding(top = 20.dp).height(48.dp)) {
                Text(stringResource(R.string.playlist_detail_retry))
            }
        }
    }
}

internal const val PLAYLIST_DETAIL_CONTENT_TAG = "playlist_detail_content"

@Composable
private fun playlistDetailBackground(): Brush {
    val colors = MaterialTheme.colorScheme
    val isLight = colors.background.luminance() > 0.5f
    return if (isLight) {
        Brush.verticalGradient(
            listOf(
                colors.primaryContainer.copy(alpha = 0.32f),
                colors.background,
                colors.background,
            ),
        )
    } else {
        Brush.verticalGradient(listOf(colors.background, colors.background))
    }
}
