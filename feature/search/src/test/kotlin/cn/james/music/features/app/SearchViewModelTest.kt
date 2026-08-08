package cn.james.music.features.app

import cn.james.music.core.model.online.SearchPage
import cn.james.music.core.model.online.SearchRepository
import cn.james.music.core.model.online.SearchResult
import cn.james.music.core.model.online.Song
import cn.james.music.feature.search.SearchCategory
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
        val content = SearchUiState(query = "MoeKoe", submittedQuery = "MoeKoe", songs = success("song-1").page.items)
        val empty = SearchUiState(query = "missing", submittedQuery = "missing")
        val error = SearchUiState(query = "MoeKoe", submittedQuery = "MoeKoe", error = cn.james.music.core.model.online.SearchError.Offline)

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

            viewModel.updateQuery("   ")
            viewModel.submit()
            advanceUntilIdle()

            assertEquals(0, repository.callCount)
            assertFalse(viewModel.state.value.hasSearched)
        }

    @Test
    fun successfulSearchPublishesContentAndPagination() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.updateQuery("  MoeKoe  ")
            viewModel.submit()
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
                    .map(Song::id),
            )
            assertEquals(1, viewModel.state.value.page)
            assertEquals(true, viewModel.state.value.hasMore)
        }

    @Test
    fun categorySelectionOnlyChangesPresentationState() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.updateQuery("初音未来")
            viewModel.selectCategory(SearchCategory.Playlists)

            assertEquals(SearchCategory.Playlists, viewModel.state.value.selectedCategory)
            assertEquals("初音未来", viewModel.state.value.query)
            assertEquals(0, repository.callCount)
        }

    @Test
    fun olderRequestCannotReplaceNewerSearch() =
        runTest(dispatcher) {
            val repository = ControlledSearchRepository()
            val viewModel = SearchViewModel(repository)

            viewModel.updateQuery("first")
            viewModel.submit()
            runCurrent()
            val first = repository.requests.single()

            viewModel.updateQuery("second")
            viewModel.submit()
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
                    .map(Song::id),
            )
        }

    private fun success(
        id: String,
        hasMore: Boolean = false,
    ) = SearchResult.Success(
        SearchPage(
            items = listOf(Song(id, "hash-$id", "Title", "Artist", null, null, 120_000, null)),
            page = 1,
            totalCount = if (hasMore) 31 else 1,
            hasMore = hasMore,
        ),
    )

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
