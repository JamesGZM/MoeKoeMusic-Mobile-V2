package cn.james.music.feature.home

import cn.james.music.core.model.home.HomeAutomaticRefreshState
import cn.james.music.core.model.home.HomeBanner
import cn.james.music.core.model.home.HomeContent
import cn.james.music.core.model.home.HomePlaylist
import cn.james.music.core.model.home.HomeRecommendation
import cn.james.music.core.model.home.HomeRefreshProblem
import cn.james.music.core.model.home.HomeRefreshResult
import cn.james.music.core.model.home.HomeRepository
import cn.james.music.core.model.online.Song
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
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
    fun cachedContentIsMappedBeforeAutomaticRefreshCompletes() =
        runTest(dispatcher) {
            val repository = FakeHomeRepository(content("cached"))
            val viewModel = HomeViewModel(repository)
            runCurrent()

            assertEquals(
                "cached",
                viewModel
                    .content()
                    .recommendations
                    .single()
                    .title,
            )

            repository.automatic.value = HomeAutomaticRefreshState.Refreshing
            runCurrent()
            assertTrue(viewModel.state.value.refreshing)

            repository.automatic.value = HomeAutomaticRefreshState.Complete(HomeRefreshResult.NotNeeded)
            runCurrent()
            assertFalse(viewModel.state.value.refreshing)
        }

    @Test
    fun firstLoadFailureUsesBlockingFailureState() =
        runTest(dispatcher) {
            val repository = FakeHomeRepository(null)
            val viewModel = HomeViewModel(repository)
            runCurrent()

            repository.automatic.value =
                HomeAutomaticRefreshState.Complete(HomeRefreshResult.Failure(HomeRefreshProblem.Offline))
            runCurrent()

            assertEquals(HomeContentUiState.Failure(HomeProblemUi.Offline), viewModel.state.value.content)
            assertNull(viewModel.state.value.refreshProblem)
        }

    @Test
    fun initialSessionFailureIsNotOverwrittenByFollowingMissingCache() =
        runTest(dispatcher) {
            val repository = FakeHomeRepository(null, emitInitialFailureBeforeContent = true)
            val viewModel = HomeViewModel(repository)
            runCurrent()

            assertEquals(
                HomeContentUiState.Failure(HomeProblemUi.SessionInitialization),
                viewModel.state.value.content,
            )
        }

    @Test
    fun automaticFailureKeepsCachedContentAsNonBlockingProblem() =
        runTest(dispatcher) {
            val repository = FakeHomeRepository(content("cached"))
            val viewModel = HomeViewModel(repository)
            runCurrent()

            repository.automatic.value =
                HomeAutomaticRefreshState.Complete(HomeRefreshResult.Failure(HomeRefreshProblem.Timeout))
            runCurrent()

            assertEquals(
                "cached",
                viewModel
                    .content()
                    .recommendations
                    .single()
                    .title,
            )
            assertEquals(HomeProblemUi.Timeout, viewModel.state.value.refreshProblem)

            viewModel.dismissRefreshProblem()
            assertNull(viewModel.state.value.refreshProblem)
        }

    @Test
    fun partialResultWithoutCacheBecomesContentWithWeakProblem() =
        runTest(dispatcher) {
            val repository = FakeHomeRepository(null)
            val viewModel = HomeViewModel(repository)
            runCurrent()

            repository.automatic.value =
                HomeAutomaticRefreshState.Complete(
                    HomeRefreshResult.Partial(content("partial"), HomeRefreshProblem.Connection),
                )
            runCurrent()

            assertEquals(
                "partial",
                viewModel
                    .content()
                    .recommendations
                    .single()
                    .title,
            )
            assertEquals(HomeProblemUi.Connection, viewModel.state.value.refreshProblem)
        }

    @Test
    fun successfulEmptyResultUsesEmptyStateButDoesNotReplaceCachedContent() =
        runTest(dispatcher) {
            val empty = HomeContent(emptyList(), emptyList(), emptyList(), 2)
            val emptyRepository = FakeHomeRepository(null)
            val emptyViewModel = HomeViewModel(emptyRepository)
            runCurrent()
            emptyRepository.automatic.value =
                HomeAutomaticRefreshState.Complete(HomeRefreshResult.Success(empty, persisted = false))
            runCurrent()
            assertEquals(HomeContentUiState.Empty, emptyViewModel.state.value.content)

            val cachedRepository = FakeHomeRepository(content("cached"))
            val cachedViewModel = HomeViewModel(cachedRepository)
            runCurrent()
            cachedRepository.automatic.value =
                HomeAutomaticRefreshState.Complete(HomeRefreshResult.Success(empty, persisted = false))
            runCurrent()
            assertEquals(
                "cached",
                cachedViewModel
                    .content()
                    .recommendations
                    .single()
                    .title,
            )
        }

    @Test
    fun explicitRefreshPublishesLatestSuccessAndCancelsOlderRequest() =
        runTest(dispatcher) {
            val repository = FakeHomeRepository(content("cached"))
            val viewModel = HomeViewModel(repository)
            runCurrent()

            viewModel.refresh()
            runCurrent()
            val first = repository.refreshRequests.single()
            assertTrue(viewModel.state.value.refreshing)

            viewModel.refresh()
            runCurrent()
            val second = repository.refreshRequests.last()
            second.complete(HomeRefreshResult.Success(content("new"), persisted = true))
            runCurrent()
            first.complete(HomeRefreshResult.Success(content("old"), persisted = true))
            runCurrent()

            assertEquals(
                "new",
                viewModel
                    .content()
                    .recommendations
                    .single()
                    .title,
            )
            assertFalse(viewModel.state.value.refreshing)
            assertEquals(2, repository.refreshRequests.size)
        }

    @Test
    fun identityPartitionSwitchToMissingCacheClearsPreviousContent() =
        runTest(dispatcher) {
            val repository = FakeHomeRepository(content("account-a"))
            val viewModel = HomeViewModel(repository)
            runCurrent()
            assertEquals(
                "account-a",
                viewModel
                    .content()
                    .recommendations
                    .single()
                    .title,
            )

            repository.content.value = null
            repository.automatic.value = HomeAutomaticRefreshState.Refreshing
            runCurrent()

            assertEquals(HomeContentUiState.Loading, viewModel.state.value.content)
            assertTrue(viewModel.state.value.refreshing)
        }

    @Test
    fun identityAutomaticResultIsAppliedWhileOlderManualRefreshIsActive() =
        runTest(dispatcher) {
            val repository = FakeHomeRepository(content("account-a"))
            val viewModel = HomeViewModel(repository)
            runCurrent()

            viewModel.refresh()
            runCurrent()
            val accountARefresh = repository.refreshRequests.single()

            repository.content.value = null
            repository.automatic.value =
                HomeAutomaticRefreshState.Complete(
                    HomeRefreshResult.Partial(content("account-b"), HomeRefreshProblem.Offline),
                )
            runCurrent()

            assertEquals(
                "account-b",
                viewModel
                    .content()
                    .recommendations
                    .single()
                    .title,
            )
            assertEquals(HomeProblemUi.Offline, viewModel.state.value.refreshProblem)
            assertTrue(viewModel.state.value.refreshing)

            accountARefresh.complete(HomeRefreshResult.Superseded)
            runCurrent()
            assertFalse(viewModel.state.value.refreshing)
        }

    private fun HomeViewModel.content(): HomeContentUi = (state.value.content as HomeContentUiState.Content).value

    private class FakeHomeRepository(
        initialContent: HomeContent?,
        private val emitInitialFailureBeforeContent: Boolean = false,
    ) : HomeRepository {
        val content = MutableStateFlow(initialContent)
        val automatic = MutableStateFlow<HomeAutomaticRefreshState>(HomeAutomaticRefreshState.Idle)
        val refreshRequests = mutableListOf<CompletableDeferred<HomeRefreshResult>>()

        override fun observeContent(): Flow<HomeContent?> =
            if (emitInitialFailureBeforeContent) {
                flow {
                    automatic.value =
                        HomeAutomaticRefreshState.Complete(
                            HomeRefreshResult.Failure(HomeRefreshProblem.SessionInitialization),
                        )
                    yield()
                    emitAll(content)
                }
            } else {
                content
            }

        override fun observeAutomaticRefresh(): Flow<HomeAutomaticRefreshState> = automatic

        override suspend fun refresh(force: Boolean): HomeRefreshResult {
            assertTrue(force)
            return CompletableDeferred<HomeRefreshResult>().also(refreshRequests::add).await()
        }
    }

    private fun content(title: String) =
        HomeContent(
            banners = listOf(HomeBanner("banner", "Banner", "https://example.test/banner")),
            recommendations =
                listOf(
                    HomeRecommendation(
                        song = Song("song", "hash", title, "artist", null, null, 1_000, null),
                        note = "note",
                    ),
                ),
            playlists = listOf(HomePlaylist("playlist", "Playlist", null, 10)),
            updatedAtEpochMs = 1,
        )
}
