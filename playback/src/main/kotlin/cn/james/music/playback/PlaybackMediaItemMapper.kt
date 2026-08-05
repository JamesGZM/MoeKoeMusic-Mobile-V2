package cn.james.music.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource

internal object PlaybackMediaItemMapper {
    private const val SOURCE_TYPE_KEY = "cn.james.music.playback.SOURCE_TYPE"
    private const val SOURCE_VALUE_KEY = "cn.james.music.playback.SOURCE_VALUE"
    private const val SOURCE_DEMO = "foundation_demo"
    private const val SOURCE_LOCAL = "imported_local"
    private const val SOURCE_KUGOU = "kugou"

    fun toRequest(item: PlaybackItem): MediaItem {
        val extras = Bundle()
        when (val source = item.source) {
            PlaybackSource.FoundationDemo -> {
                extras.putString(SOURCE_TYPE_KEY, SOURCE_DEMO)
            }

            is PlaybackSource.ImportedLocal -> {
                extras.putString(SOURCE_TYPE_KEY, SOURCE_LOCAL)
                extras.putString(SOURCE_VALUE_KEY, source.localMusicId)
            }

            is PlaybackSource.Kugou -> {
                extras.putString(SOURCE_TYPE_KEY, SOURCE_KUGOU)
                extras.putString(SOURCE_VALUE_KEY, source.songHash)
            }
        }
        return MediaItem
            .Builder()
            .setMediaId(item.id)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(item.title)
                    .setArtist(item.artist)
                    .setAlbumTitle(item.albumTitle)
                    .setExtras(extras)
                    .build(),
            ).build()
    }

    fun withUri(
        request: MediaItem,
        uri: Uri,
    ): MediaItem = request.buildUpon().setUri(uri).build()

    fun toModel(item: MediaItem): PlaybackItem? {
        val metadata = item.mediaMetadata
        val source = sourceFrom(metadata.extras) ?: return null
        return runCatching {
            PlaybackItem(
                id = item.mediaId,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                albumTitle = metadata.albumTitle?.toString(),
                source = source,
            )
        }.getOrNull()
    }

    private fun sourceFrom(extras: Bundle?): PlaybackSource? =
        when (extras?.getString(SOURCE_TYPE_KEY)) {
            SOURCE_DEMO -> PlaybackSource.FoundationDemo
            SOURCE_LOCAL -> extras.getString(SOURCE_VALUE_KEY)?.takeIf(String::isNotBlank)?.let(PlaybackSource::ImportedLocal)
            SOURCE_KUGOU -> extras.getString(SOURCE_VALUE_KEY)?.takeIf(String::isNotBlank)?.let(PlaybackSource::Kugou)
            else -> null
        }
}
