package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouProtocolResult
import cn.james.music.kugou.api.transport.KugouRequestContext

interface KugouDailyVipService {
    suspend fun claimDay(
        receiveDay: String,
        context: KugouRequestContext,
    ): KugouDailyVipDayResult

    suspend fun upgrade(context: KugouRequestContext): KugouDailyVipUpgradeResult
}

/**
 * Narrow protocol boundary for the daily concept-VIP endpoints. It intentionally does not expose
 * the rejected legacy ad-report endpoint or compose a day-to-upgrade product flow.
 */
class KugouDailyVipClient
    internal constructor(
        private val executor: KugouCallExecutor,
        private val requestBuilder: KugouDailyVipRequestBuilder,
        private val dayDecoder: KugouDailyVipDayDecoder,
        private val upgradeDecoder: KugouDailyVipUpgradeDecoder,
    ) : KugouDailyVipService {
        constructor(executor: KugouCallExecutor) : this(
            executor = executor,
            requestBuilder = KugouDailyVipRequestBuilder(),
            dayDecoder = KugouDailyVipDayDecoder(),
            upgradeDecoder = KugouDailyVipUpgradeDecoder(),
        )

        override suspend fun claimDay(
            receiveDay: String,
            context: KugouRequestContext,
        ): KugouDailyVipDayResult {
            if (authenticatedUserId(context) == null) return missingDaySession()
            return when (val response = executor.executeJson(requestBuilder.claimDay(receiveDay), context)) {
                is KugouProtocolResult.Failure -> KugouDailyVipDayResult.Failure(response.error)
                is KugouProtocolResult.Success -> dayDecoder.decode(response.body)
            }
        }

        override suspend fun upgrade(context: KugouRequestContext): KugouDailyVipUpgradeResult {
            val userId = authenticatedUserId(context) ?: return missingUpgradeSession()
            return when (val response = executor.executeJson(requestBuilder.upgrade(userId), context)) {
                is KugouProtocolResult.Failure -> KugouDailyVipUpgradeResult.Failure(response.error)
                is KugouProtocolResult.Success -> upgradeDecoder.decode(response.body)
            }
        }

        private fun authenticatedUserId(context: KugouRequestContext): Long? =
            context.token
                ?.takeIf(String::isNotBlank)
                ?.let { context.userId?.toLongOrNull()?.takeIf { userId -> userId > 0 } }

        private fun missingDaySession() =
            KugouDailyVipDayResult.Failure(
                KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
            )

        private fun missingUpgradeSession() =
            KugouDailyVipUpgradeResult.Failure(
                KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
            )
    }
