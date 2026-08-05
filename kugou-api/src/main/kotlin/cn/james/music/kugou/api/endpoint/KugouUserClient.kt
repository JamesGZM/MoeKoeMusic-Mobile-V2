package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouProtocolResult
import cn.james.music.kugou.api.transport.KugouRequestContext

interface KugouUserService {
    suspend fun fetchUserDetail(context: KugouRequestContext): KugouApiResult<KugouUserDetailDto>

    suspend fun fetchVipSummary(context: KugouRequestContext): KugouApiResult<KugouVipSummaryDto>
}

class KugouUserClient
    internal constructor(
        private val executor: KugouCallExecutor,
        private val requestBuilder: KugouUserRequestBuilder,
        private val userDetailDecoder: KugouUserDetailDecoder,
        private val vipSummaryDecoder: KugouVipSummaryDecoder,
    ) : KugouUserService {
        constructor(executor: KugouCallExecutor) : this(
            executor = executor,
            requestBuilder = KugouUserRequestBuilder(),
            userDetailDecoder = KugouUserDetailDecoder(),
            vipSummaryDecoder = KugouVipSummaryDecoder(),
        )

        override suspend fun fetchUserDetail(context: KugouRequestContext): KugouApiResult<KugouUserDetailDto> {
            val token = context.token?.takeIf(String::isNotBlank) ?: return missingSession()
            val userId = context.userId?.toLongOrNull()?.takeIf { it > 0 } ?: return missingSession()
            return when (val response = executor.executeJson(requestBuilder.userDetail(token, userId), context)) {
                is KugouProtocolResult.Failure -> KugouApiResult.Failure(response.error)
                is KugouProtocolResult.Success -> userDetailDecoder.decode(response.body)
            }
        }

        override suspend fun fetchVipSummary(context: KugouRequestContext): KugouApiResult<KugouVipSummaryDto> {
            if (context.token.isNullOrBlank() || context.userId?.toLongOrNull()?.let { it > 0 } != true) {
                return missingSession()
            }
            return when (val response = executor.executeJson(requestBuilder.vipSummary(), context)) {
                is KugouProtocolResult.Failure -> KugouApiResult.Failure(response.error)
                is KugouProtocolResult.Success -> vipSummaryDecoder.decode(response.body)
            }
        }

        private fun missingSession() =
            KugouApiResult.Failure(
                KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
            )
    }
