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
            assertEquals(listOf(AppThemePreference.Amoled), repository.requests)
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

    private class FakeRepository(
        initialTheme: AppThemePreference,
    ) : AppSettingsRepository {
        val settingsState = MutableStateFlow(AppSettingsSnapshot(settings = AppSettings(initialTheme)))
        override val settings: Flow<AppSettingsSnapshot> = settingsState
        var result: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        val requests = mutableListOf<AppThemePreference>()

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult {
            requests += theme
            return result.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value = AppSettingsSnapshot(settings = AppSettings(theme))
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

        fun complete(theme: AppThemePreference) {
            completions.getValue(theme).complete(Unit)
        }
    }
}
