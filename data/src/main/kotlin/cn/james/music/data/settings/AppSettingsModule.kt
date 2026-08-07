package cn.james.music.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import cn.james.music.core.model.settings.AppSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppSettingsDataStore

@Module
@InstallIn(SingletonComponent::class)
object AppSettingsDataStoreModule {
    @Provides
    @Singleton
    @AppSettingsDataStore
    fun provideAppSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE_NAME) },
        )

    private const val SETTINGS_FILE_NAME = "app_settings.preferences_pb"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppSettingsBindings {
    @Binds
    abstract fun bindAppSettingsRepository(implementation: PreferencesAppSettingsRepository): AppSettingsRepository
}
