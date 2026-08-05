package cn.james.music.data.kugou

import cn.james.music.kugou.api.endpoint.KugouEndpoints
import cn.james.music.kugou.api.endpoint.KugouPlaybackAddressDecodeResult
import cn.james.music.kugou.api.endpoint.KugouPlaybackAddressDecoder
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouProtocolResult
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
        private val executor: KugouCallExecutor,
        private val decoder: KugouPlaybackAddressDecoder,
    ) : KugouSourceResolver {
        override suspend fun resolve(songHash: String): String? {
            val normalizedHash = songHash.trim()
            if (normalizedHash.isEmpty()) return null
            val session =
                when (val initialization = sessionProvider.initialize()) {
                    is KugouInitializationResult.Failure -> return null
                    is KugouInitializationResult.Ready -> initialization.session
                }
            val response = executor.executeJson(KugouEndpoints.songUrl(normalizedHash), session.requestContext())
            if (response !is KugouProtocolResult.Success) return null
            val decoded = decoder.decode(response.body)
            if (decoded !is KugouPlaybackAddressDecodeResult.Success) return null
            return decoded.address.urls.firstOrNull()
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class KugouPlaybackSourceBindings {
    @Binds
    abstract fun bindKugouSourceResolver(implementation: KugouPlaybackSourceResolver): KugouSourceResolver
}
