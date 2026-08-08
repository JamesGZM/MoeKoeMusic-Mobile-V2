package cn.james.music.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.online.SearchError
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
    val songs: List<Song> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: SearchError? = null,
    val selectedCategory: SearchCategory = SearchCategory.Overview,
    val artist: SearchArtistUi? = null,
    val collections: List<SearchCollectionUi> = emptyList(),
    val songBadges: Map<String, SearchSongBadge> = emptyMap(),
    val songArtwork: Map<String, Int> = emptyMap(),
    val playingSongId: String? = null,
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

        fun updateQuery(value: String) {
            mutableState.value = mutableState.value.copy(query = value)
        }

        fun selectCategory(category: SearchCategory) {
            mutableState.value = mutableState.value.copy(selectedCategory = category)
        }

        fun submit() {
            val keyword = mutableState.value.query.trim()
            if (keyword.isEmpty()) return
            searchJob?.cancel()
            val generation = ++requestGeneration
            mutableState.value = SearchUiState(query = mutableState.value.query, submittedQuery = keyword, loading = true)
            searchJob =
                viewModelScope.launch {
                    val result = repository.searchSongs(keyword, page = 1)
                    if (generation != requestGeneration) return@launch
                    mutableState.value = result.toUiState(query = mutableState.value.query, submittedQuery = keyword)
                }
        }

        fun loadMore() {
            val current = mutableState.value
            if (current.loading || current.loadingMore || !current.hasMore || current.submittedQuery.isEmpty()) return
            val generation = requestGeneration
            mutableState.value = current.copy(loadingMore = true, error = null)
            searchJob =
                viewModelScope.launch {
                    when (val result = repository.searchSongs(current.submittedQuery, page = current.page + 1)) {
                        is SearchResult.Failure -> {
                            if (generation == requestGeneration) {
                                mutableState.value = mutableState.value.copy(loadingMore = false, error = result.error)
                            }
                        }

                        is SearchResult.Success -> {
                            if (generation == requestGeneration) {
                                mutableState.value =
                                    mutableState.value.copy(
                                        songs = (current.songs + result.page.items).distinctBy(Song::id),
                                        page = result.page.page,
                                        hasMore = result.page.hasMore,
                                        loadingMore = false,
                                    )
                            }
                        }
                    }
                }
        }

        private fun SearchResult.toUiState(
            query: String,
            submittedQuery: String,
        ): SearchUiState =
            when (this) {
                is SearchResult.Failure -> {
                    SearchUiState(query = query, submittedQuery = submittedQuery, error = error)
                }

                is SearchResult.Success -> {
                    SearchUiState(
                        query = query,
                        submittedQuery = submittedQuery,
                        songs = page.items,
                        page = page.page,
                        hasMore = page.hasMore,
                    )
                }
            }
    }
