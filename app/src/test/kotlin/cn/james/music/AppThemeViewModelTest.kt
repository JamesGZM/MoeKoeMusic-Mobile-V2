package cn.james.music

import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.settings.AppSettings
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsSnapshot
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppThemeViewModelTest {
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
    fun everyStoredPreferenceMapsToDesignSystemTheme() =
        runTest(dispatcher) {
            AppThemePreference.entries.forEach { preference ->
                val repository = FakeAppSettingsRepository(preference)
                val viewModel = AppThemeViewModel(repository)

                runCurrent()

                assertEquals(preference.expectedThemeMode, viewModel.themeMode.value)
            }
        }

    @Test
    fun writeFailureKeepsLastPersistedTheme() =
        runTest(dispatcher) {
            val repository = FakeAppSettingsRepository(AppThemePreference.Dark)
            repository.updateResult = AppSettingsUpdateResult.Failure(cn.james.music.core.model.settings.AppSettingsProblem.Write)
            val viewModel = AppThemeViewModel(repository)
            runCurrent()

            viewModel.updateTheme(ThemeMode.Light)
            advanceUntilIdle()

            assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
        }

    @Test
    fun newerSelectionCancelsPendingOlderWrite() =
        runTest(dispatcher) {
            val repository = FakeAppSettingsRepository(AppThemePreference.System)
            val pendingFirstWrite = CompletableDeferred<Unit>()
            repository.nextWriteGate = pendingFirstWrite
            val viewModel = AppThemeViewModel(repository)
            runCurrent()

            viewModel.updateTheme(ThemeMode.Dark)
            runCurrent()
            repository.nextWriteGate = null
            viewModel.updateTheme(ThemeMode.Light)
            advanceUntilIdle()

            assertEquals(ThemeMode.Light, viewModel.themeMode.value)
            assertEquals(listOf(AppThemePreference.Dark, AppThemePreference.Light), repository.requests)
        }

    private class FakeAppSettingsRepository(
        initialTheme: AppThemePreference,
    ) : AppSettingsRepository {
        private val mutableSettings = MutableStateFlow(AppSettingsSnapshot(settings = AppSettings(initialTheme)))
        override val settings: Flow<AppSettingsSnapshot> = mutableSettings
        var updateResult: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        var nextWriteGate: CompletableDeferred<Unit>? = null
        val requests = mutableListOf<AppThemePreference>()

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult {
            requests += theme
            nextWriteGate?.await()
            return updateResult.also { result ->
                if (result == AppSettingsUpdateResult.Success) {
                    mutableSettings.value = AppSettingsSnapshot(settings = AppSettings(theme))
                }
            }
        }
    }

    private val AppThemePreference.expectedThemeMode: ThemeMode
        get() =
            when (this) {
                AppThemePreference.System -> ThemeMode.System
                AppThemePreference.Light -> ThemeMode.Light
                AppThemePreference.Dark -> ThemeMode.Dark
                AppThemePreference.Amoled -> ThemeMode.Amoled
            }
}
