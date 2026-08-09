package cn.james.music.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.MoeMediaBadge
import cn.james.music.core.designsystem.component.MoeMediaBadgeTone
import cn.james.music.core.designsystem.component.MoePassiveOutline

@Composable
internal fun SearchArtistHero(
    artist: SearchArtistUi,
    onFollow: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).searchLayoutProbe(SEARCH_PROBE_ARTIST_HERO),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = MoePassiveOutline(),
    ) {
        if (largeText) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ArtistAvatar(artist)
                ArtistDetails(artist, Modifier.fillMaxWidth().padding(vertical = 10.dp))
                FollowButton(onFollow)
            }
        } else {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtistAvatar(artist)
                ArtistDetails(artist, Modifier.weight(1f).padding(horizontal = 14.dp))
                FollowButton(onFollow)
            }
        }
    }
}

@Composable
private fun FollowButton(onFollow: () -> Unit) {
    Box(
        modifier = Modifier.width(56.dp).height(48.dp).clickable(role = Role.Button, onClick = onFollow),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp, max = 48.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.search_follow), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ArtistAvatar(artist: SearchArtistUi) {
    Image(
        painterResource(artist.artworkRes),
        contentDescription = artist.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(64.dp).clip(CircleShape),
    )
}

@Composable
private fun ArtistDetails(
    artist: SearchArtistUi,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                artist.name,
                style = MaterialTheme.typography.titleSmall.copy(lineHeight = 22.sp),
                fontWeight = FontWeight.SemiBold,
            )
            artist.badge?.let { MoeMediaBadge(it, tone = MoeMediaBadgeTone.Accent) }
        }
        Text(artist.stats, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(artist.representativeWorks, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
