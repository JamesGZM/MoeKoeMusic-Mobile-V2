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
import cn.james.music.kugou.api.endpoint.KugouAuthClient
import cn.james.music.kugou.api.endpoint.KugouAuthenticationClient
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.endpoint.KugouPlaybackAddressDecoder
import cn.james.music.kugou.api.endpoint.KugouPrivilegeDecoder
import cn.james.music.kugou.api.endpoint.KugouSongSearchDecoder
import cn.james.music.kugou.api.session.KugouAnonymousSessionInitializer
import cn.james.music.kugou.api.session.KugouDeviceIdentityFactory
import cn.james.music.kugou.api.session.KugouDeviceProfile
import cn.james.music.kugou.api.session.KugouDeviceProfileProvider
import cn.james.music.kugou.api.session.KugouSessionMutator
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionStore
import cn.james.music.kugou.api.transport.KtorKugouTransport
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouRequestFactory
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
    fun provideTransport(): KtorKugouTransport = KtorKugouTransport()

    @Provides
    @Singleton
    fun provideCallExecutor(
        requestFactory: KugouRequestFactory,
        transport: KtorKugouTransport,
    ): KugouCallExecutor = KugouCallExecutor(requestFactory, transport)

    @Provides
    @Singleton
    fun provideOnlineClient(
        executor: KugouCallExecutor,
        songSearchDecoder: KugouSongSearchDecoder,
        playbackAddressDecoder: KugouPlaybackAddressDecoder,
        privilegeDecoder: KugouPrivilegeDecoder,
    ): KugouOnlineClient = KugouOnlineClient(executor, songSearchDecoder, playbackAddressDecoder, privilegeDecoder)

    @Provides
    @Singleton
    fun provideAuthClient(executor: KugouCallExecutor): KugouAuthenticationClient = KugouAuthClient(executor)

    @Provides
    fun provideSongSearchDecoder(): KugouSongSearchDecoder = KugouSongSearchDecoder()

    @Provides
    fun providePlaybackAddressDecoder(): KugouPlaybackAddressDecoder = KugouPlaybackAddressDecoder()

    @Provides
    fun providePrivilegeDecoder(): KugouPrivilegeDecoder = KugouPrivilegeDecoder()

    @Provides
    @Singleton
    fun provideSessionInitializer(
        store: KugouSessionStore,
        profileProvider: KugouDeviceProfileProvider,
        requestFactory: KugouRequestFactory,
        transport: KtorKugouTransport,
    ): KugouAnonymousSessionInitializer =
        KugouAnonymousSessionInitializer(
            store = store,
            identityFactory = KugouDeviceIdentityFactory(),
            profileProvider = profileProvider,
            requestFactory = requestFactory,
            transport = transport,
        )

    @Provides
    fun provideSessionProvider(initializer: KugouAnonymousSessionInitializer): KugouSessionProvider = initializer

    @Provides
    fun provideSessionMutator(initializer: KugouAnonymousSessionInitializer): KugouSessionMutator = initializer

    private const val SESSION_FILE_NAME = "kugou_session.preferences_pb"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class KugouSessionBindings {
    @Binds
    abstract fun bindSessionCipher(implementation: AndroidKeystoreSessionCipher): KugouSessionCipher

    @Binds
    abstract fun bindSessionStore(implementation: EncryptedKugouSessionStore): KugouSessionStore

    @Binds
    abstract fun bindDeviceProfileProvider(implementation: AndroidKugouDeviceProfileProvider): KugouDeviceProfileProvider
}
