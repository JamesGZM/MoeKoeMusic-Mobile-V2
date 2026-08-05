package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouProtocolResult
import cn.james.music.kugou.api.transport.KugouRequestContext

interface KugouAuthenticationClient {
    suspend fun sendMobileCode(
        mobile: String,
        context: KugouRequestContext,
    ): KugouApiResult<Unit>

    suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String?,
        context: KugouRequestContext,
    ): KugouMobileLoginResult

    suspend fun loginWithPassword(
        username: String,
        password: String,
        context: KugouRequestContext,
    ): KugouPasswordLoginResult

    suspend fun createQrLogin(context: KugouRequestContext): KugouApiResult<KugouQrLoginSessionDto>

    suspend fun checkQrLogin(
        key: String,
        context: KugouRequestContext,
    ): KugouQrLoginCheckResult

    suspend fun getRiskMethod(
        eventId: String,
        context: KugouRequestContext,
    ): KugouApiResult<KugouRiskMethodDto>

    suspend fun verifyRisk(
        challenge: KugouRiskChallengeDto,
        proof: KugouRiskProofDto,
        context: KugouRequestContext,
    ): KugouApiResult<Unit>
}

class KugouAuthClient
    internal constructor(
        private val executor: KugouCallExecutor,
        private val requestBuilder: KugouAuthRequestBuilder,
        private val mobileLoginDecoder: KugouMobileLoginDecoder,
        private val passwordLoginDecoder: KugouPasswordLoginDecoder = KugouPasswordLoginDecoder(),
        private val riskMethodDecoder: KugouRiskMethodDecoder = KugouRiskMethodDecoder(),
        private val riskVerificationDecoder: KugouRiskVerificationDecoder = KugouRiskVerificationDecoder(),
        private val qrLoginKeyDecoder: KugouQrLoginKeyDecoder = KugouQrLoginKeyDecoder(),
        private val qrLoginCheckDecoder: KugouQrLoginCheckDecoder = KugouQrLoginCheckDecoder(),
    ) : KugouAuthenticationClient {
        constructor(executor: KugouCallExecutor) : this(executor, KugouAuthRequestBuilder(), KugouMobileLoginDecoder())

        override suspend fun sendMobileCode(
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

        override suspend fun loginWithMobileCode(
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

        override suspend fun loginWithPassword(
            username: String,
            password: String,
            context: KugouRequestContext,
        ): KugouPasswordLoginResult {
            val request = requestBuilder.loginWithPassword(username, password)
            return when (val response = executor.executeJson(request.spec, context)) {
                is KugouProtocolResult.Failure -> {
                    KugouPasswordLoginResult.Failure(response.error)
                }

                is KugouProtocolResult.Success -> {
                    passwordLoginDecoder.decode(
                        body = response.body,
                        responseCookies = response.responseCookies,
                        temporaryKey = request.temporaryKey,
                        responseRiskCode = response.riskCode,
                    )
                }
            }
        }

        override suspend fun getRiskMethod(
            eventId: String,
            context: KugouRequestContext,
        ): KugouApiResult<KugouRiskMethodDto> =
            when (val response = executor.executeJson(requestBuilder.getRiskMethod(eventId, context.userId), context)) {
                is KugouProtocolResult.Failure -> KugouApiResult.Failure(response.error)
                is KugouProtocolResult.Success -> riskMethodDecoder.decode(response.body)
            }

        override suspend fun createQrLogin(context: KugouRequestContext): KugouApiResult<KugouQrLoginSessionDto> =
            when (val response = executor.executeJson(requestBuilder.createQrLogin(), context)) {
                is KugouProtocolResult.Failure -> KugouApiResult.Failure(response.error)
                is KugouProtocolResult.Success -> qrLoginKeyDecoder.decode(response.body)
            }

        override suspend fun checkQrLogin(
            key: String,
            context: KugouRequestContext,
        ): KugouQrLoginCheckResult =
            when (val response = executor.executeJson(requestBuilder.checkQrLogin(key), context)) {
                is KugouProtocolResult.Failure -> KugouQrLoginCheckResult.Failure(response.error)
                is KugouProtocolResult.Success -> qrLoginCheckDecoder.decode(response.body, response.responseCookies)
            }

        override suspend fun verifyRisk(
            challenge: KugouRiskChallengeDto,
            proof: KugouRiskProofDto,
            context: KugouRequestContext,
        ): KugouApiResult<Unit> =
            when (val response = executor.executeJson(requestBuilder.verifyRisk(challenge, proof, context.userId), context)) {
                is KugouProtocolResult.Failure -> KugouApiResult.Failure(response.error)
                is KugouProtocolResult.Success -> riskVerificationDecoder.decode(response.body)
            }
    }
