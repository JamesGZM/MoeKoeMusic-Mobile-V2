package cn.james.music.data.kugou

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import cn.james.music.kugou.api.endpoint.KugouSongSearchDecoder
import cn.james.music.kugou.api.session.KugouAnonymousSessionInitializer
import cn.james.music.kugou.api.session.KugouDeviceIdentityFactory
import cn.james.music.kugou.api.session.KugouDeviceProfile
import cn.james.music.kugou.api.session.KugouDeviceProfileProvider
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionStore
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.OkHttpKugouTransport
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KugouSessionDataStore

@Singleton
class AndroidKugouDeviceProfileProvider
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : KugouDeviceProfileProvider {
        override suspend fun load(): KugouDeviceProfile {
            val memory = ActivityManager.MemoryInfo()
            context.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)
            return KugouDeviceProfile(
                availableRamBytes = memory.availMem.coerceAtLeast(0),
                availableInternalBytes = availableBytes(Environment.getDataDirectory()),
                availableExternalBytes = availableBytes(context.getExternalFilesDir(null)),
                brand = Build.BRAND.orUnknown(),
                buildSerial = Build.ID.orUnknown(),
                device = Build.DEVICE.orUnknown(),
                manufacturer = Build.MANUFACTURER.orUnknown(),
            )
        }

        private fun availableBytes(directory: File?): Long =
            directory
                ?.takeIf(File::exists)
                ?.let { runCatching { StatFs(it.absolutePath).availableBytes }.getOrDefault(0) }
                ?.coerceAtLeast(0)
                ?: 0

        private fun String?.orUnknown(): String = this?.takeIf(String::isNotBlank) ?: "unknown"
    }

@Module
@InstallIn(SingletonComponent::class)
object KugouSessionModule {
    @Provides
    @Singleton
    @KugouSessionDataStore
    fun provideSessionDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile(SESSION_FILE_NAME) },
        )

    @Provides
    @Singleton
    fun provideRequestFactory(): KugouRequestFactory = KugouRequestFactory()

    @Provides
    @Singleton
    fun provideTransport(): OkHttpKugouTransport = OkHttpKugouTransport()

    @Provides
    @Singleton
    fun provideCallExecutor(
        requestFactory: KugouRequestFactory,
        transport: OkHttpKugouTransport,
    ): KugouCallExecutor = KugouCallExecutor(requestFactory, transport)

    @Provides
    fun provideSongSearchDecoder(): KugouSongSearchDecoder = KugouSongSearchDecoder()

    @Provides
    @Singleton
    fun provideSessionProvider(
        store: KugouSessionStore,
        profileProvider: KugouDeviceProfileProvider,
        requestFactory: KugouRequestFactory,
        transport: OkHttpKugouTransport,
    ): KugouSessionProvider =
        KugouAnonymousSessionInitializer(
            store = store,
            identityFactory = KugouDeviceIdentityFactory(),
            profileProvider = profileProvider,
            requestFactory = requestFactory,
            transport = transport,
        )

    private const val SESSION_FILE_NAME = "kugou_session.preferences_pb"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class KugouSessionBindings {
    @Binds
    abstract fun bindSessionStore(implementation: EncryptedKugouSessionStore): KugouSessionStore

    @Binds
    abstract fun bindDeviceProfileProvider(implementation: AndroidKugouDeviceProfileProvider): KugouDeviceProfileProvider
}
