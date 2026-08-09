package cn.james.music.feature.localmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.designsystem.component.MoeSongMoreAction
import cn.james.music.core.designsystem.component.MoeSongRowStyle
import coil3.compose.AsyncImage
import java.io.File

@Composable
internal fun LocalMusicMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Column(
        modifier = modifier.padding(MoeKoeTheme.spacing.large),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            text = message,
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.compact),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun LocalMusicRow(
    song: LocalSongRowUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoeSongRow(
        title = song.title,
        subtitle = song.artist,
        metadata = song.durationLabel,
        onClick = onClick,
        modifier = modifier,
        style = MoeSongRowStyle.Comfortable,
        isPlaying = song.isPlaying,
        showPlayingIndicator = false,
        artwork = {
            LocalArtworkRenderer(artwork = song.artwork, title = song.title)
        },
        badges = {
            if (song.isPlaying) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailing = {
            MoeSongMoreAction(
                contentDescription = stringResource(R.string.local_music_more, song.title),
                onClick = onDelete,
            )
        },
    )
}

@Composable
private fun LocalArtworkRenderer(
    artwork: LocalArtworkUiModel,
    title: String,
) {
    val context = LocalContext.current
    val model =
        when (artwork) {
            is LocalArtworkUiModel.AppStorage -> File(context.filesDir, artwork.ref.key)
            LocalArtworkUiModel.None -> null
        }
    AsyncImage(
        model = model,
        contentDescription = stringResource(R.string.local_music_artwork_description, title),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
