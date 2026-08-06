package cn.james.music.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.home.HomeAutomaticRefreshState
import cn.james.music.core.model.home.HomeContent
import cn.james.music.core.model.home.HomeRefreshProblem
import cn.james.music.core.model.home.HomeRefreshResult
import cn.james.music.core.model.home.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class HomeBannerUi(
    val id: String,
    val title: String?,
    val artworkUrl: String,
)

internal data class HomeSongUi(
    val id: String,
    val hash: String,
    val title: String,
    val artistName: String,
    val albumId: String?,
    val albumTitle: String?,
    val durationMs: Long,
    val artworkUrl: String?,
    val note: String?,
)

internal data class HomePlaylistUi(
    val id: String,
    val title: String,
    val artworkUrl: String?,
    val playCount: Long?,
)

internal data class HomeContentUi(
    val banners: List<HomeBannerUi>,
    val recommendations: List<HomeSongUi>,
    val playlists: List<HomePlaylistUi>,
)

internal enum class HomeProblemUi {
    SessionInitialization,
    Storage,
    Offline,
    Timeout,
    Connection,
    ServiceUnavailable,
    Rejected,
    Protocol,
}

internal sealed interface HomeContentUiState {
    data object Loading : HomeContentUiState

    data object Empty : HomeContentUiState

    data class Content(
        val value: HomeContentUi,
    ) : HomeContentUiState

    data class Failure(
        val problem: HomeProblemUi,
    ) : HomeContentUiState
}

internal data class HomeUiState(
    val content: HomeContentUiState = HomeContentUiState.Loading,
    val refreshing: Boolean = false,
    val refreshProblem: HomeProblemUi? = null,
)

@HiltViewModel
internal class HomeViewModel
    @Inject
    constructor(
        private val repository: HomeRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = mutableState.asStateFlow()
        private var refreshJob: Job? = null
        private var refreshGeneration = 0L

        init {
            viewModelScope.launch {
                repository.observeContent().collect { content ->
                    val contentState =
                        content?.toUiState()
                            ?: mutableState.value.content.takeIf { it is HomeContentUiState.Failure }
                            ?: HomeContentUiState.Loading
                    mutableState.value =
                        mutableState.value.copy(
                            content = contentState,
                            refreshProblem = null,
                        )
                }
            }
            viewModelScope.launch {
                repository.observeAutomaticRefresh().collect(::applyAutomaticRefresh)
            }
        }

        fun refresh() {
            refreshJob?.cancel()
            val generation = ++refreshGeneration
            mutableState.value = mutableState.value.copy(refreshing = true, refreshProblem = null)
            refreshJob =
                viewModelScope.launch {
                    val result = repository.refresh(force = true)
                    if (generation == refreshGeneration) applyRefreshResult(result)
                }
        }

        fun dismissRefreshProblem() {
            mutableState.value = mutableState.value.copy(refreshProblem = null)
        }

        private fun applyAutomaticRefresh(state: HomeAutomaticRefreshState) {
            when (state) {
                HomeAutomaticRefreshState.Idle -> {
                    Unit
                }

                HomeAutomaticRefreshState.Refreshing -> {
                    mutableState.value = mutableState.value.copy(refreshing = true, refreshProblem = null)
                }

                is HomeAutomaticRefreshState.Complete -> {
                    applyRefreshResult(state.result, keepRefreshing = refreshJob?.isActive == true)
                }
            }
        }

        private fun applyRefreshResult(
            result: HomeRefreshResult,
            keepRefreshing: Boolean = false,
        ) {
            mutableState.value =
                when (result) {
                    HomeRefreshResult.NotNeeded,
                    HomeRefreshResult.Superseded,
                    -> {
                        mutableState.value.copy(refreshing = keepRefreshing)
                    }

                    is HomeRefreshResult.Success -> {
                        val remoteState = result.content.toUiState()
                        val content =
                            if (!result.persisted && remoteState == HomeContentUiState.Empty &&
                                mutableState.value.content is HomeContentUiState.Content
                            ) {
                                mutableState.value.content
                            } else {
                                remoteState
                            }
                        mutableState.value.copy(content = content, refreshing = keepRefreshing, refreshProblem = null)
                    }

                    is HomeRefreshResult.Partial -> {
                        mutableState.value.copy(
                            content = result.content.toUiState(),
                            refreshing = keepRefreshing,
                            refreshProblem = result.problem.toUi(),
                        )
                    }

                    is HomeRefreshResult.Failure -> {
                        val problem = result.problem.toUi()
                        if (mutableState.value.content is HomeContentUiState.Content) {
                            mutableState.value.copy(refreshing = keepRefreshing, refreshProblem = problem)
                        } else {
                            mutableState.value.copy(
                                content = HomeContentUiState.Failure(problem),
                                refreshing = keepRefreshing,
                                refreshProblem = null,
                            )
                        }
                    }
                }
        }

        private fun HomeContent.toUiState(): HomeContentUiState {
            if (recommendations.isEmpty() && playlists.isEmpty()) return HomeContentUiState.Empty
            return HomeContentUiState.Content(
                HomeContentUi(
                    banners = banners.map { banner -> HomeBannerUi(banner.id, banner.title, banner.artworkUrl) },
                    recommendations =
                        recommendations.map { recommendation ->
                            val song = recommendation.song
                            HomeSongUi(
                                id = song.id,
                                hash = song.hash,
                                title = song.title,
                                artistName = song.artistName,
                                albumId = song.albumId,
                                albumTitle = song.albumTitle,
                                durationMs = song.durationMs,
                                artworkUrl = song.artworkUrl,
                                note = recommendation.note,
                            )
                        },
                    playlists =
                        playlists.map { playlist ->
                            HomePlaylistUi(playlist.id, playlist.title, playlist.artworkUrl, playlist.playCount)
                        },
                ),
            )
        }

        private fun HomeRefreshProblem.toUi(): HomeProblemUi =
            when (this) {
                HomeRefreshProblem.SessionInitialization -> HomeProblemUi.SessionInitialization
                HomeRefreshProblem.Storage -> HomeProblemUi.Storage
                HomeRefreshProblem.Offline -> HomeProblemUi.Offline
                HomeRefreshProblem.Timeout -> HomeProblemUi.Timeout
                HomeRefreshProblem.Connection -> HomeProblemUi.Connection
                HomeRefreshProblem.ServiceUnavailable -> HomeProblemUi.ServiceUnavailable
                HomeRefreshProblem.Rejected -> HomeProblemUi.Rejected
                HomeRefreshProblem.Protocol -> HomeProblemUi.Protocol
            }
    }
