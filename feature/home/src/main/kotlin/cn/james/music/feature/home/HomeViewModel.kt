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
        private var songsById = emptyMap<String, cn.james.music.core.model.online.Song>()

        fun onAction(action: HomeAction) {
            when (action) {
                HomeAction.Refresh -> refresh()

                HomeAction.DismissProblem -> dismissRefreshProblem()

                HomeAction.Search,
                is HomeAction.PlaySong,
                -> Unit
            }
        }

        fun songFor(id: String): cn.james.music.core.model.online.Song? = songsById[id]

        init {
            viewModelScope.launch {
                repository.observeContent().collect { content ->
                    val snapshot =
                        content?.toSnapshot()
                            ?: HomeContentSnapshot(
                                content =
                                    mutableState.value.content.takeIf { it is HomeContentUiState.Failure }
                                        ?: HomeContentUiState.Loading,
                                songsById = emptyMap(),
                            )
                    publish(snapshot, refreshProblem = null)
                }
            }
            viewModelScope.launch {
                repository.observeAutomaticRefresh().collect(::applyAutomaticRefresh)
            }
        }

        private fun refresh() {
            refreshJob?.cancel()
            val generation = ++refreshGeneration
            mutableState.value = mutableState.value.copy(refreshing = true, refreshProblem = null)
            refreshJob =
                viewModelScope.launch {
                    val result = repository.refresh(force = true)
                    if (generation == refreshGeneration) applyRefreshResult(result)
                }
        }

        private fun dismissRefreshProblem() {
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
            when (result) {
                HomeRefreshResult.NotNeeded,
                HomeRefreshResult.Superseded,
                -> {
                    mutableState.value = mutableState.value.copy(refreshing = keepRefreshing)
                }

                is HomeRefreshResult.Success -> {
                    val snapshot = result.content.toSnapshot()
                    if (!result.persisted && snapshot.content == HomeContentUiState.Empty &&
                        mutableState.value.content is HomeContentUiState.Content
                    ) {
                        mutableState.value = mutableState.value.copy(refreshing = keepRefreshing, refreshProblem = null)
                    } else {
                        publish(snapshot, refreshing = keepRefreshing, refreshProblem = null)
                    }
                }

                is HomeRefreshResult.Partial -> {
                    publish(
                        result.content.toSnapshot(),
                        refreshing = keepRefreshing,
                        refreshProblem = result.problem.toUi(),
                    )
                }

                is HomeRefreshResult.Failure -> {
                    val problem = result.problem.toUi()
                    if (mutableState.value.content is HomeContentUiState.Content) {
                        mutableState.value = mutableState.value.copy(refreshing = keepRefreshing, refreshProblem = problem)
                    } else {
                        publish(
                            HomeContentSnapshot(HomeContentUiState.Failure(problem), emptyMap()),
                            refreshing = keepRefreshing,
                            refreshProblem = null,
                        )
                    }
                }
            }
        }

        private fun publish(
            snapshot: HomeContentSnapshot,
            refreshing: Boolean = mutableState.value.refreshing,
            refreshProblem: HomeProblemUi?,
        ) {
            songsById = snapshot.songsById
            mutableState.value =
                mutableState.value.copy(
                    content = snapshot.content,
                    refreshing = refreshing,
                    refreshProblem = refreshProblem,
                )
        }

        private fun HomeContent.toSnapshot(): HomeContentSnapshot {
            if (recommendations.isEmpty() && playlists.isEmpty()) {
                return HomeContentSnapshot(HomeContentUiState.Empty, emptyMap())
            }
            return HomeContentSnapshot(
                content =
                    HomeContentUiState.Content(
                        HomeContentUi(
                            banners = banners.map { banner -> HomeBannerUi(banner.id, banner.title, banner.artworkUrl) },
                            recommendations =
                                recommendations.map { recommendation ->
                                    val song = recommendation.song
                                    HomeSongUi(
                                        id = song.id,
                                        title = song.title,
                                        artistName = song.artistName,
                                        artwork = song.artworkUrl?.let(HomeArtworkUi::Remote) ?: HomeArtworkUi.Placeholder,
                                    )
                                },
                            playlists =
                                playlists.map { playlist ->
                                    HomePlaylistUi(
                                        id = playlist.id,
                                        title = playlist.title,
                                        artwork = playlist.artworkUrl?.let(HomeArtworkUi::Remote) ?: HomeArtworkUi.Placeholder,
                                        supporting = playlist.playCount?.let(HomePlaylistSupportingUi::PlayCount),
                                    )
                                },
                        ),
                    ),
                songsById = recommendations.associate { recommendation -> recommendation.song.id to recommendation.song },
            )
        }

        private data class HomeContentSnapshot(
            val content: HomeContentUiState,
            val songsById: Map<String, cn.james.music.core.model.online.Song>,
        )

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
