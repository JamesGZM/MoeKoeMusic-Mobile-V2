package cn.james.music.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface HomeArtworkUi {
    data class Remote(
        val url: String,
    ) : HomeArtworkUi

    data class Resource(
        @param:DrawableRes val drawableRes: Int,
    ) : HomeArtworkUi

    data object Placeholder : HomeArtworkUi
}

@Immutable
internal enum class HomeSongBadgeUi {
    Quality,
    MusicVideo,
}

@Immutable
internal data class HomeSongUi(
    val id: String,
    val title: String,
    val artistName: String,
    val artwork: HomeArtworkUi,
    val badge: HomeSongBadgeUi? = null,
)

@Immutable
internal sealed interface HomePlaylistSupportingUi {
    data class PlayCount(
        val count: Long,
    ) : HomePlaylistSupportingUi

    data class Text(
        val value: String,
    ) : HomePlaylistSupportingUi
}

@Immutable
internal data class HomePlaylistUi(
    val id: String,
    val title: String,
    val artwork: HomeArtworkUi,
    val supporting: HomePlaylistSupportingUi? = null,
)

internal sealed interface HomeAction {
    data object Search : HomeAction

    data object Refresh : HomeAction

    data object DismissProblem : HomeAction

    data class PlaySong(
        val id: String,
    ) : HomeAction
}
