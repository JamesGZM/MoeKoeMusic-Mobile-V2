package cn.james.music.feature.playlist

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState

    data object Empty : PlaylistDetailUiState

    data object Error : PlaylistDetailUiState

    data object Offline : PlaylistDetailUiState

    data class Content(
        val value: PlaylistDetailUi,
    ) : PlaylistDetailUiState
}

@Immutable
internal data class PlaylistDetailUi(
    val id: String,
    val title: String,
    val creatorName: String,
    val creatorVip: Boolean,
    val songCount: Int,
    val durationLabel: String,
    val description: String,
    @param:DrawableRes val artworkRes: Int,
    @param:DrawableRes val creatorAvatarRes: Int,
    val isFavorite: Boolean,
    val songs: List<PlaylistTrackUi>,
    val currentTrackId: String? = null,
)

@Immutable
internal data class PlaylistTrackUi(
    val id: String,
    val title: String,
    val artist: String,
    val durationLabel: String,
    val badge: PlaylistTrackBadgeUi?,
    @param:DrawableRes val artworkRes: Int,
)

internal enum class PlaylistTrackBadgeUi {
    HighQuality,
    MusicVideo,
}

internal val playlistDetailDesignPreview =
    PlaylistDetailUi(
        id = "design-preview-acg",
        title = "ACG 收藏",
        creatorName = "阿珏",
        creatorVip = true,
        songCount = 82,
        durationLabel = "5 小时 26 分",
        description = "精选动漫、游戏相关音乐，带你重温那些心动瞬间。",
        artworkRes = R.drawable.playlist_detail_cover,
        creatorAvatarRes = R.drawable.playlist_detail_avatar,
        isFavorite = false,
        currentTrackId = "track-5",
        songs =
            listOf(
                PlaylistTrackUi(
                    id = "track-1",
                    title = "これからも。",
                    artist = "水瀬祈（みなせ いのり）",
                    durationLabel = "4:28",
                    badge = PlaylistTrackBadgeUi.HighQuality,
                    artworkRes = R.drawable.playlist_track_seaside,
                ),
                PlaylistTrackUi(
                    id = "track-2",
                    title = "キライ…でも好き",
                    artist = "BRIGHT",
                    durationLabel = "4:15",
                    badge = PlaylistTrackBadgeUi.MusicVideo,
                    artworkRes = R.drawable.playlist_detail_cover,
                ),
                PlaylistTrackUi(
                    id = "track-3",
                    title = "クリームソーダとシャンデリア",
                    artist = "ねんね",
                    durationLabel = "3:59",
                    badge = PlaylistTrackBadgeUi.HighQuality,
                    artworkRes = R.drawable.playlist_track_headphones,
                ),
                PlaylistTrackUi(
                    id = "track-4",
                    title = "フェイスレス",
                    artist = "藍井エイル（Aoi Eir）",
                    durationLabel = "4:03",
                    badge = PlaylistTrackBadgeUi.HighQuality,
                    artworkRes = R.drawable.playlist_track_live,
                ),
                PlaylistTrackUi(
                    id = "track-5",
                    title = "unlasting",
                    artist = "LiSA",
                    durationLabel = "5:34",
                    badge = PlaylistTrackBadgeUi.MusicVideo,
                    artworkRes = R.drawable.playlist_track_healing,
                ),
                PlaylistTrackUi(
                    id = "track-6",
                    title = "IGNITE",
                    artist = "藍井エイル（Aoi Eir）",
                    durationLabel = "4:36",
                    badge = PlaylistTrackBadgeUi.HighQuality,
                    artworkRes = R.drawable.playlist_track_night,
                ),
                PlaylistTrackUi(
                    id = "track-7",
                    title = "adrenaline!!!",
                    artist = "TrySail",
                    durationLabel = "4:11",
                    badge = PlaylistTrackBadgeUi.HighQuality,
                    artworkRes = R.drawable.playlist_track_room,
                ),
                PlaylistTrackUi(
                    id = "track-8",
                    title = "アイドル",
                    artist = "YOASOBI",
                    durationLabel = "3:33",
                    badge = PlaylistTrackBadgeUi.HighQuality,
                    artworkRes = R.drawable.playlist_track_bamboo,
                ),
            ),
    )
