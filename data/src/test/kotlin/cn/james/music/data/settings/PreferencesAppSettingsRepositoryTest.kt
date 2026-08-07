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
    fun unknownStoredThemeFallsBackToSystemAndReportsReadProblem() =
        withFixture {
            dataStore.edit { preferences -> preferences[PreferencesAppSettingsRepository.THEME] = "future-theme" }

            assertEquals(
                AppSettingsSnapshot(problem = AppSettingsProblem.Read),
                repository.settings.first(),
            )
        }

    @Test
    fun readFailureUsesSafeDefaultAndReportsProblem() =
        runBlocking {
            val repository = PreferencesAppSettingsRepository(FailingDataStore(readError = IOException("fixture")))

            assertEquals(
                AppSettingsSnapshot(problem = AppSettingsProblem.Read),
                repository.settings.first(),
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
    fun cancellationIsNotMappedToWriteFailure() {
        val repository = PreferencesAppSettingsRepository(FailingDataStore(writeError = CancellationException("fixture")))

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.setTheme(AppThemePreference.Dark) }
        }
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
