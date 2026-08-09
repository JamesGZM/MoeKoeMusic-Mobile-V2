package cn.james.music.features.app

import cn.james.music.core.model.online.SearchPage
import cn.james.music.core.model.online.SearchRepository
import cn.james.music.core.model.online.SearchResult
import cn.james.music.core.model.online.Song
import cn.james.music.feature.search.SearchAction
import cn.james.music.feature.search.SearchCategory
import cn.james.music.feature.search.SearchProblemUi
import cn.james.music.feature.search.SearchSongArtworkUi
import cn.james.music.feature.search.SearchSongUiModel
import cn.james.music.feature.search.SearchUiState
import cn.james.music.feature.search.SearchViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchStateMatrixKeepsIdleLoadingContentEmptyAndErrorDistinct() {
        val idle = SearchUiState()
        val loading = SearchUiState(query = "MoeKoe", submittedQuery = "MoeKoe", loading = true)
        val content =
            SearchUiState(
                query = "MoeKoe",
                submittedQuery = "MoeKoe",
                songs = listOf(SearchSongUiModel("song-1", "Title", "Artist", null, SearchSongArtworkUi.Remote(null))),
            )
        val empty = SearchUiState(query = "missing", submittedQuery = "missing")
        val error = SearchUiState(query = "MoeKoe", submittedQuery = "MoeKoe", error = SearchProblemUi.Offline)

        assertFalse(idle.hasSearched)
        assertTrue(loading.hasSearched && loading.loading)
        assertTrue(content.hasSearched && content.songs.isNotEmpty())
        assertTrue(empty.hasSearched && empty.songs.isEmpty() && empty.error == null)
        assertTrue(error.hasSearched && error.error != null)
    }

    @Test
    fun blankKeywordDoesNotStartARequest() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.onAction(SearchAction.QueryChanged("   "))
            viewModel.onAction(SearchAction.Submit)
            advanceUntilIdle()

            assertEquals(0, repository.callCount)
            assertFalse(viewModel.state.value.hasSearched)
        }

    @Test
    fun successfulSearchPublishesContentAndPagination() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.onAction(SearchAction.QueryChanged("  MoeKoe  "))
            viewModel.onAction(SearchAction.Submit)
            runCurrent()
            repository.requests
                .single()
                .result
                .complete(success("song-1", hasMore = true))
            advanceUntilIdle()

            assertEquals("MoeKoe", viewModel.state.value.submittedQuery)
            assertEquals(
                listOf("song-1"),
                viewModel.state.value.songs
                    .map(SearchSongUiModel::id),
            )
            assertEquals(1, viewModel.state.value.page)
            assertEquals(true, viewModel.state.value.hasMore)
        }

    @Test
    fun categorySelectionOnlyChangesPresentationState() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.onAction(SearchAction.QueryChanged("初音未来"))
            viewModel.onAction(SearchAction.SelectCategory(SearchCategory.Playlists))

            assertEquals(SearchCategory.Playlists, viewModel.state.value.selectedCategory)
            assertEquals("初音未来", viewModel.state.value.query)
            assertEquals(0, repository.callCount)
        }

    @Test
    fun olderRequestCannotReplaceNewerSearch() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.onAction(SearchAction.QueryChanged("first"))
            viewModel.onAction(SearchAction.Submit)
            runCurrent()
            val first = repository.requests.single()

            viewModel.onAction(SearchAction.QueryChanged("second"))
            viewModel.onAction(SearchAction.Submit)
            runCurrent()
            val second = repository.requests.last()
            second.result.complete(success("new-song"))
            advanceUntilIdle()
            first.result.complete(success("old-song"))
            advanceUntilIdle()

            assertEquals("second", viewModel.state.value.submittedQuery)
            assertEquals(
                listOf("new-song"),
                viewModel.state.value.songs
                    .map(SearchSongUiModel::id),
            )
        }

    @Test
    fun paginationAtomicallyMergesCompleteUiRowsAndDeduplicatesByStableId() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.onAction(SearchAction.QueryChanged("MoeKoe"))
            viewModel.onAction(SearchAction.Submit)
            runCurrent()
            repository.requests
                .single()
                .result
                .complete(success(listOf(song("song-1", "hash-old", "Old title")), hasMore = true))
            advanceUntilIdle()

            viewModel.onAction(SearchAction.LoadMore)
            runCurrent()
            repository.requests
                .last()
                .result
                .complete(
                    success(
                        listOf(
                            song("song-1", "hash-new", "New title"),
                            song("song-2", "hash-song-2", "Title song-2"),
                        ),
                        page = 2,
                    ),
                )
            advanceUntilIdle()

            assertEquals(
                listOf("song-1", "song-2"),
                viewModel.state.value.songs
                    .map(SearchSongUiModel::id),
            )
            assertEquals(
                "Title song-2",
                viewModel.state.value.songs
                    .last()
                    .title,
            )
            assertEquals(
                "Old title",
                viewModel.state.value.songs
                    .first()
                    .title,
            )
            assertEquals("hash-old", viewModel.songFor("song-1")?.hash)
            assertEquals("hash-song-2", viewModel.songFor("song-2")?.hash)
        }

    @Test
    fun stateDoesNotLeakDomainSongAndRouteCanResolveStableId() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.onAction(SearchAction.QueryChanged("MoeKoe"))
            viewModel.onAction(SearchAction.Submit)
            runCurrent()
            repository.requests
                .single()
                .result
                .complete(success("song-1"))
            advanceUntilIdle()

            assertTrue(
                viewModel.state.value.songs
                    .none { it::class.java == Song::class.java },
            )
            assertEquals("hash-song-1", viewModel.songFor("song-1")?.hash)
        }

    private fun success(
        vararg ids: String,
        hasMore: Boolean = false,
        page: Int = 1,
    ) = success(ids.map { id -> song(id) }, hasMore, page)

    private fun success(
        items: List<Song>,
        hasMore: Boolean = false,
        page: Int = 1,
    ) = SearchResult.Success(
        SearchPage(
            items = items,
            page = page,
            totalCount = if (hasMore) 31 else items.size,
            hasMore = hasMore,
        ),
    )

    private fun song(
        id: String,
        hash: String = "hash-$id",
        title: String = "Title $id",
    ) = Song(id, hash, title, "Artist", null, null, 120_000, null)

    private class ControlledSearchRepository : SearchRepository {
        val requests = mutableListOf<Request>()
        val callCount: Int get() = requests.size

        override suspend fun searchSongs(
            keyword: String,
            page: Int,
            pageSize: Int,
        ): SearchResult {
            val request = Request(keyword, page)
            requests += request
            return request.result.await()
        }
    }

    private data class Request(
        val keyword: String,
        val page: Int,
        val result: CompletableDeferred<SearchResult> = CompletableDeferred(),
    )
}
