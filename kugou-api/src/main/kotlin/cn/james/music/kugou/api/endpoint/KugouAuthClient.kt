package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouProtocolResult
import cn.james.music.kugou.api.transport.KugouRequestContext

class KugouAuthClient
    internal constructor(
        private val executor: KugouCallExecutor,
        private val requestBuilder: KugouAuthRequestBuilder,
        private val mobileLoginDecoder: KugouMobileLoginDecoder,
    ) {
        constructor(executor: KugouCallExecutor) : this(executor, KugouAuthRequestBuilder(), KugouMobileLoginDecoder())

        suspend fun sendMobileCode(
            mobile: String,
            context: KugouRequestContext,
        ): KugouApiResult<Unit> {
            val midOnlyContext =
                KugouRequestContext(
                    mid = context.mid,
                    cookies = KugouCookies.from(mapOf("mid" to context.mid)),
                )
            return when (val response = executor.executeJson(KugouEndpoints.sendMobileCode(mobile), midOnlyContext)) {
                is KugouProtocolResult.Failure -> KugouApiResult.Failure(response.error)
                is KugouProtocolResult.Success -> KugouApiResult.Success(Unit)
            }
        }

        suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
            context: KugouRequestContext,
        ): KugouMobileLoginResult {
            val request = requestBuilder.loginWithMobileCode(mobile, code, selectedUserId)
            return when (val response = executor.executeJson(request.spec, context)) {
                is KugouProtocolResult.Failure -> {
                    KugouMobileLoginResult.Failure(response.error)
                }

                is KugouProtocolResult.Success -> {
                    mobileLoginDecoder.decode(
                        body = response.body,
                        responseCookies = response.responseCookies,
                        temporaryKey = request.temporaryKey,
                    )
                }
            }
        }
    }
