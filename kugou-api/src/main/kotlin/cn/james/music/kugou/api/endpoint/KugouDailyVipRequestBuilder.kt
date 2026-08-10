package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouRequestSpec
import cn.james.music.kugou.api.transport.KugouRetryMode
import cn.james.music.kugou.api.transport.KugouServiceErrorPolicy

internal class KugouDailyVipRequestBuilder {
    // Endpoint contract from KuGouMusicApi@6efe84e module/youth_day_vip.js (MIT).
    fun claimDay(receiveDay: String): KugouRequestSpec {
        require(RECEIVE_DAY.matches(receiveDay)) { "receiveDay must use YYYY-MM-DD" }
        return KugouRequestSpec(
            id = "daily_vip_claim_day",
            method = KugouHttpMethod.Post,
            path = "/youth/v1/recharge/receive_vip_listen_song",
            params =
                mapOf(
                    "source_id" to "90139",
                    "receive_day" to receiveDay,
                ),
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            retryMode = KugouRetryMode.None,
            serviceErrorPolicy = KugouServiceErrorPolicy.DecodeByEndpoint,
        )
    }

    // Endpoint contract from KuGouMusicApi@6efe84e module/youth_day_vip_upgrade.js (MIT).
    fun upgrade(userId: Long): KugouRequestSpec {
        require(userId > 0) { "userId must be positive" }
        return KugouRequestSpec(
            id = "daily_vip_upgrade",
            method = KugouHttpMethod.Post,
            path = "/youth/v1/listen_song/upgrade_vip_reward",
            params =
                mapOf(
                    "kugouid" to userId.toString(),
                    "ad_type" to "1",
                ),
            retryMode = KugouRetryMode.None,
            serviceErrorPolicy = KugouServiceErrorPolicy.DecodeByEndpoint,
        )
    }

    private companion object {
        val RECEIVE_DAY = Regex("\\d{4}-\\d{2}-\\d{2}")
    }
}
