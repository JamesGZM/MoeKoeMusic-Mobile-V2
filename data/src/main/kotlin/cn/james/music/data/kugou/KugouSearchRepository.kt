package cn.james.music.data.kugou

import cn.james.music.core.model.online.SearchError
import cn.james.music.core.model.online.SearchPage
import cn.james.music.core.model.online.SearchRepository
import cn.james.music.core.model.online.SearchResult
import cn.james.music.core.model.online.Song
import cn.james.music.kugou.api.endpoint.KugouEndpoints
import cn.james.music.kugou.api.endpoint.KugouSongDto
import cn.james.music.kugou.api.endpoint.KugouSongSearchDecodeResult
import cn.james.music.kugou.api.endpoint.KugouSongSearchDecoder
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouProtocolResult
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KugouSearchRepository
    @Inject
    constructor(
        private val sessionProvider: KugouSessionProvider,
        private val executor: KugouCallExecutor,
        private val decoder: KugouSongSearchDecoder,
    ) : SearchRepository {
        override suspend fun searchSongs(
            keyword: String,
            page: Int,
            pageSize: Int,
        ): SearchResult {
            val normalizedKeyword = keyword.trim()
            require(normalizedKeyword.isNotEmpty()) { "Search keyword must not be blank" }
            val session =
                when (val initialization = sessionProvider.initialize()) {
                    is KugouInitializationResult.Failure -> return SearchResult.Failure(SearchError.SessionInitialization)
                    is KugouInitializationResult.Ready -> initialization.session
                }
            return when (
                val response =
                    executor.executeJson(
                        KugouEndpoints.searchSongsAnonymous(normalizedKeyword, page, pageSize),
                        session.requestContext(),
                    )
            ) {
                is KugouProtocolResult.Failure -> SearchResult.Failure(response.error.toSearchError())
                is KugouProtocolResult.Success -> response.body.toSearchResult(page, pageSize)
            }
        }

        private fun kotlinx.serialization.json.JsonElement.toSearchResult(
            page: Int,
            pageSize: Int,
        ): SearchResult =
            when (val decoded = decoder.decode(this)) {
                is KugouSongSearchDecodeResult.Failure -> {
                    SearchResult.Failure(decoded.error.toSearchError())
                }

                is KugouSongSearchDecodeResult.Success -> {
                    SearchResult.Success(
                        SearchPage(
                            items = decoded.page.items.map { item -> item.toDomain() },
                            page = page,
                            totalCount = decoded.page.totalCount,
                            hasMore = page.toLong() * pageSize < decoded.page.totalCount,
                        ),
                    )
                }
            }

        private fun KugouSongDto.toDomain(): Song =
            Song(
                id = id,
                hash = hash,
                title = title,
                artistName = artistName,
                albumId = albumId,
                albumTitle = albumTitle,
                durationMs = durationSeconds.coerceAtMost(Long.MAX_VALUE / 1_000) * 1_000,
                artworkUrl = artworkUrl?.replace("{size}", "300")?.replaceFirst("http://", "https://"),
            )

        private fun KugouError.toSearchError(): SearchError =
            when (this) {
                is KugouError.Network -> {
                    when (kind) {
                        KugouError.Network.Kind.Offline -> SearchError.Offline
                        KugouError.Network.Kind.Timeout -> SearchError.Timeout
                        KugouError.Network.Kind.Connection -> SearchError.Connection
                    }
                }

                is KugouError.Risk -> {
                    SearchError.VerificationRequired
                }

                is KugouError.Http -> {
                    SearchError.ServiceUnavailable
                }

                is KugouError.Protocol -> {
                    if (serviceCode == AUTHENTICATION_REQUIRED_CODE) {
                        SearchError.AuthenticationRequired
                    } else {
                        SearchError.Protocol
                    }
                }
            }

        private companion object {
            const val AUTHENTICATION_REQUIRED_CODE = "152"
        }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class KugouSearchBindings {
    @Binds
    abstract fun bindSearchRepository(implementation: KugouSearchRepository): SearchRepository
}
