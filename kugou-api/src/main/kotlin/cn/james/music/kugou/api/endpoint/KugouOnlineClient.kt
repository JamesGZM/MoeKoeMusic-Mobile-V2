package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouProtocolResult
import cn.james.music.kugou.api.transport.KugouRequestContext

sealed interface KugouApiResult<out T> {
    data class Success<T>(
        val value: T,
    ) : KugouApiResult<T>

    data class Failure(
        val error: KugouError,
    ) : KugouApiResult<Nothing>
}

sealed interface KugouPlaybackAddressResult {
    data class Available(
        val address: KugouPlaybackAddress,
    ) : KugouPlaybackAddressResult

    data class Unavailable(
        val reason: KugouPlaybackUnavailableReason,
    ) : KugouPlaybackAddressResult
}

/**
 * Typed protocol boundary for online music features. Dynamic JSON never crosses this class into
 * repositories or UI code.
 */
class KugouOnlineClient(
    private val executor: KugouCallExecutor,
    private val searchDecoder: KugouSongSearchDecoder = KugouSongSearchDecoder(),
    private val playbackAddressDecoder: KugouPlaybackAddressDecoder = KugouPlaybackAddressDecoder(),
    private val privilegeDecoder: KugouPrivilegeDecoder = KugouPrivilegeDecoder(),
) {
    suspend fun searchSongs(
        keyword: String,
        page: Int,
        pageSize: Int,
        context: KugouRequestContext,
    ): KugouApiResult<KugouSongSearchPageDto> =
        when (val response = executor.executeJson(KugouEndpoints.searchSongsAnonymous(keyword, page, pageSize), context)) {
            is KugouProtocolResult.Failure -> {
                KugouApiResult.Failure(response.error)
            }

            is KugouProtocolResult.Success -> {
                when (val decoded = searchDecoder.decode(response.body)) {
                    is KugouSongSearchDecodeResult.Failure -> KugouApiResult.Failure(decoded.error)
                    is KugouSongSearchDecodeResult.Success -> KugouApiResult.Success(decoded.page)
                }
            }
        }

    suspend fun resolvePlaybackAddress(
        songHash: String,
        context: KugouRequestContext,
    ): KugouApiResult<KugouPlaybackAddressResult> =
        when (val response = executor.executeJson(KugouEndpoints.songUrl(songHash), context)) {
            is KugouProtocolResult.Failure -> {
                KugouApiResult.Failure(response.error)
            }

            is KugouProtocolResult.Success -> {
                when (val decoded = playbackAddressDecoder.decode(response.body)) {
                    is KugouPlaybackAddressDecodeResult.Failure -> {
                        KugouApiResult.Failure(decoded.error)
                    }

                    is KugouPlaybackAddressDecodeResult.Success -> {
                        KugouApiResult.Success(KugouPlaybackAddressResult.Available(decoded.address))
                    }

                    is KugouPlaybackAddressDecodeResult.Unavailable -> {
                        KugouApiResult.Success(KugouPlaybackAddressResult.Unavailable(decoded.reason))
                    }
                }
            }
        }

    suspend fun fetchPrivilegeCandidates(
        resources: List<KugouAudioResource>,
        context: KugouRequestContext,
    ): KugouApiResult<List<KugouPrivilegeCandidate>> =
        when (val response = executor.executeJson(KugouEndpoints.privilegeLite(resources), context)) {
            is KugouProtocolResult.Failure -> {
                KugouApiResult.Failure(response.error)
            }

            is KugouProtocolResult.Success -> {
                when (val decoded = privilegeDecoder.decode(response.body)) {
                    is KugouPrivilegeDecodeResult.Failure -> KugouApiResult.Failure(decoded.error)
                    is KugouPrivilegeDecodeResult.Success -> KugouApiResult.Success(decoded.candidates)
                }
            }
        }
}
