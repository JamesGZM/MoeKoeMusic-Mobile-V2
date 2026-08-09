package cn.james.music.feature.search

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface SearchSongArtworkUi {
    data class Remote(
        val url: String?,
    ) : SearchSongArtworkUi

    data class Resource(
        @param:DrawableRes val drawableRes: Int,
    ) : SearchSongArtworkUi
}

@Immutable
internal data class SearchSongUiModel(
    val id: String,
    val title: String,
    val artistName: String,
    val albumTitle: String?,
    val artwork: SearchSongArtworkUi,
    val badge: SearchSongBadge? = null,
    val isPlaying: Boolean = false,
)

internal sealed interface SearchProblemUi {
    data object Offline : SearchProblemUi

    data object Timeout : SearchProblemUi

    data object Connection : SearchProblemUi

    data object VerificationRequired : SearchProblemUi

    data object AuthenticationRequired : SearchProblemUi

    data object ServiceUnavailable : SearchProblemUi

    data object Protocol : SearchProblemUi

    data object SessionInitialization : SearchProblemUi
}

internal sealed interface SearchAction {
    data object Back : SearchAction

    data class QueryChanged(
        val value: String,
    ) : SearchAction

    data object ClearQuery : SearchAction

    data object Submit : SearchAction

    data class SelectCategory(
        val category: SearchCategory,
    ) : SearchAction

    data object LoadMore : SearchAction

    data class PlaySong(
        val id: String,
    ) : SearchAction

    data class MoreSong(
        val id: String,
    ) : SearchAction

    data object Voice : SearchAction

    data object FollowArtist : SearchAction

    data object ViewAllSongs : SearchAction

    data object ViewAllCollections : SearchAction

    data class OpenCollection(
        val id: String,
    ) : SearchAction
}
