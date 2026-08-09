package cn.james.music.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.online.SearchRepository
import cn.james.music.core.model.online.SearchResult
import cn.james.music.core.model.online.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class SearchCategory {
    Overview,
    Songs,
    Playlists,
    Albums,
    Artists,
    Mv,
}

internal data class SearchArtistUi(
    val name: String,
    val badge: String?,
    val stats: String,
    val representativeWorks: String,
    val artworkRes: Int,
)

internal data class SearchCollectionUi(
    val id: String,
    val title: String,
    val metadata: String,
    val artworkRes: Int,
)

internal enum class SearchSongBadge { Quality, Mv }

internal data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val songs: List<SearchSongUiModel> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: SearchProblemUi? = null,
    val selectedCategory: SearchCategory = SearchCategory.Overview,
    val artist: SearchArtistUi? = null,
    val collections: List<SearchCollectionUi> = emptyList(),
) {
    val hasSearched: Boolean get() = submittedQuery.isNotEmpty()
}

@HiltViewModel
internal class SearchViewModel
    @Inject
    constructor(
        private val repository: SearchRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SearchUiState())
        val state: StateFlow<SearchUiState> = mutableState.asStateFlow()
        private var searchJob: Job? = null
        private var requestGeneration = 0L
        private var songsById: Map<String, Song> = emptyMap()

        fun onAction(action: SearchAction) {
            when (action) {
                is SearchAction.QueryChanged -> updateQuery(action.value)

                SearchAction.ClearQuery -> updateQuery("")

                SearchAction.Submit -> submit()

                is SearchAction.SelectCategory -> selectCategory(action.category)

                SearchAction.LoadMore -> loadMore()

                SearchAction.Back,
                is SearchAction.PlaySong,
                is SearchAction.MoreSong,
                SearchAction.Voice,
                SearchAction.FollowArtist,
                SearchAction.ViewAllSongs,
                SearchAction.ViewAllCollections,
                is SearchAction.OpenCollection,
                -> Unit
            }
        }

        fun songFor(id: String): Song? = songsById[id]

        private fun updateQuery(value: String) {
            mutableState.value = mutableState.value.copy(query = value)
        }

        private fun selectCategory(category: SearchCategory) {
            mutableState.value = mutableState.value.copy(selectedCategory = category)
        }

        private fun submit() {
            val keyword = mutableState.value.query.trim()
            if (keyword.isEmpty()) return
            searchJob?.cancel()
            val generation = ++requestGeneration
            songsById = emptyMap()
            mutableState.value = SearchUiState(query = mutableState.value.query, submittedQuery = keyword, loading = true)
            searchJob =
                viewModelScope.launch {
                    val result = repository.searchSongs(keyword, page = 1)
                    if (generation != requestGeneration) return@launch
                    val snapshot = result.toSnapshot(query = mutableState.value.query, submittedQuery = keyword)
                    songsById = snapshot.songsById
                    mutableState.value = snapshot.state
                }
        }

        private fun loadMore() {
            val current = mutableState.value
            if (current.loading || current.loadingMore || !current.hasMore || current.submittedQuery.isEmpty()) return
            val generation = requestGeneration
            mutableState.value = current.copy(loadingMore = true, error = null)
            searchJob =
                viewModelScope.launch {
                    when (val result = repository.searchSongs(current.submittedQuery, page = current.page + 1)) {
                        is SearchResult.Failure -> {
                            if (generation == requestGeneration) {
                                mutableState.value = mutableState.value.copy(loadingMore = false, error = result.error.toUi())
                            }
                        }

                        is SearchResult.Success -> {
                            if (generation == requestGeneration) {
                                val snapshot =
                                    SearchSongsSnapshot.from(
                                        current.songs.mapNotNull { song -> songsById[song.id] } + result.page.items,
                                    )
                                songsById = snapshot.songsById
                                mutableState.value =
                                    mutableState.value.copy(
                                        songs = snapshot.rows,
                                        page = result.page.page,
                                        hasMore = result.page.hasMore,
                                        loadingMore = false,
                                    )
                            }
                        }
                    }
                }
        }

        private fun SearchResult.toSnapshot(
            query: String,
            submittedQuery: String,
        ): SearchSnapshot =
            when (this) {
                is SearchResult.Failure -> {
                    SearchSnapshot(
                        state = SearchUiState(query = query, submittedQuery = submittedQuery, error = error.toUi()),
                        songsById = emptyMap(),
                    )
                }

                is SearchResult.Success -> {
                    val songs = SearchSongsSnapshot.from(page.items)
                    SearchSnapshot(
                        state =
                            SearchUiState(
                                query = query,
                                submittedQuery = submittedQuery,
                                songs = songs.rows,
                                page = page.page,
                                hasMore = page.hasMore,
                            ),
                        songsById = songs.songsById,
                    )
                }
            }

        private data class SearchSnapshot(
            val state: SearchUiState,
            val songsById: Map<String, Song>,
        )

        private data class SearchSongsSnapshot(
            val rows: List<SearchSongUiModel>,
            val songsById: Map<String, Song>,
        ) {
            companion object {
                fun from(songs: List<Song>): SearchSongsSnapshot {
                    val uniqueSongs = songs.distinctBy(Song::id)
                    return SearchSongsSnapshot(
                        rows = uniqueSongs.map(Song::toSearchUiModel),
                        songsById = uniqueSongs.associateBy(Song::id),
                    )
                }
            }
        }

        private fun cn.james.music.core.model.online.SearchError.toUi(): SearchProblemUi =
            when (this) {
                cn.james.music.core.model.online.SearchError.Offline -> SearchProblemUi.Offline
                cn.james.music.core.model.online.SearchError.Timeout -> SearchProblemUi.Timeout
                cn.james.music.core.model.online.SearchError.Connection -> SearchProblemUi.Connection
                cn.james.music.core.model.online.SearchError.VerificationRequired -> SearchProblemUi.VerificationRequired
                cn.james.music.core.model.online.SearchError.AuthenticationRequired -> SearchProblemUi.AuthenticationRequired
                cn.james.music.core.model.online.SearchError.ServiceUnavailable -> SearchProblemUi.ServiceUnavailable
                cn.james.music.core.model.online.SearchError.Protocol -> SearchProblemUi.Protocol
                cn.james.music.core.model.online.SearchError.SessionInitialization -> SearchProblemUi.SessionInitialization
            }
    }

private fun Song.toSearchUiModel() =
    SearchSongUiModel(
        id = id,
        title = title,
        artistName = artistName,
        albumTitle = albumTitle,
        artwork = SearchSongArtworkUi.Remote(artworkUrl),
    )
