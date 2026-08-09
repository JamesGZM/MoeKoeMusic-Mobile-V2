package cn.james.music.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import cn.james.music.core.model.settings.AppSettings
import cn.james.music.core.model.settings.AppSettingsProblem
import cn.james.music.core.model.settings.AppSettingsSnapshot
import cn.james.music.core.model.settings.AppSettingsUpdateResult
import cn.james.music.core.model.settings.AppThemePreference
import cn.james.music.core.model.settings.LyricsTextSizePreference
import cn.james.music.core.model.settings.PlaybackQualityPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.util.UUID

class PreferencesAppSettingsRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyStoreUsesSystemThemeWithoutProblem() =
        withFixture {
            assertEquals(AppSettingsSnapshot(), repository.settings.first())
        }

    @Test
    fun everyThemeRoundTripsThroughDataStore() =
        withFixture {
            AppThemePreference.entries.forEach { theme ->
                assertEquals(AppSettingsUpdateResult.Success, repository.setTheme(theme))
                assertEquals(
                    AppSettingsSnapshot(settings = AppSettings(theme)),
                    repository.settings.first(),
                )
            }
        }

    @Test
    fun autoSkipFailedPlaybackDefaultsToTrueAndRoundTrips() =
        withFixture {
            assertEquals(
                true,
                repository.settings
                    .first()
                    .settings.autoSkipFailedPlayback,
            )

            assertEquals(AppSettingsUpdateResult.Success, repository.setAutoSkipFailedPlayback(false))
            assertEquals(
                false,
                repository.settings
                    .first()
                    .settings.autoSkipFailedPlayback,
            )
        }

    @Test
    fun playbackQualityDefaultsToStandardAndEveryValueRoundTrips() =
        withFixture {
            assertEquals(
                PlaybackQualityPreference.Standard,
                repository.settings
                    .first()
                    .settings.playbackQuality,
            )

            PlaybackQualityPreference.entries
                .zip(
                    listOf("128", "320", "flac", "high", "viper_atmos", "viper_clear", "viper_tape"),
                ).forEach { (quality, storageValue) ->
                    assertEquals(AppSettingsUpdateResult.Success, repository.setPlaybackQuality(quality))
                    assertEquals(storageValue, dataStore.data.first()[PreferencesAppSettingsRepository.PLAYBACK_QUALITY])
                    assertEquals(
                        quality,
                        repository.settings
                            .first()
                            .settings.playbackQuality,
                    )
                }
        }

    @Test
    fun dynamicCoverColorsDefaultsToTrueAndRoundTrips() =
        withFixture {
            assertEquals(
                true,
                repository.settings
                    .first()
                    .settings.dynamicCoverColors,
            )

            assertEquals(AppSettingsUpdateResult.Success, repository.setDynamicCoverColors(false))
            assertEquals(
                false,
                repository.settings
                    .first()
                    .settings.dynamicCoverColors,
            )
        }

    @Test
    fun lyricsSupplementalTextDefaultsToTrueAndRoundTrips() =
        withFixture {
            assertEquals(
                true,
                repository.settings
                    .first()
                    .settings.showLyricsSupplementalText,
            )

            assertEquals(AppSettingsUpdateResult.Success, repository.setShowLyricsSupplementalText(false))
            assertEquals(
                false,
                repository.settings
                    .first()
                    .settings.showLyricsSupplementalText,
            )
        }

    @Test
    fun lyricsTextSizeDefaultsToStandardAndEveryValueRoundTrips() =
        withFixture {
            assertEquals(
                LyricsTextSizePreference.Standard,
                repository.settings
                    .first()
                    .settings.lyricsTextSize,
            )

            LyricsTextSizePreference.entries.forEach { size ->
                assertEquals(AppSettingsUpdateResult.Success, repository.setLyricsTextSize(size))
                assertEquals(
                    size,
                    repository.settings
                        .first()
                        .settings.lyricsTextSize,
                )
            }
        }

    @Test
    fun unknownStoredThemeFallsBackToSystemAndReportsReadProblem() =
        withFixture {
            dataStore.edit { preferences -> preferences[PreferencesAppSettingsRepository.THEME] = "future-theme" }

            assertEquals(
                AppSettingsSnapshot(problem = AppSettingsProblem.Read),
                repository.settings.first(),
            )
        }

    @Test
    fun unknownLyricsTextSizeFallsBackToStandardAndReportsReadProblem() =
        withFixture {
            dataStore.edit { preferences -> preferences[PreferencesAppSettingsRepository.LYRICS_TEXT_SIZE] = "future-size" }

            assertEquals(
                LyricsTextSizePreference.Standard,
                repository.settings
                    .first()
                    .settings.lyricsTextSize,
            )
            assertEquals(AppSettingsProblem.Read, repository.settings.first().problem)
        }

    @Test
    fun unknownPlaybackQualityFallsBackToStandardAndReportsReadProblem() =
        withFixture {
            dataStore.edit { preferences -> preferences[PreferencesAppSettingsRepository.PLAYBACK_QUALITY] = "future-quality" }

            assertEquals(
                PlaybackQualityPreference.Standard,
                repository.settings
                    .first()
                    .settings.playbackQuality,
            )
            assertEquals(AppSettingsProblem.Read, repository.settings.first().problem)
        }

    @Test
    fun readFailureUsesSafeDefaultAndReportsProblem() =
        runBlocking {
            val repository = PreferencesAppSettingsRepository(FailingDataStore(readError = IOException("fixture")))

            assertEquals(
                AppSettingsSnapshot(problem = AppSettingsProblem.Read),
                repository.settings.first(),
            )
            assertEquals(
                PlaybackQualityPreference.Standard,
                repository.settings
                    .first()
                    .settings.playbackQuality,
            )
        }

    @Test
    fun writeFailureIsTypedAndDoesNotChangeObservedValue() =
        runBlocking {
            val dataStore = FailingDataStore(writeError = IOException("fixture"))
            val repository = PreferencesAppSettingsRepository(dataStore)

            assertEquals(
                AppSettingsUpdateResult.Failure(AppSettingsProblem.Write),
                repository.setTheme(AppThemePreference.Dark),
            )
            assertEquals(AppSettingsSnapshot(), repository.settings.first())
        }

    @Test
    fun autoSkipWriteFailureDoesNotChangeSafeDefault() =
        runBlocking {
            val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = IOException("fixture")))

            assertEquals(
                AppSettingsUpdateResult.Failure(AppSettingsProblem.Write),
                repository.setAutoSkipFailedPlayback(false),
            )
            assertEquals(
                true,
                repository.settings
                    .first()
                    .settings.autoSkipFailedPlayback,
            )
        }

    @Test
    fun playbackQualityWriteFailureDoesNotChangeSafeDefault() =
        runBlocking {
            val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = IOException("fixture")))

            assertEquals(
                AppSettingsUpdateResult.Failure(AppSettingsProblem.Write),
                repository.setPlaybackQuality(PlaybackQualityPreference.High),
            )
            assertEquals(
                PlaybackQualityPreference.Standard,
                repository.settings
                    .first()
                    .settings.playbackQuality,
            )
        }

    @Test
    fun dynamicCoverColorsWriteFailureDoesNotChangeSafeDefault() =
        runBlocking {
            val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = IOException("fixture")))

            assertEquals(
                AppSettingsUpdateResult.Failure(AppSettingsProblem.Write),
                repository.setDynamicCoverColors(false),
            )
            assertEquals(
                true,
                repository.settings
                    .first()
                    .settings.dynamicCoverColors,
            )
        }

    @Test
    fun lyricsSupplementalTextWriteFailureDoesNotChangeSafeDefault() =
        runBlocking {
            val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = IOException("fixture")))

            assertEquals(
                AppSettingsUpdateResult.Failure(AppSettingsProblem.Write),
                repository.setShowLyricsSupplementalText(false),
            )
            assertEquals(
                true,
                repository.settings
                    .first()
                    .settings.showLyricsSupplementalText,
            )
        }

    @Test
    fun lyricsTextSizeWriteFailureDoesNotChangeSafeDefault() =
        runBlocking {
            val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = IOException("fixture")))

            assertEquals(
                AppSettingsUpdateResult.Failure(AppSettingsProblem.Write),
                repository.setLyricsTextSize(LyricsTextSizePreference.Large),
            )
            assertEquals(
                LyricsTextSizePreference.Standard,
                repository.settings
                    .first()
                    .settings.lyricsTextSize,
            )
        }

    @Test
    fun cancellationIsNotMappedToWriteFailure() {
        val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = CancellationException("fixture")))

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.setTheme(AppThemePreference.Dark) }
        }
    }

    @Test
    fun lyricsSupplementalTextCancellationIsNotMappedToWriteFailure() {
        val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = CancellationException("fixture")))

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.setShowLyricsSupplementalText(false) }
        }
    }

    @Test
    fun lyricsTextSizeCancellationIsNotMappedToWriteFailure() {
        val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = CancellationException("fixture")))

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.setLyricsTextSize(LyricsTextSizePreference.Large) }
        }
    }

    @Test
    fun playbackQualityCancellationIsNotMappedToWriteFailure() {
        val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = CancellationException("fixture")))

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.setPlaybackQuality(PlaybackQualityPreference.High) }
        }
    }

    @Test
    fun playbackQualityWriteDoesNotOverwriteExistingPreferences() =
        withFixture {
            assertEquals(AppSettingsUpdateResult.Success, repository.setTheme(AppThemePreference.Dark))
            assertEquals(AppSettingsUpdateResult.Success, repository.setAutoSkipFailedPlayback(false))
            assertEquals(AppSettingsUpdateResult.Success, repository.setPlaybackQuality(PlaybackQualityPreference.ViperTape))

            assertEquals(
                AppSettings(
                    theme = AppThemePreference.Dark,
                    playbackQuality = PlaybackQualityPreference.ViperTape,
                    autoSkipFailedPlayback = false,
                ),
                repository.settings.first().settings,
            )
        }

    private fun withFixture(block: suspend Fixture.() -> Unit) {
        val file = temporaryFolder.newFile("settings-${UUID.randomUUID()}.preferences_pb")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val fixture = Fixture(dataStore, PreferencesAppSettingsRepository(dataStore))
        try {
            runBlocking { fixture.block() }
        } finally {
            scope.cancel()
        }
    }

    private data class Fixture(
        val dataStore: DataStore<Preferences>,
        val repository: PreferencesAppSettingsRepository,
    )

    private class FailingDataStore(
        private val readError: Throwable? = null,
        private val writeError: Throwable? = null,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> =
            readError?.let { error -> flow { throw error } } ?: flowOf(emptyPreferences())

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            writeError?.let { throw it }
            return transform(emptyPreferences())
        }
    }
}
