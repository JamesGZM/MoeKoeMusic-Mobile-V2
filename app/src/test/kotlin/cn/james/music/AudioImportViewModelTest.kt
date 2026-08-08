package cn.james.music

import androidx.lifecycle.SavedStateHandle
import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalImportSource
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.data.local.LocalImportGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioImportViewModelTest {
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
    fun repeatedResumeDoesNotEnqueueTheSameIntentTwice() =
        runTest(dispatcher) {
            val gateway = FakeGateway()
            val repository = FakeRepository()
            val viewModel = AudioImportViewModel(gateway, repository, SavedStateHandle())

            viewModel.resumeOrEnqueue(request())
            viewModel.resumeOrEnqueue(request())
            advanceUntilIdle()

            assertEquals(1, gateway.enqueueCalls)
            assertFalse(viewModel.state.value.finished)
        }

    @Test
    fun restoredBatchIsObservedWithoutBeingEnqueuedAgain() =
        runTest(dispatcher) {
            val gateway = FakeGateway()
            val repository = FakeRepository()
            val savedStateHandle = SavedStateHandle()
            AudioImportViewModel(gateway, repository, savedStateHandle).resumeOrEnqueue(request())
            advanceUntilIdle()

            val restored = AudioImportViewModel(gateway, repository, savedStateHandle)
            restored.resumeOrEnqueue(request())
            repository.imports.value = listOf(progress(batchId = "batch-1", LocalImportBatchState.Completed))
            advanceUntilIdle()

            assertEquals(1, gateway.enqueueCalls)
            assertEquals("导入完成 · 1/1", restored.state.value.message)
            assertTrue(restored.state.value.finished)
        }

    @Test
    fun newIntentReplacesObservationAndCreatesANewBatch() =
        runTest(dispatcher) {
            val gateway = FakeGateway()
            val repository = FakeRepository()
            val viewModel = AudioImportViewModel(gateway, repository, SavedStateHandle())

            viewModel.resumeOrEnqueue(request())
            advanceUntilIdle()
            viewModel.replaceWith(request())
            advanceUntilIdle()
            repository.imports.value = listOf(progress(batchId = "batch-2", LocalImportBatchState.Completed))
            advanceUntilIdle()

            assertEquals(2, gateway.enqueueCalls)
            assertEquals("导入完成 · 1/1", viewModel.state.value.message)
            assertTrue(viewModel.state.value.finished)
        }

    private fun request() =
        ExternalAudioImportRequest(
            uris = emptyList(),
            source = LocalImportSource.ExternalView,
            completionAction = ImportCompletionAction.PlayImportedTrack,
        )

    private fun progress(
        batchId: String,
        state: LocalImportBatchState,
    ) = LocalImportProgress(
        batchId = batchId,
        state = state,
        totalCount = 1,
        completedCount = if (state == LocalImportBatchState.Completed) 1 else 0,
        failedCount = 0,
        currentDisplayName = null,
        copiedBytes = 0,
        totalBytes = null,
    )

    private class FakeGateway : LocalImportGateway {
        var enqueueCalls = 0

        override suspend fun enqueue(
            uris: List<android.net.Uri>,
            source: LocalImportSource,
            completionAction: ImportCompletionAction,
        ): String = "batch-${++enqueueCalls}"

        override suspend fun enqueueMediaStore(mediaStoreIds: List<Long>): String = error("Not used")
    }

    private class FakeRepository : LocalMusicRepository {
        val imports = MutableStateFlow<List<LocalImportProgress>>(emptyList())

        override fun observeMusic(): Flow<List<LocalMusic>> = emptyFlow()

        override fun observeImports(): Flow<List<LocalImportProgress>> = imports

        override fun scanDevice(): Flow<DeviceAudioCandidate> = emptyFlow()

        override suspend fun cancelImport(batchId: String) = Unit

        override suspend fun delete(localMusicId: String) = Unit
    }
}
