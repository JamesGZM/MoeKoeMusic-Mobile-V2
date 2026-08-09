package cn.james.music

import cn.james.music.core.designsystem.BrandThemeColor
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.settings.AppSettings
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsSnapshot
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import cn.james.music.core.model.settings.BrandThemeColorPreference
import cn.james.music.core.model.settings.LyricsTextSizePreference
import cn.james.music.core.model.settings.PlaybackQualityPreference
import cn.james.music.feature.player.PlayerLyricsTextSize
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
    fun everyStoredBrandPreferenceMapsToDesignSystemBrandWithoutChangingThemeMode() =
        runTest(dispatcher) {
            BrandThemeColorPreference.entries.forEach { preference ->
                val repository =
                    FakeAppSettingsRepository(
                        initialTheme = AppThemePreference.Amoled,
                        brandThemeColor = preference,
                    )
                val viewModel = AppThemeViewModel(repository)

                runCurrent()

                assertEquals(AppThemePreference.Amoled.expectedThemeMode, viewModel.themeMode.value)
                assertEquals(preference.expectedBrandThemeColor, viewModel.brandThemeColor.value)
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
    fun dynamicCoverColorsMapsFromAppSettingsWithoutChangingTheme() =
        runTest(dispatcher) {
            val repository = FakeAppSettingsRepository(AppThemePreference.Dark, dynamicCoverColors = false)
            val viewModel = AppThemeViewModel(repository)
            runCurrent()

            assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
            assertEquals(false, viewModel.dynamicCoverColors.value)
        }

    @Test
    fun lyricsSupplementalTextMapsFromAppSettingsWithoutChangingTheme() =
        runTest(dispatcher) {
            val repository = FakeAppSettingsRepository(AppThemePreference.Dark, showLyricsSupplementalText = false)
            val viewModel = AppThemeViewModel(repository)
            runCurrent()

            assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
            assertEquals(false, viewModel.showLyricsSupplementalText.value)
        }

    @Test
    fun lyricsTextSizeMapsToPurePlayerInputWithoutChangingTheme() =
        runTest(dispatcher) {
            val repository = FakeAppSettingsRepository(AppThemePreference.Dark, lyricsTextSize = LyricsTextSizePreference.Largest)
            val viewModel = AppThemeViewModel(repository)
            runCurrent()

            assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
            assertEquals(PlayerLyricsTextSize.Largest, viewModel.lyricsTextSize.value)
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
        dynamicCoverColors: Boolean = true,
        showLyricsSupplementalText: Boolean = true,
        lyricsTextSize: LyricsTextSizePreference = LyricsTextSizePreference.Standard,
        brandThemeColor: BrandThemeColorPreference = BrandThemeColorPreference.SkyBlue,
    ) : AppSettingsRepository {
        private val mutableSettings =
            MutableStateFlow(
                AppSettingsSnapshot(
                    settings =
                        AppSettings(
                            initialTheme,
                            dynamicCoverColors = dynamicCoverColors,
                            showLyricsSupplementalText = showLyricsSupplementalText,
                            lyricsTextSize = lyricsTextSize,
                            brandThemeColor = brandThemeColor,
                        ),
                ),
            )
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

        override suspend fun setBrandThemeColor(color: BrandThemeColorPreference): AppSettingsUpdateResult =
            error("Unexpected brand theme color write")

        override suspend fun setPlaybackQuality(quality: PlaybackQualityPreference): AppSettingsUpdateResult = updateResult

        override suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult = updateResult

        override suspend fun setDynamicCoverColors(enabled: Boolean): AppSettingsUpdateResult = updateResult

        override suspend fun setShowLyricsSupplementalText(enabled: Boolean): AppSettingsUpdateResult = updateResult

        override suspend fun setLyricsTextSize(size: LyricsTextSizePreference): AppSettingsUpdateResult = updateResult
    }

    private val AppThemePreference.expectedThemeMode: ThemeMode
        get() =
            when (this) {
                AppThemePreference.System -> ThemeMode.System
                AppThemePreference.Light -> ThemeMode.Light
                AppThemePreference.Dark -> ThemeMode.Dark
                AppThemePreference.Amoled -> ThemeMode.Amoled
            }

    private val BrandThemeColorPreference.expectedBrandThemeColor: BrandThemeColor
        get() =
            when (this) {
                BrandThemeColorPreference.SkyBlue -> BrandThemeColor.SkyBlue
                BrandThemeColorPreference.SakuraPink -> BrandThemeColor.SakuraPink
                BrandThemeColorPreference.StarPurple -> BrandThemeColor.StarPurple
                BrandThemeColorPreference.MintGreen -> BrandThemeColor.MintGreen
                BrandThemeColorPreference.LakeCyan -> BrandThemeColor.LakeCyan
                BrandThemeColorPreference.SunsetOrange -> BrandThemeColor.SunsetOrange
            }
}
