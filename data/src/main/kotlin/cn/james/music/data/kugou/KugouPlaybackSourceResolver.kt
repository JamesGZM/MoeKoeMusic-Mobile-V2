package cn.james.music.data.kugou

import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouOnlineClient
import cn.james.music.kugou.api.endpoint.KugouPlaybackAddressResult
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.playback.KugouSourceResolver
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
        override suspend fun resolve(songHash: String): String? {
            val normalizedHash = songHash.trim()
            if (normalizedHash.isEmpty()) return null
            val session =
                when (val initialization = sessionProvider.initialize()) {
                    is KugouInitializationResult.Failure -> return null
                    is KugouInitializationResult.Ready -> initialization.session
                }
            return when (val result = onlineClient.resolvePlaybackAddress(normalizedHash, session.requestContext())) {
                is KugouApiResult.Failure -> {
                    null
                }

                is KugouApiResult.Success -> {
                    when (val value = result.value) {
                        is KugouPlaybackAddressResult.Available -> value.address.urls.firstOrNull()
                        is KugouPlaybackAddressResult.Unavailable -> null
                    }
                }
            }
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class KugouPlaybackSourceBindings {
    @Binds
    abstract fun bindKugouSourceResolver(implementation: KugouPlaybackSourceResolver): KugouSourceResolver
}
