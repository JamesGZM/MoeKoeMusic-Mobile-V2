package cn.james.music.feature.settings

import cn.james.music.core.model.settings.AppSettings
import cn.james.music.core.model.settings.AppSettingsProblem
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsSnapshot
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
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
    fun repositorySnapshotDrivesThemeAndReadProblem() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.Dark)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            assertEquals(SettingsThemeUi.Dark, viewModel.state.value.theme)
            repository.settingsState.value =
                AppSettingsSnapshot(
                    settings = AppSettings(AppThemePreference.System),
                    problem = AppSettingsProblem.Read,
                )
            runCurrent()

            assertEquals(SettingsThemeUi.System, viewModel.state.value.theme)
            assertEquals(SettingsProblemUi.Read, viewModel.state.value.problem)
        }

    @Test
    fun successfulSelectionCommitsRepositoryValue() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Amoled))
            advanceUntilIdle()

            assertEquals(SettingsThemeUi.Amoled, viewModel.state.value.theme)
            assertEquals(null, viewModel.state.value.savingTheme)
            assertEquals(listOf(AppThemePreference.Amoled), repository.themeRequests)
        }

    @Test
    fun failedSelectionKeepsPersistedThemeAndCanRetry() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.Dark)
            repository.result = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Light))
            advanceUntilIdle()

            assertEquals(SettingsThemeUi.Dark, viewModel.state.value.theme)
            assertEquals(SettingsProblemUi.Write, viewModel.state.value.problem)
            assertTrue(viewModel.state.value.canRetry)

            repository.result = AppSettingsUpdateResult.Success
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(SettingsThemeUi.Light, viewModel.state.value.theme)
            assertFalse(viewModel.state.value.canRetry)
        }

    @Test
    fun overlaysAreOwnedByViewModelAndSelectingThemeDismissesThem() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.OpenTheme)
            assertEquals(SettingsOverlay.ThemeSelection, viewModel.state.value.overlay)

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            assertEquals(null, viewModel.state.value.overlay)
            advanceUntilIdle()

            viewModel.onAction(SettingsAction.OpenAbout)
            assertEquals(SettingsOverlay.About, viewModel.state.value.overlay)
            viewModel.onAction(SettingsAction.DismissOverlay)
            assertEquals(null, viewModel.state.value.overlay)
        }

    @Test
    fun staleWriteCannotOverrideNewerThemeSelection() =
        runTest(dispatcher) {
            val repository = OutOfOrderRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            runCurrent()
            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Light))
            runCurrent()

            repository.complete(AppThemePreference.Dark)
            runCurrent()
            assertEquals(SettingsThemeUi.System, viewModel.state.value.theme)
            assertEquals(SettingsThemeUi.Light, viewModel.state.value.savingTheme)

            repository.complete(AppThemePreference.Light)
            advanceUntilIdle()
            assertEquals(SettingsThemeUi.Light, viewModel.state.value.theme)
            assertEquals(null, viewModel.state.value.savingTheme)
        }

    @Test
    fun autoSkipFailedPlaybackPersistsAndDoesNotCancelThemeUpdate() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            viewModel.onAction(SettingsAction.SetAutoSkipFailedPlayback(false))
            advanceUntilIdle()

            assertEquals(SettingsThemeUi.Dark, viewModel.state.value.theme)
            assertFalse(viewModel.state.value.autoSkipFailedPlayback)
            assertEquals(listOf(AppThemePreference.Dark), repository.themeRequests)
            assertEquals(listOf(false), repository.autoSkipRequests)
        }

    @Test
    fun failedAutoSkipSelectionKeepsPersistedValueAndCanRetry() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.autoSkipResult = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SetAutoSkipFailedPlayback(false))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.autoSkipFailedPlayback)
            assertEquals(SettingsProblemUi.Write, viewModel.state.value.problem)
            assertTrue(viewModel.state.value.canRetry)

            repository.autoSkipResult = AppSettingsUpdateResult.Success
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.autoSkipFailedPlayback)
            assertFalse(viewModel.state.value.canRetry)
        }

    @Test
    fun failedDynamicCoverColorSelectionRollsBackAndRetriesItsOwnRequest() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.dynamicCoverColorsResult = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SetDynamicCoverColors(false))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.dynamicCoverColors)
            assertTrue(viewModel.state.value.canRetry)

            repository.dynamicCoverColorsResult = AppSettingsUpdateResult.Success
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.dynamicCoverColors)
            assertEquals(listOf(false, false), repository.dynamicCoverColorsRequests)
        }

    @Test
    fun failedLyricsSupplementalTextSelectionRollsBackAndRetriesItsOwnRequest() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.lyricsSupplementalTextResult = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SetShowLyricsSupplementalText(false))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.showLyricsSupplementalText)
            assertTrue(viewModel.state.value.canRetry)

            repository.lyricsSupplementalTextResult = AppSettingsUpdateResult.Success
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.showLyricsSupplementalText)
            assertEquals(listOf(false, false), repository.lyricsSupplementalTextRequests)
        }

    @Test
    fun retryTargetsTheLastFailureWhenThemeAndAutoSkipWritesBothFail() =
        runTest(dispatcher) {
            val repository = ConcurrentFailureRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            runCurrent()
            viewModel.onAction(SettingsAction.SetAutoSkipFailedPlayback(false))
            runCurrent()

            repository.failTheme()
            runCurrent()
            repository.failAutoSkipFailedPlayback()
            runCurrent()

            assertEquals(SettingsProblemUi.Write, viewModel.state.value.problem)
            assertTrue(viewModel.state.value.canRetry)

            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(listOf(AppThemePreference.Dark), repository.themeRequests)
            assertEquals(listOf(false, false), repository.autoSkipRequests)
        }

    @Test
    fun retryTargetsDynamicCoverColorsWhenItsFailureFinishesLast() =
        runTest(dispatcher) {
            val repository = ConcurrentFailureRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            viewModel.onAction(SettingsAction.SetAutoSkipFailedPlayback(false))
            viewModel.onAction(SettingsAction.SetDynamicCoverColors(false))
            runCurrent()

            repository.failTheme()
            runCurrent()
            repository.failAutoSkipFailedPlayback()
            runCurrent()
            repository.failDynamicCoverColors()
            runCurrent()

            assertEquals(SettingsProblemUi.Write, viewModel.state.value.problem)
            assertTrue(viewModel.state.value.canRetry)

            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(listOf(AppThemePreference.Dark), repository.themeRequests)
            assertEquals(listOf(false), repository.autoSkipRequests)
            assertEquals(listOf(false, false), repository.dynamicCoverColorsRequests)
        }

    @Test
    fun retryTargetsLyricsSupplementalTextWhenItsFailureFinishesLast() =
        runTest(dispatcher) {
            val repository = ConcurrentFailureRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            viewModel.onAction(SettingsAction.SetDynamicCoverColors(false))
            viewModel.onAction(SettingsAction.SetShowLyricsSupplementalText(false))
            runCurrent()

            repository.failDynamicCoverColors()
            runCurrent()
            repository.failLyricsSupplementalText()
            runCurrent()

            assertEquals(SettingsProblemUi.Write, viewModel.state.value.problem)
            assertTrue(viewModel.state.value.canRetry)

            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(listOf(false), repository.dynamicCoverColorsRequests)
            assertEquals(listOf(false, false), repository.lyricsSupplementalTextRequests)
        }

    private class FakeRepository(
        initialTheme: AppThemePreference,
    ) : AppSettingsRepository {
        val settingsState = MutableStateFlow(AppSettingsSnapshot(settings = AppSettings(initialTheme)))
        override val settings: Flow<AppSettingsSnapshot> = settingsState
        var result: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        var autoSkipResult: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        var dynamicCoverColorsResult: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        var lyricsSupplementalTextResult: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        val themeRequests = mutableListOf<AppThemePreference>()
        val autoSkipRequests = mutableListOf<Boolean>()
        val dynamicCoverColorsRequests = mutableListOf<Boolean>()
        val lyricsSupplementalTextRequests = mutableListOf<Boolean>()

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult {
            themeRequests += theme
            return result.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value = AppSettingsSnapshot(settings = AppSettings(theme))
                }
            }
        }

        override suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult {
            autoSkipRequests += enabled
            return autoSkipResult.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value =
                        settingsState.value.copy(settings = settingsState.value.settings.copy(autoSkipFailedPlayback = enabled))
                }
            }
        }

        override suspend fun setDynamicCoverColors(enabled: Boolean): AppSettingsUpdateResult {
            dynamicCoverColorsRequests += enabled
            return dynamicCoverColorsResult.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value =
                        settingsState.value.copy(settings = settingsState.value.settings.copy(dynamicCoverColors = enabled))
                }
            }
        }

        override suspend fun setShowLyricsSupplementalText(enabled: Boolean): AppSettingsUpdateResult {
            lyricsSupplementalTextRequests += enabled
            return lyricsSupplementalTextResult.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value =
                        settingsState.value.copy(settings = settingsState.value.settings.copy(showLyricsSupplementalText = enabled))
                }
            }
        }
    }

    private class OutOfOrderRepository(
        initialTheme: AppThemePreference,
    ) : AppSettingsRepository {
        private val settingsState = MutableStateFlow(AppSettingsSnapshot(settings = AppSettings(initialTheme)))
        override val settings: Flow<AppSettingsSnapshot> = settingsState
        private val completions = AppThemePreference.entries.associateWith { CompletableDeferred<Unit>() }

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult {
            withContext(NonCancellable) { completions.getValue(theme).await() }
            settingsState.value = AppSettingsSnapshot(settings = AppSettings(theme))
            return AppSettingsUpdateResult.Success
        }

        override suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult = AppSettingsUpdateResult.Success

        override suspend fun setDynamicCoverColors(enabled: Boolean): AppSettingsUpdateResult = AppSettingsUpdateResult.Success

        override suspend fun setShowLyricsSupplementalText(enabled: Boolean): AppSettingsUpdateResult = AppSettingsUpdateResult.Success

        fun complete(theme: AppThemePreference) {
            completions.getValue(theme).complete(Unit)
        }
    }

    private class ConcurrentFailureRepository(
        initialTheme: AppThemePreference,
    ) : AppSettingsRepository {
        private val settingsState = MutableStateFlow(AppSettingsSnapshot(settings = AppSettings(initialTheme)))
        override val settings: Flow<AppSettingsSnapshot> = settingsState
        private val themeCompletion = CompletableDeferred<AppSettingsUpdateResult>()
        private val autoSkipCompletion = CompletableDeferred<AppSettingsUpdateResult>()
        private val dynamicCoverColorsCompletion = CompletableDeferred<AppSettingsUpdateResult>()
        private val lyricsSupplementalTextCompletion = CompletableDeferred<AppSettingsUpdateResult>()
        val themeRequests = mutableListOf<AppThemePreference>()
        val autoSkipRequests = mutableListOf<Boolean>()
        val dynamicCoverColorsRequests = mutableListOf<Boolean>()
        val lyricsSupplementalTextRequests = mutableListOf<Boolean>()

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult {
            themeRequests += theme
            return if (themeRequests.size == 1) {
                withContext(NonCancellable) { themeCompletion.await() }
            } else {
                AppSettingsUpdateResult.Success
            }
        }

        override suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult {
            autoSkipRequests += enabled
            return if (autoSkipRequests.size == 1) {
                withContext(NonCancellable) { autoSkipCompletion.await() }
            } else {
                AppSettingsUpdateResult.Success
            }
        }

        override suspend fun setDynamicCoverColors(enabled: Boolean): AppSettingsUpdateResult {
            dynamicCoverColorsRequests += enabled
            return if (dynamicCoverColorsRequests.size == 1) {
                withContext(NonCancellable) { dynamicCoverColorsCompletion.await() }
            } else {
                AppSettingsUpdateResult.Success
            }
        }

        override suspend fun setShowLyricsSupplementalText(enabled: Boolean): AppSettingsUpdateResult {
            lyricsSupplementalTextRequests += enabled
            return if (lyricsSupplementalTextRequests.size == 1) {
                withContext(NonCancellable) { lyricsSupplementalTextCompletion.await() }
            } else {
                AppSettingsUpdateResult.Success
            }
        }

        fun failTheme() {
            themeCompletion.complete(AppSettingsUpdateResult.Failure(AppSettingsProblem.Write))
        }

        fun failAutoSkipFailedPlayback() {
            autoSkipCompletion.complete(AppSettingsUpdateResult.Failure(AppSettingsProblem.Write))
        }

        fun failDynamicCoverColors() {
            dynamicCoverColorsCompletion.complete(AppSettingsUpdateResult.Failure(AppSettingsProblem.Write))
        }

        fun failLyricsSupplementalText() {
            lyricsSupplementalTextCompletion.complete(AppSettingsUpdateResult.Failure(AppSettingsProblem.Write))
        }
    }
}
