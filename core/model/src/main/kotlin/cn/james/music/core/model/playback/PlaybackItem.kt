package cn.james.music.core.model.playback

enum class PlaybackMode {
    Sequential,
    RepeatAll,
    RepeatOne,
    Shuffle,
}

sealed interface PlaybackSource {
    data class ImportedLocal(
        val localMusicId: String,
    ) : PlaybackSource

    data class Kugou(
        val songHash: String,
    ) : PlaybackSource

    data object FoundationDemo : PlaybackSource
}

sealed interface PlaybackArtwork {
    val value: String

    data class Remote(
        override val value: String,
    ) : PlaybackArtwork {
        init {
            require(value.startsWith("https://")) { "Remote playback artwork must use HTTPS" }
        }
    }

    data class AppFile(
        override val value: String,
    ) : PlaybackArtwork {
        init {
            require(value.isNotBlank()) { "App playback artwork path must not be blank" }
            require(!value.startsWith('/') && ".." !in value.split('/')) {
                "App playback artwork must be an app-relative path"
            }
        }
    }
}

data class PlaybackItem(
    val id: String,
    val title: String,
    val artist: String,
    val albumTitle: String? = null,
    val source: PlaybackSource,
    val artwork: PlaybackArtwork? = null,
) {
    init {
        require(id.isNotBlank()) { "Playback item id must not be blank" }
        require(title.isNotBlank()) { "Playback item title must not be blank" }
        require(artist.isNotBlank()) { "Playback item artist must not be blank" }
    }
}
