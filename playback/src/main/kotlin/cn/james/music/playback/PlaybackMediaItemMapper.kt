package cn.james.music.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource

internal object PlaybackMediaItemMapper {
    private const val SOURCE_TYPE_KEY = "cn.james.music.playback.SOURCE_TYPE"
    private const val SOURCE_VALUE_KEY = "cn.james.music.playback.SOURCE_VALUE"
    private const val SOURCE_DEMO = "foundation_demo"
    private const val SOURCE_LOCAL = "imported_local"
    private const val SOURCE_KUGOU = "kugou"
    private const val ARTWORK_TYPE_KEY = "cn.james.music.playback.ARTWORK_TYPE"
    private const val ARTWORK_VALUE_KEY = "cn.james.music.playback.ARTWORK_VALUE"
    private const val ARTWORK_REMOTE = "remote"
    private const val ARTWORK_APP_FILE = "app_file"
    private const val RESOLVED_QUALITY_KEY = "cn.james.music.playback.RESOLVED_QUALITY"

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
        item.artwork?.let { artwork ->
            extras.putString(
                ARTWORK_TYPE_KEY,
                when (artwork) {
                    is PlaybackArtwork.Remote -> ARTWORK_REMOTE
                    is PlaybackArtwork.AppFile -> ARTWORK_APP_FILE
                },
            )
            extras.putString(ARTWORK_VALUE_KEY, artwork.value)
        }
        val metadata =
            MediaMetadata
                .Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setAlbumTitle(item.albumTitle)
                .setExtras(extras)
                .apply {
                    (item.artwork as? PlaybackArtwork.Remote)?.let { artwork ->
                        setArtworkUri(Uri.parse(artwork.value))
                    }
                }.build()
        return MediaItem
            .Builder()
            .setMediaId(item.id)
            .setMediaMetadata(metadata)
            .build()
    }

    fun withUri(
        request: MediaItem,
        uri: Uri,
        quality: ResolvedPlaybackQuality?,
    ): MediaItem {
        val extras = Bundle(request.mediaMetadata.extras ?: Bundle())
        quality?.let { resolved ->
            extras.putString(RESOLVED_QUALITY_KEY, resolved.runtimeValue)
        }
        val metadata =
            request.mediaMetadata
                .buildUpon()
                .setExtras(extras)
                .build()
        return request
            .buildUpon()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun resolvedQuality(item: MediaItem): ResolvedPlaybackQuality? =
        ResolvedPlaybackQuality.fromRuntimeValue(item.mediaMetadata.extras?.getString(RESOLVED_QUALITY_KEY))

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
                artwork = artworkFrom(metadata.extras),
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

    private fun artworkFrom(extras: Bundle?): PlaybackArtwork? {
        val value = extras?.getString(ARTWORK_VALUE_KEY)?.takeIf(String::isNotBlank) ?: return null
        return runCatching {
            when (extras.getString(ARTWORK_TYPE_KEY)) {
                ARTWORK_REMOTE -> PlaybackArtwork.Remote(value)
                ARTWORK_APP_FILE -> PlaybackArtwork.AppFile(value)
                else -> null
            }
        }.getOrNull()
    }
}
