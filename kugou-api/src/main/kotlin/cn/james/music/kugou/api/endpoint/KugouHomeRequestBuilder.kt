package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.config.KugouPlatformConfig
import cn.james.music.kugou.api.signing.KugouRequestSigner
import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouRequestContext
import cn.james.music.kugou.api.transport.KugouRequestSpec
import cn.james.music.kugou.api.transport.KugouRetryMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.roundToLong

internal class KugouHomeRequestBuilder(
    private val platform: KugouPlatformConfig = KugouPlatformConfig.Standard,
    private val signer: KugouRequestSigner = KugouRequestSigner(),
    private val clock: EpochMillisProvider = EpochMillisProvider(System::currentTimeMillis),
) {
    // Contracts from KuGouMusicApi@6efe84e (MIT): yueku_banner.js.
    fun banners(context: KugouRequestContext): KugouRequestSpec {
        val body =
            buildJsonObject {
                put("plat", 0)
                put("channel", 201)
                put("operator", 7)
                put("networktype", 2)
                val userId = context.positiveUserId()
                if (userId == null) put("userid", 0) else put("userid", userId)
                put("vip_type", 0)
                put("m_type", 0)
                put("tags", buildJsonArray {})
                put("apiver", 5)
                put("ability", 2)
                put("mode", "normal")
            }
        return KugouRequestSpec(
            id = "yueku_banner",
            method = KugouHttpMethod.Post,
            path = "/ads.gateway/v3/listen_banner",
            headers = mapOf("Content-Type" to "application/json"),
            body = Json.encodeToString(body).encodeToByteArray(),
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    // Contract from KuGouMusicApi@6efe84e (MIT): everyday_recommend.js.
    fun dailyRecommendations(): KugouRequestSpec =
        KugouRequestSpec(
            id = "everyday_recommend",
            method = KugouHttpMethod.Post,
            path = "/everyday_song_recommend",
            params = mapOf("platform" to "ios"),
            headers = mapOf("x-router" to "everydayrec.service.kugou.com"),
            retryMode = KugouRetryMode.IdempotentRead,
        )

    // Contract from KuGouMusicApi@6efe84e (MIT): top_playlist.js.
    fun topPlaylists(
        context: KugouRequestContext,
        page: Int = 1,
        pageSize: Int = 6,
    ): KugouRequestSpec {
        require(page > 0) { "Page must be positive" }
        require(pageSize in 1..100) { "Page size must be between 1 and 100" }
        val clientTime = (clock.now() / 1_000.0).roundToLong()
        val body =
            buildJsonObject {
                put("appid", platform.appId.toInt())
                put("mid", context.mid)
                put("clientver", platform.clientVersion.toInt())
                put("platform", "android")
                put("clienttime", clientTime)
                val userId = context.positiveUserId()
                if (userId == null) put("userid", 0) else put("userid", userId)
                put("module_id", 1)
                put("page", page)
                put("pagesize", pageSize)
                put("key", signer.signParamsKey(clientTime.toString(), platform.appId, platform.clientVersion))
                put(
                    "special_recommend",
                    buildJsonObject {
                        put("withtag", 1)
                        put("withsong", 1)
                        put("sort", 1)
                        put("ugc", 1)
                        put("is_selected", 0)
                        put("withrecommend", 1)
                        put("area_code", 1)
                        put("categoryid", 0)
                    },
                )
                put("req_multi", 1)
                put("retrun_min", 5)
                put("return_special_falg", 1)
            }
        return KugouRequestSpec(
            id = "top_playlist",
            method = KugouHttpMethod.Post,
            path = "/v2/special_recommend",
            headers =
                mapOf(
                    "Content-Type" to "application/json",
                    "x-router" to "specialrec.service.kugou.com",
                ),
            body = Json.encodeToString(body).encodeToByteArray(),
            retryMode = KugouRetryMode.IdempotentRead,
        )
    }

    private fun KugouRequestContext.positiveUserId(): String? = userId?.takeIf { value -> value.toLongOrNull()?.let { it > 0 } == true }
}
