package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.crypto.KugouAuthCrypto
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouRequestSpec
import cn.james.music.kugou.api.transport.KugouRetryMode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class KugouUserRequestBuilder(
    private val clock: EpochSecondsProvider = EpochSecondsProvider { System.currentTimeMillis() / 1_000L },
) {
    // Endpoint contract from KuGouMusicApi@6efe84e module/user_detail.js (MIT).
    fun userDetail(
        token: String,
        userId: Long,
    ): KugouRequestSpec {
        require(token.isNotBlank()) { "Token must not be blank" }
        require(userId > 0) { "User id must be positive" }
        val clientTime = clock.now()
        val envelope =
            KugouAuthCrypto.encryptKeyEnvelope(
                buildJsonObject {
                    put("token", token)
                    put("clienttime", clientTime)
                }.toString(),
            )
        val body =
            buildJsonObject {
                put("visit_time", clientTime)
                put("usertype", 1)
                put("p", envelope)
                put("userid", userId)
            }.toString()
        return KugouRequestSpec(
            id = "user_detail",
            method = KugouHttpMethod.Post,
            path = "/v3/get_my_info",
            params = mapOf("plat" to "1"),
            headers =
                mapOf(
                    "Content-Type" to "application/json",
                    "x-router" to "usercenter.kugou.com",
                ),
            body = body.encodeToByteArray(),
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    // Endpoint contract from KuGouMusicApi@6efe84e module/user_vip_detail.js (MIT).
    fun vipSummary(): KugouRequestSpec =
        KugouRequestSpec(
            id = "user_vip_detail",
            method = KugouHttpMethod.Get,
            baseUrl = "https://kugouvip.kugou.com",
            path = "/v1/get_union_vip",
            params = mapOf("busi_type" to "concept"),
            retryMode = KugouRetryMode.IdempotentRead,
        )
}
