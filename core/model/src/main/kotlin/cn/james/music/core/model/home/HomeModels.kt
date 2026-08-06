package cn.james.music.core.model.home

import cn.james.music.core.model.online.Song
import kotlinx.coroutines.flow.Flow

data class HomeContent(
    val banners: List<HomeBanner>,
    val recommendations: List<HomeRecommendation>,
    val playlists: List<HomePlaylist>,
    val updatedAtEpochMs: Long,
)

data class HomeBanner(
    val id: String,
    val title: String?,
    val artworkUrl: String,
)

data class HomeRecommendation(
    val song: Song,
    val note: String?,
)

data class HomePlaylist(
    val id: String,
    val title: String,
    val artworkUrl: String?,
    val playCount: Long?,
)

sealed interface HomeRefreshProblem {
    data object SessionInitialization : HomeRefreshProblem

    data object Storage : HomeRefreshProblem

    data object Offline : HomeRefreshProblem

    data object Timeout : HomeRefreshProblem

    data object Connection : HomeRefreshProblem

    data object ServiceUnavailable : HomeRefreshProblem

    data object Rejected : HomeRefreshProblem

    data object Protocol : HomeRefreshProblem
}

sealed interface HomeRefreshResult {
    data object NotNeeded : HomeRefreshResult

    data object Superseded : HomeRefreshResult

    data class Success(
        val content: HomeContent,
        val persisted: Boolean,
    ) : HomeRefreshResult

    data class Partial(
        val content: HomeContent,
        val problem: HomeRefreshProblem,
    ) : HomeRefreshResult

    data class Failure(
        val problem: HomeRefreshProblem,
    ) : HomeRefreshResult
}

sealed interface HomeAutomaticRefreshState {
    data object Idle : HomeAutomaticRefreshState

    data object Refreshing : HomeAutomaticRefreshState

    data class Complete(
        val result: HomeRefreshResult,
    ) : HomeAutomaticRefreshState
}

interface HomeRepository {
    fun observeContent(): Flow<HomeContent?>

    fun observeAutomaticRefresh(): Flow<HomeAutomaticRefreshState>

    suspend fun refresh(force: Boolean = false): HomeRefreshResult
}
