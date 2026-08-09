package cn.james.music.feature.settings

import cn.james.music.core.model.cache.CacheMaintenanceRepository
import cn.james.music.core.model.cache.CacheMaintenanceResult
import cn.james.music.core.model.cache.CacheMaintenanceScope
import cn.james.music.core.model.settings.AppSettings
import cn.james.music.core.model.settings.AppSettingsProblem
import cn.james.music.core.model.settings.AppSettingsRepository
import cn.james.music.core.model.settings.AppSettingsSnapshot
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import cn.james.music.core.model.settings.BrandThemeColorPreference
import cn.james.music.core.model.settings.LyricsTextSizePreference
import cn.james.music.core.model.settings.PlaybackQualityPreference
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
    private lateinit var cacheMaintenanceRepository: FakeCacheMaintenanceRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        cacheMaintenanceRepository = FakeCacheMaintenanceRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun repositorySnapshotDrivesThemeAndReadProblem() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.Dark)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
    fun lyricsTextSizeSelectionPersistsAndDoesNotCancelOtherSettings() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SetDynamicCoverColors(false))
            viewModel.onAction(SettingsAction.OpenLyricsTextSize)
            viewModel.onAction(SettingsAction.SelectLyricsTextSize(SettingsLyricsTextSizeUi.Large))
            advanceUntilIdle()

            assertEquals(SettingsLyricsTextSizeUi.Large, viewModel.state.value.lyricsTextSize)
            assertFalse(viewModel.state.value.dynamicCoverColors)
            assertEquals(listOf(LyricsTextSizePreference.Large), repository.lyricsTextSizeRequests)
        }

    @Test
    fun failedLyricsTextSizeSelectionRollsBackAndRetriesItsOwnRequest() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.lyricsTextSizeResult = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectLyricsTextSize(SettingsLyricsTextSizeUi.Largest))
            advanceUntilIdle()

            assertEquals(SettingsLyricsTextSizeUi.Standard, viewModel.state.value.lyricsTextSize)
            assertTrue(viewModel.state.value.canRetry)

            repository.lyricsTextSizeResult = AppSettingsUpdateResult.Success
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(SettingsLyricsTextSizeUi.Largest, viewModel.state.value.lyricsTextSize)
            assertEquals(
                listOf(LyricsTextSizePreference.Largest, LyricsTextSizePreference.Largest),
                repository.lyricsTextSizeRequests,
            )
        }

    @Test
    fun staleLyricsTextSizeWriteCannotOverrideNewerSelection() =
        runTest(dispatcher) {
            val repository = OutOfOrderRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectLyricsTextSize(SettingsLyricsTextSizeUi.Large))
            runCurrent()
            viewModel.onAction(SettingsAction.SelectLyricsTextSize(SettingsLyricsTextSizeUi.Largest))
            runCurrent()

            repository.completeLyricsTextSize(LyricsTextSizePreference.Large)
            runCurrent()
            assertEquals(SettingsLyricsTextSizeUi.Standard, viewModel.state.value.lyricsTextSize)
            assertEquals(SettingsLyricsTextSizeUi.Largest, viewModel.state.value.savingLyricsTextSize)

            repository.completeLyricsTextSize(LyricsTextSizePreference.Largest)
            advanceUntilIdle()
            assertEquals(SettingsLyricsTextSizeUi.Largest, viewModel.state.value.lyricsTextSize)
            assertEquals(null, viewModel.state.value.savingLyricsTextSize)
        }

    @Test
    fun retryTargetsTheLastFailureWhenThemeAndAutoSkipWritesBothFail() =
        runTest(dispatcher) {
            val repository = ConcurrentFailureRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
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

    @Test
    fun retryTargetsLyricsTextSizeWhenItsFailureFinishesLast() =
        runTest(dispatcher) {
            val repository = ConcurrentFailureRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SetShowLyricsSupplementalText(false))
            viewModel.onAction(SettingsAction.SelectLyricsTextSize(SettingsLyricsTextSizeUi.Largest))
            runCurrent()

            repository.failLyricsSupplementalText()
            runCurrent()
            repository.failLyricsTextSize()
            runCurrent()

            assertEquals(SettingsProblemUi.Write, viewModel.state.value.problem)
            assertTrue(viewModel.state.value.canRetry)

            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(listOf(false), repository.lyricsSupplementalTextRequests)
            assertEquals(
                listOf(LyricsTextSizePreference.Largest, LyricsTextSizePreference.Largest),
                repository.lyricsTextSizeRequests,
            )
        }

    @Test
    fun repositorySnapshotMapsPlaybackQualityToTheSettingsRow() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.settingsState.value =
                AppSettingsSnapshot(
                    settings = AppSettings(playbackQuality = PlaybackQualityPreference.ViperTape),
                )
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            assertEquals(SettingsPlaybackQualityUi.ViperTape, viewModel.state.value.playbackQuality)
        }

    @Test
    fun playbackQualitySelectionPersistsIndependently() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectPlaybackQuality(SettingsPlaybackQualityUi.HiRes))
            advanceUntilIdle()

            assertEquals(SettingsPlaybackQualityUi.HiRes, viewModel.state.value.playbackQuality)
            assertEquals(null, viewModel.state.value.savingPlaybackQuality)
            assertEquals(listOf(PlaybackQualityPreference.HiRes), repository.playbackQualityRequests)
        }

    @Test
    fun failedPlaybackQualitySelectionRollsBackAndRetriesItsOwnRequest() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.playbackQualityResult = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectPlaybackQuality(SettingsPlaybackQualityUi.Lossless))
            advanceUntilIdle()

            assertEquals(SettingsPlaybackQualityUi.Standard, viewModel.state.value.playbackQuality)
            assertTrue(viewModel.state.value.canRetry)

            repository.playbackQualityResult = AppSettingsUpdateResult.Success
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(SettingsPlaybackQualityUi.Lossless, viewModel.state.value.playbackQuality)
            assertEquals(
                listOf(PlaybackQualityPreference.Lossless, PlaybackQualityPreference.Lossless),
                repository.playbackQualityRequests,
            )
        }

    @Test
    fun stalePlaybackQualityWriteCannotOverrideNewerSelection() =
        runTest(dispatcher) {
            val repository = OutOfOrderRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectPlaybackQuality(SettingsPlaybackQualityUi.High))
            runCurrent()
            viewModel.onAction(SettingsAction.SelectPlaybackQuality(SettingsPlaybackQualityUi.ViperTape))
            runCurrent()

            repository.completePlaybackQuality(PlaybackQualityPreference.High)
            runCurrent()
            assertEquals(SettingsPlaybackQualityUi.Standard, viewModel.state.value.playbackQuality)
            assertEquals(SettingsPlaybackQualityUi.ViperTape, viewModel.state.value.savingPlaybackQuality)

            repository.completePlaybackQuality(PlaybackQualityPreference.ViperTape)
            advanceUntilIdle()
            assertEquals(SettingsPlaybackQualityUi.ViperTape, viewModel.state.value.playbackQuality)
            assertEquals(null, viewModel.state.value.savingPlaybackQuality)
        }

    @Test
    fun retryTargetsPlaybackQualityWhenItsFailureFinishesLast() =
        runTest(dispatcher) {
            val repository = ConcurrentFailureRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SetDynamicCoverColors(false))
            viewModel.onAction(SettingsAction.SelectPlaybackQuality(SettingsPlaybackQualityUi.HiRes))
            runCurrent()

            repository.failDynamicCoverColors()
            runCurrent()
            repository.failPlaybackQuality()
            runCurrent()

            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(listOf(false), repository.dynamicCoverColorsRequests)
            assertEquals(
                listOf(PlaybackQualityPreference.HiRes, PlaybackQualityPreference.HiRes),
                repository.playbackQualityRequests,
            )
        }

    @Test
    fun repositorySnapshotMapsBrandThemeColorToTheSettingsRow() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.settingsState.value =
                AppSettingsSnapshot(
                    settings = AppSettings(brandThemeColor = BrandThemeColorPreference.SunsetOrange),
                )
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            assertEquals(SettingsBrandThemeColorUi.SunsetOrange, viewModel.state.value.brandThemeColor)
        }

    @Test
    fun brandThemeColorSelectionPersistsIndependently() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SetDynamicCoverColors(false))
            viewModel.onAction(SettingsAction.SelectBrandThemeColor(SettingsBrandThemeColorUi.MintGreen))
            advanceUntilIdle()

            assertEquals(SettingsBrandThemeColorUi.MintGreen, viewModel.state.value.brandThemeColor)
            assertFalse(viewModel.state.value.dynamicCoverColors)
            assertEquals(listOf(BrandThemeColorPreference.MintGreen), repository.brandThemeColorRequests)
        }

    @Test
    fun brandThemeColorDoesNotCancelAnyOtherSettingsWrite() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            viewModel.onAction(SettingsAction.SetAutoSkipFailedPlayback(false))
            viewModel.onAction(SettingsAction.SetDynamicCoverColors(false))
            viewModel.onAction(SettingsAction.SetShowLyricsSupplementalText(false))
            viewModel.onAction(SettingsAction.SelectLyricsTextSize(SettingsLyricsTextSizeUi.Large))
            viewModel.onAction(SettingsAction.SelectPlaybackQuality(SettingsPlaybackQualityUi.High))
            viewModel.onAction(SettingsAction.SelectBrandThemeColor(SettingsBrandThemeColorUi.MintGreen))
            advanceUntilIdle()

            assertEquals(listOf(AppThemePreference.Dark), repository.themeRequests)
            assertEquals(listOf(false), repository.autoSkipRequests)
            assertEquals(listOf(false), repository.dynamicCoverColorsRequests)
            assertEquals(listOf(false), repository.lyricsSupplementalTextRequests)
            assertEquals(listOf(LyricsTextSizePreference.Large), repository.lyricsTextSizeRequests)
            assertEquals(listOf(PlaybackQualityPreference.High), repository.playbackQualityRequests)
            assertEquals(listOf(BrandThemeColorPreference.MintGreen), repository.brandThemeColorRequests)
        }

    @Test
    fun failedBrandThemeColorSelectionRollsBackAndRetriesItsOwnRequest() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.brandThemeColorResult = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectBrandThemeColor(SettingsBrandThemeColorUi.StarPurple))
            advanceUntilIdle()

            assertEquals(SettingsBrandThemeColorUi.SkyBlue, viewModel.state.value.brandThemeColor)
            assertTrue(viewModel.state.value.canRetry)

            repository.brandThemeColorResult = AppSettingsUpdateResult.Success
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(SettingsBrandThemeColorUi.StarPurple, viewModel.state.value.brandThemeColor)
            assertEquals(
                listOf(BrandThemeColorPreference.StarPurple, BrandThemeColorPreference.StarPurple),
                repository.brandThemeColorRequests,
            )
        }

    @Test
    fun staleBrandThemeColorWriteCannotOverrideNewerSelection() =
        runTest(dispatcher) {
            val repository = OutOfOrderRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectBrandThemeColor(SettingsBrandThemeColorUi.SakuraPink))
            runCurrent()
            viewModel.onAction(SettingsAction.SelectBrandThemeColor(SettingsBrandThemeColorUi.LakeCyan))
            runCurrent()

            repository.completeBrandThemeColor(BrandThemeColorPreference.SakuraPink)
            runCurrent()
            assertEquals(SettingsBrandThemeColorUi.SkyBlue, viewModel.state.value.brandThemeColor)
            assertEquals(SettingsBrandThemeColorUi.LakeCyan, viewModel.state.value.savingBrandThemeColor)

            repository.completeBrandThemeColor(BrandThemeColorPreference.LakeCyan)
            advanceUntilIdle()
            assertEquals(SettingsBrandThemeColorUi.LakeCyan, viewModel.state.value.brandThemeColor)
            assertEquals(null, viewModel.state.value.savingBrandThemeColor)
        }

    @Test
    fun retryTargetsBrandThemeColorWhenItsFailureFinishesLast() =
        runTest(dispatcher) {
            val repository = ConcurrentFailureRepository(AppThemePreference.System)
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectPlaybackQuality(SettingsPlaybackQualityUi.HiRes))
            viewModel.onAction(SettingsAction.SelectBrandThemeColor(SettingsBrandThemeColorUi.SunsetOrange))
            runCurrent()

            repository.failPlaybackQuality()
            runCurrent()
            repository.failBrandThemeColor()
            runCurrent()

            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(listOf(PlaybackQualityPreference.HiRes), repository.playbackQualityRequests)
            assertEquals(
                listOf(BrandThemeColorPreference.SunsetOrange, BrandThemeColorPreference.SunsetOrange),
                repository.brandThemeColorRequests,
            )
        }

    @Test
    fun clearCacheConfirmationCancelsBeforeSubmitAndLocksWhileSubmitting() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            cacheMaintenanceRepository.pendingResult = CompletableDeferred()
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.OpenClearCache)
            assertEquals(SettingsOverlay.ClearCacheConfirmation, viewModel.state.value.overlay)
            viewModel.onAction(SettingsAction.DismissOverlay)
            assertEquals(null, viewModel.state.value.overlay)

            viewModel.onAction(SettingsAction.OpenClearCache)
            viewModel.onAction(SettingsAction.ConfirmClearCache)
            viewModel.onAction(SettingsAction.ConfirmClearCache)
            runCurrent()

            assertTrue(viewModel.state.value.clearingCache)
            assertEquals(SettingsOverlay.ClearCacheConfirmation, viewModel.state.value.overlay)
            assertEquals(1, cacheMaintenanceRepository.requests)
            viewModel.onAction(SettingsAction.DismissOverlay)
            assertEquals(SettingsOverlay.ClearCacheConfirmation, viewModel.state.value.overlay)

            cacheMaintenanceRepository.pendingResult?.complete(CacheMaintenanceResult.Cleared(CacheMaintenanceScope.entries.toSet()))
            advanceUntilIdle()

            assertFalse(viewModel.state.value.clearingCache)
            assertEquals(null, viewModel.state.value.overlay)
            assertEquals(SettingsCacheClearFeedbackUi.Cleared, viewModel.state.value.cacheClearFeedback)
            assertFalse(viewModel.state.value.canRetry)
        }

    @Test
    fun partialClearClosesDialogAndRetriesTheWholeCacheClearOperation() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            cacheMaintenanceRepository.result =
                CacheMaintenanceResult.PartiallyCleared(
                    clearedScopes = setOf(CacheMaintenanceScope.HomeContentSnapshots),
                    failedScopes = setOf(CacheMaintenanceScope.ImageDisk),
                )
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.OpenClearCache)
            viewModel.onAction(SettingsAction.ConfirmClearCache)
            advanceUntilIdle()

            assertEquals(null, viewModel.state.value.overlay)
            assertEquals(SettingsCacheClearFeedbackUi.PartiallyCleared, viewModel.state.value.cacheClearFeedback)
            assertTrue(viewModel.state.value.canRetry)

            cacheMaintenanceRepository.result = CacheMaintenanceResult.Cleared(CacheMaintenanceScope.entries.toSet())
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(2, cacheMaintenanceRepository.requests)
            assertEquals(SettingsCacheClearFeedbackUi.Cleared, viewModel.state.value.cacheClearFeedback)
            assertFalse(viewModel.state.value.canRetry)
        }

    @Test
    fun failedClearUsesCacheRetryAndDoesNotCancelAnotherSettingsWrite() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            cacheMaintenanceRepository.pendingResult = CompletableDeferred()
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            viewModel.onAction(SettingsAction.OpenClearCache)
            viewModel.onAction(SettingsAction.ConfirmClearCache)
            runCurrent()

            assertEquals(listOf(AppThemePreference.Dark), repository.themeRequests)
            assertTrue(viewModel.state.value.clearingCache)

            cacheMaintenanceRepository.pendingResult?.complete(
                CacheMaintenanceResult.FailedBeforeAnyClear(setOf(CacheMaintenanceScope.ImageMemory)),
            )
            advanceUntilIdle()

            assertEquals(SettingsThemeUi.Dark, viewModel.state.value.theme)
            assertEquals(SettingsCacheClearFeedbackUi.Failed, viewModel.state.value.cacheClearFeedback)
            assertTrue(viewModel.state.value.canRetry)
        }

    @Test
    fun cacheClearSuccessFeedbackCanBeConsumedWithoutChangingRetryState() =
        runTest(dispatcher) {
            val viewModel = SettingsViewModel(FakeRepository(AppThemePreference.System), cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.OpenClearCache)
            viewModel.onAction(SettingsAction.ConfirmClearCache)
            advanceUntilIdle()
            assertEquals(SettingsCacheClearFeedbackUi.Cleared, viewModel.state.value.cacheClearFeedback)

            viewModel.onAction(SettingsAction.DismissCacheClearFeedback)

            assertEquals(null, viewModel.state.value.cacheClearFeedback)
            assertFalse(viewModel.state.value.canRetry)
        }

    @Test
    fun startingAnotherSettingWriteClearsPartialCacheFeedbackAndRetryNeverReplaysCache() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            cacheMaintenanceRepository.result =
                CacheMaintenanceResult.PartiallyCleared(
                    clearedScopes = setOf(CacheMaintenanceScope.HomeContentSnapshots),
                    failedScopes = setOf(CacheMaintenanceScope.ImageDisk),
                )
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.OpenClearCache)
            viewModel.onAction(SettingsAction.ConfirmClearCache)
            advanceUntilIdle()
            assertEquals(SettingsCacheClearFeedbackUi.PartiallyCleared, viewModel.state.value.cacheClearFeedback)

            repository.result = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            advanceUntilIdle()

            assertEquals(null, viewModel.state.value.cacheClearFeedback)
            assertEquals(SettingsProblemUi.Write, viewModel.state.value.problem)

            repository.result = AppSettingsUpdateResult.Success
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(1, cacheMaintenanceRepository.requests)
            assertEquals(listOf(AppThemePreference.Dark, AppThemePreference.Dark), repository.themeRequests)
        }

    @Test
    fun cacheFeedbackThatCompletesAfterAnotherWriteRemainsAlignedWithCacheRetry() =
        runTest(dispatcher) {
            val repository = FakeRepository(AppThemePreference.System)
            repository.result = AppSettingsUpdateResult.Failure(AppSettingsProblem.Write)
            cacheMaintenanceRepository.pendingResult = CompletableDeferred()
            val viewModel = SettingsViewModel(repository, cacheMaintenanceRepository)
            runCurrent()

            viewModel.onAction(SettingsAction.OpenClearCache)
            viewModel.onAction(SettingsAction.ConfirmClearCache)
            viewModel.onAction(SettingsAction.SelectTheme(SettingsThemeUi.Dark))
            runCurrent()
            assertEquals(SettingsProblemUi.Write, viewModel.state.value.problem)

            cacheMaintenanceRepository.pendingResult?.complete(
                CacheMaintenanceResult.PartiallyCleared(
                    clearedScopes = setOf(CacheMaintenanceScope.HomeContentSnapshots),
                    failedScopes = setOf(CacheMaintenanceScope.ImageDisk),
                ),
            )
            advanceUntilIdle()

            assertEquals(SettingsCacheClearFeedbackUi.PartiallyCleared, viewModel.state.value.cacheClearFeedback)
            repository.result = AppSettingsUpdateResult.Success
            cacheMaintenanceRepository.pendingResult = null
            cacheMaintenanceRepository.result = CacheMaintenanceResult.Cleared(CacheMaintenanceScope.entries.toSet())
            viewModel.onAction(SettingsAction.Retry)
            advanceUntilIdle()

            assertEquals(listOf(AppThemePreference.Dark), repository.themeRequests)
            assertEquals(2, cacheMaintenanceRepository.requests)
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
        var lyricsTextSizeResult: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        var playbackQualityResult: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        var brandThemeColorResult: AppSettingsUpdateResult = AppSettingsUpdateResult.Success
        val themeRequests = mutableListOf<AppThemePreference>()
        val autoSkipRequests = mutableListOf<Boolean>()
        val dynamicCoverColorsRequests = mutableListOf<Boolean>()
        val lyricsSupplementalTextRequests = mutableListOf<Boolean>()
        val lyricsTextSizeRequests = mutableListOf<LyricsTextSizePreference>()
        val playbackQualityRequests = mutableListOf<PlaybackQualityPreference>()
        val brandThemeColorRequests = mutableListOf<BrandThemeColorPreference>()

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult {
            themeRequests += theme
            return result.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value = AppSettingsSnapshot(settings = AppSettings(theme))
                }
            }
        }

        override suspend fun setBrandThemeColor(color: BrandThemeColorPreference): AppSettingsUpdateResult {
            brandThemeColorRequests += color
            return brandThemeColorResult.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value = settingsState.value.copy(settings = settingsState.value.settings.copy(brandThemeColor = color))
                }
            }
        }

        override suspend fun setPlaybackQuality(quality: PlaybackQualityPreference): AppSettingsUpdateResult {
            playbackQualityRequests += quality
            return playbackQualityResult.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value = settingsState.value.copy(settings = settingsState.value.settings.copy(playbackQuality = quality))
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

        override suspend fun setLyricsTextSize(size: LyricsTextSizePreference): AppSettingsUpdateResult {
            lyricsTextSizeRequests += size
            return lyricsTextSizeResult.also {
                if (it == AppSettingsUpdateResult.Success) {
                    settingsState.value = settingsState.value.copy(settings = settingsState.value.settings.copy(lyricsTextSize = size))
                }
            }
        }
    }

    private class FakeCacheMaintenanceRepository : CacheMaintenanceRepository {
        var result: CacheMaintenanceResult = CacheMaintenanceResult.Cleared(CacheMaintenanceScope.entries.toSet())
        var pendingResult: CompletableDeferred<CacheMaintenanceResult>? = null
        var requests = 0

        override suspend fun clearCaches(): CacheMaintenanceResult {
            requests += 1
            return pendingResult?.await() ?: result
        }
    }

    private class OutOfOrderRepository(
        initialTheme: AppThemePreference,
    ) : AppSettingsRepository {
        private val settingsState = MutableStateFlow(AppSettingsSnapshot(settings = AppSettings(initialTheme)))
        override val settings: Flow<AppSettingsSnapshot> = settingsState
        private val completions = AppThemePreference.entries.associateWith { CompletableDeferred<Unit>() }
        private val lyricsTextSizeCompletions =
            LyricsTextSizePreference.entries.associateWith { CompletableDeferred<Unit>() }
        private val playbackQualityCompletions =
            PlaybackQualityPreference.entries.associateWith { CompletableDeferred<Unit>() }
        private val brandThemeColorCompletions =
            BrandThemeColorPreference.entries.associateWith { CompletableDeferred<Unit>() }

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult {
            withContext(NonCancellable) { completions.getValue(theme).await() }
            settingsState.value = AppSettingsSnapshot(settings = AppSettings(theme))
            return AppSettingsUpdateResult.Success
        }

        override suspend fun setBrandThemeColor(color: BrandThemeColorPreference): AppSettingsUpdateResult {
            withContext(NonCancellable) { brandThemeColorCompletions.getValue(color).await() }
            settingsState.value = settingsState.value.copy(settings = settingsState.value.settings.copy(brandThemeColor = color))
            return AppSettingsUpdateResult.Success
        }

        override suspend fun setPlaybackQuality(quality: PlaybackQualityPreference): AppSettingsUpdateResult {
            withContext(NonCancellable) { playbackQualityCompletions.getValue(quality).await() }
            settingsState.value = settingsState.value.copy(settings = settingsState.value.settings.copy(playbackQuality = quality))
            return AppSettingsUpdateResult.Success
        }

        override suspend fun setAutoSkipFailedPlayback(enabled: Boolean): AppSettingsUpdateResult = AppSettingsUpdateResult.Success

        override suspend fun setDynamicCoverColors(enabled: Boolean): AppSettingsUpdateResult = AppSettingsUpdateResult.Success

        override suspend fun setShowLyricsSupplementalText(enabled: Boolean): AppSettingsUpdateResult = AppSettingsUpdateResult.Success

        override suspend fun setLyricsTextSize(size: LyricsTextSizePreference): AppSettingsUpdateResult {
            withContext(NonCancellable) { lyricsTextSizeCompletions.getValue(size).await() }
            settingsState.value = settingsState.value.copy(settings = settingsState.value.settings.copy(lyricsTextSize = size))
            return AppSettingsUpdateResult.Success
        }

        fun complete(theme: AppThemePreference) {
            completions.getValue(theme).complete(Unit)
        }

        fun completeLyricsTextSize(size: LyricsTextSizePreference) {
            lyricsTextSizeCompletions.getValue(size).complete(Unit)
        }

        fun completePlaybackQuality(quality: PlaybackQualityPreference) {
            playbackQualityCompletions.getValue(quality).complete(Unit)
        }

        fun completeBrandThemeColor(color: BrandThemeColorPreference) {
            brandThemeColorCompletions.getValue(color).complete(Unit)
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
        private val lyricsTextSizeCompletion = CompletableDeferred<AppSettingsUpdateResult>()
        private val playbackQualityCompletion = CompletableDeferred<AppSettingsUpdateResult>()
        private val brandThemeColorCompletion = CompletableDeferred<AppSettingsUpdateResult>()
        val themeRequests = mutableListOf<AppThemePreference>()
        val autoSkipRequests = mutableListOf<Boolean>()
        val dynamicCoverColorsRequests = mutableListOf<Boolean>()
        val lyricsSupplementalTextRequests = mutableListOf<Boolean>()
        val lyricsTextSizeRequests = mutableListOf<LyricsTextSizePreference>()
        val playbackQualityRequests = mutableListOf<PlaybackQualityPreference>()
        val brandThemeColorRequests = mutableListOf<BrandThemeColorPreference>()

        override suspend fun setTheme(theme: AppThemePreference): AppSettingsUpdateResult {
            themeRequests += theme
            return if (themeRequests.size == 1) {
                withContext(NonCancellable) { themeCompletion.await() }
            } else {
                AppSettingsUpdateResult.Success
            }
        }

        override suspend fun setBrandThemeColor(color: BrandThemeColorPreference): AppSettingsUpdateResult {
            brandThemeColorRequests += color
            return if (brandThemeColorRequests.size == 1) {
                withContext(NonCancellable) { brandThemeColorCompletion.await() }
            } else {
                AppSettingsUpdateResult.Success
            }
        }

        override suspend fun setPlaybackQuality(quality: PlaybackQualityPreference): AppSettingsUpdateResult {
            playbackQualityRequests += quality
            return if (playbackQualityRequests.size == 1) {
                withContext(NonCancellable) { playbackQualityCompletion.await() }
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

        override suspend fun setLyricsTextSize(size: LyricsTextSizePreference): AppSettingsUpdateResult {
            lyricsTextSizeRequests += size
            return if (lyricsTextSizeRequests.size == 1) {
                withContext(NonCancellable) { lyricsTextSizeCompletion.await() }
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

        fun failLyricsTextSize() {
            lyricsTextSizeCompletion.complete(AppSettingsUpdateResult.Failure(AppSettingsProblem.Write))
        }

        fun failPlaybackQuality() {
            playbackQualityCompletion.complete(AppSettingsUpdateResult.Failure(AppSettingsProblem.Write))
        }

        fun failBrandThemeColor() {
            brandThemeColorCompletion.complete(AppSettingsUpdateResult.Failure(AppSettingsProblem.Write))
        }
    }
}
