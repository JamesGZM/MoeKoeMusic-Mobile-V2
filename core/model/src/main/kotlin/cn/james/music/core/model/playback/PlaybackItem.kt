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

data class PlaybackItem(
    val id: String,
    val title: String,
    val artist: String,
    val albumTitle: String? = null,
    val source: PlaybackSource,
) {
    init {
        require(id.isNotBlank()) { "Playback item id must not be blank" }
        require(title.isNotBlank()) { "Playback item title must not be blank" }
        require(artist.isNotBlank()) { "Playback item artist must not be blank" }
    }
}
