package cn.james.music.feature.my

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeArtwork

@Composable
internal fun MyPlaylistSection(
    account: MyAccountUiState,
    playlists: List<MyPlaylistUi>,
    onAction: (MyAction) -> Unit,
) {
    val anonymous = account is MyAccountUiState.Anonymous
    Column(
        modifier = Modifier.myLayoutProbe(MY_PROBE_PLAYLIST_SECTION),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.my_created_playlists),
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            if (!anonymous) {
                TextButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.my_create_playlist))
                }
            }
        }
        when {
            anonymous -> MyAnonymousPlaylistState { onAction(MyAction.OpenLogin) }
            playlists.isEmpty() -> MyEmptyPlaylistState()
            else ->
                playlists.forEachIndexed { index, playlist ->
                    key(playlist.id) {
                        MyPlaylistRow(
                            playlist = playlist,
                            probeName =
                                when (index) {
                                    1 -> MY_PROBE_PLAYLIST_ROW_2
                                    2 -> MY_PROBE_PLAYLIST_ROW_3
                                    else -> null
                                },
                        )
                    }
                }
        }
    }
}

@Composable
private fun MyAnonymousPlaylistState(onLogin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onLogin),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = MoeKoeTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.small),
        ) {
            Image(
                painter = painterResource(R.drawable.my_playlist_empty),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(96.dp).alpha(0.38f),
            )
            Text(
                text = stringResource(R.string.my_playlists_login),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MyEmptyPlaylistState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = stringResource(R.string.my_playlists_empty),
            modifier = Modifier.fillMaxWidth().padding(MoeKoeTheme.spacing.large),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MyPlaylistRow(
    playlist: MyPlaylistUi,
    probeName: String?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 65.dp)
                .then(if (probeName == null) Modifier else Modifier.myLayoutProbe(probeName)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoeArtwork(size = 52.dp, shape = RoundedCornerShape(12.dp)) {
            Image(
                painter = painterResource(playlist.artwork.drawableRes()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(MoeKoeTheme.spacing.medium))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.space4)) {
            Text(
                playlist.title,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
            )
            Text(
                playlist.supportingText,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
