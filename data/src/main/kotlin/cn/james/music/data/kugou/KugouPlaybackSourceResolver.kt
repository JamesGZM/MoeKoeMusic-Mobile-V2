package cn.james.music.data.kugou

import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.endpoint.KugouPlaybackAddressResult
import cn.james.music.kugou.api.endpoint.KugouPlaybackUnavailableReason
import cn.james.music.kugou.api.session.KugouInitializationError
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.playback.KugouSourceResolver
import cn.james.music.playback.PlaybackSourceError
import cn.james.music.playback.RemotePlaybackSourceResult
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KugouPlaybackSourceResolver
    @Inject
    constructor(
        private val sessionProvider: KugouSessionProvider,
        private val onlineClient: KugouOnlineClient,
    ) : KugouSourceResolver {
        override suspend fun resolve(songHash: String): RemotePlaybackSourceResult {
            val normalizedHash = songHash.trim()
            if (normalizedHash.isEmpty()) return RemotePlaybackSourceResult.Unavailable(PlaybackSourceError.InvalidSource)
            val session =
                when (val initialization = sessionProvider.initialize()) {
                    is KugouInitializationResult.Failure -> {
                        return RemotePlaybackSourceResult.Unavailable(initialization.error.toPlaybackSourceError())
                    }
                    is KugouInitializationResult.Ready -> initialization.session
                }
            return when (val result = onlineClient.resolvePlaybackAddress(normalizedHash, session.requestContext())) {
                is KugouApiResult.Failure -> {
                    RemotePlaybackSourceResult.Unavailable(result.error.toPlaybackSourceError())
                }

                is KugouApiResult.Success -> {
                    when (val value = result.value) {
                        is KugouPlaybackAddressResult.Available -> {
                            value.address.urls.firstOrNull()?.let(RemotePlaybackSourceResult::Resolved)
                                ?: RemotePlaybackSourceResult.Unavailable(PlaybackSourceError.Protocol)
                        }
                        is KugouPlaybackAddressResult.Unavailable -> {
                            RemotePlaybackSourceResult.Unavailable(
                                when (value.reason) {
                                    KugouPlaybackUnavailableReason.NoCopyright -> PlaybackSourceError.NoCopyright
                                    KugouPlaybackUnavailableReason.VipRequired -> PlaybackSourceError.VipRequired
                                },
                            )
                        }
                    }
                }
            }
        }

        private fun KugouInitializationError.toPlaybackSourceError(): PlaybackSourceError =
            when (this) {
                is KugouInitializationError.Network -> error.toPlaybackSourceError()
                is KugouInitializationError.Http -> PlaybackSourceError.ServiceUnavailable
                is KugouInitializationError.Protocol,
                KugouInitializationError.StorageRead,
                KugouInitializationError.StorageWrite,
                -> PlaybackSourceError.SessionInitialization
            }

        private fun KugouError.toPlaybackSourceError(): PlaybackSourceError =
            when (this) {
                is KugouError.Network ->
                    when (kind) {
                        KugouError.Network.Kind.Offline -> PlaybackSourceError.Offline
                        KugouError.Network.Kind.Timeout -> PlaybackSourceError.Timeout
                        KugouError.Network.Kind.Connection -> PlaybackSourceError.Connection
                    }
                is KugouError.Risk -> PlaybackSourceError.VerificationRequired
                is KugouError.Http -> PlaybackSourceError.ServiceUnavailable
                is KugouError.Protocol ->
                    if (serviceCode == AUTHENTICATION_REQUIRED_CODE) {
                        PlaybackSourceError.AuthenticationRequired
                    } else {
                        PlaybackSourceError.Protocol
                    }
            }

        private companion object {
            const val AUTHENTICATION_REQUIRED_CODE = "152"
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class KugouPlaybackSourceBindings {
    @Binds
    abstract fun bindKugouSourceResolver(implementation: KugouPlaybackSourceResolver): KugouSourceResolver
}
