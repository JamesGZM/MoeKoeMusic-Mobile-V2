package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.crypto.KugouAuthCrypto
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class KugouAccountOptionDto(
    val userId: String,
    val nickname: String,
    val avatarUrl: String?,
    val grade: String?,
)

data class KugouAuthenticatedSessionDto(
    val token: String,
    val userId: String,
    val cookies: KugouCookies,
) {
    override fun toString(): String = "KugouAuthenticatedSessionDto(token=<redacted>, userId=<redacted>, cookies=$cookies)"
}

sealed interface KugouMobileLoginResult {
    data class Authenticated(
        val session: KugouAuthenticatedSessionDto,
    ) : KugouMobileLoginResult

    data class MultipleAccounts(
        val accounts: List<KugouAccountOptionDto>,
    ) : KugouMobileLoginResult

    data class Failure(
        val error: KugouError,
    ) : KugouMobileLoginResult
}

data class KugouRiskChallengeDto(
    val eventId: String,
    val sid: String?,
    val edt: String?,
) {
    override fun toString(): String = "KugouRiskChallengeDto(eventId=<redacted>, sid=<redacted>, edt=<redacted>)"
}

sealed interface KugouRiskProofDto {
    val type: Int

    class Sms(
        val code: String,
    ) : KugouRiskProofDto {
        override val type: Int = 32

        override fun toString(): String = "KugouRiskProofDto.Sms(code=<redacted>)"
    }

    class Tencent(
        val ticket: String,
        val randomString: String,
        val appId: String,
    ) : KugouRiskProofDto {
        override val type: Int = 23

        override fun toString(): String = "KugouRiskProofDto.Tencent(ticket=<redacted>, randomString=<redacted>, appId=<redacted>)"
    }
}

sealed interface KugouPasswordLoginResult {
    data class Authenticated(
        val session: KugouAuthenticatedSessionDto,
    ) : KugouPasswordLoginResult

    data class RiskChallenge(
        val challenge: KugouRiskChallengeDto,
    ) : KugouPasswordLoginResult

    data class MultipleAccounts(
        val accounts: List<KugouAccountOptionDto>,
    ) : KugouPasswordLoginResult

    data class Failure(
        val error: KugouError,
    ) : KugouPasswordLoginResult
}

sealed interface KugouRiskMethodDto {
    data object Sms : KugouRiskMethodDto

    data class Tencent(
        val appId: String,
    ) : KugouRiskMethodDto

    data class Unsupported(
        val type: Int,
    ) : KugouRiskMethodDto
}

internal class KugouMobileLoginDecoder(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun decode(
        body: JsonElement,
        responseCookies: KugouCookies,
        temporaryKey: String,
    ): KugouMobileLoginResult {
        val root = body as? JsonObject ?: return malformed()
        if (root.valueText("status") != "1") {
            val accounts = readAccounts(root)
            if (accounts.isNotEmpty()) return KugouMobileLoginResult.MultipleAccounts(accounts)
            return KugouMobileLoginResult.Failure(
                KugouError.Protocol(
                    reason = KugouError.Protocol.Reason.ServiceRejected,
                    serviceCode = root.valueText("error_code") ?: root.valueText("status"),
                ),
            )
        }

        val data = root["data"] as? JsonObject ?: return malformed()
        val encrypted = data.valueText("secu_params")?.takeIf(String::isNotBlank) ?: return missingField()
        val decrypted =
            runCatching { KugouAuthCrypto.decryptJson(encrypted, temporaryKey) }
                .getOrElse { return malformed() }
        val secret =
            runCatching { json.parseToJsonElement(decrypted) }
                .getOrElse { JsonPrimitive(decrypted) }
        val secretObject = secret as? JsonObject
        val token = secretObject?.valueText("token") ?: (secret as? JsonPrimitive)?.contentOrNull ?: data.valueText("token")
        val userId = secretObject?.valueText("userid") ?: data.valueText("userid")
        if (token.isNullOrBlank() || userId.isNullOrBlank() || userId == "0") return missingField()

        val authCookies =
            linkedMapOf(
                "token" to token,
                "userid" to userId,
                "t1" to (secretObject?.valueText("t1") ?: data.valueText("t1")).orEmpty(),
                "vip_type" to (secretObject?.valueText("vip_type") ?: data.valueText("vip_type") ?: "0"),
                "vip_token" to (secretObject?.valueText("vip_token") ?: data.valueText("vip_token")).orEmpty(),
            )
        return KugouMobileLoginResult.Authenticated(
            KugouAuthenticatedSessionDto(
                token = token,
                userId = userId,
                cookies = KugouCookies.from(responseCookies.asMap() + authCookies),
            ),
        )
    }

    private fun readAccounts(root: JsonObject): List<KugouAccountOptionDto> =
        runCatching {
            root["data"]
                ?.jsonObject
                ?.get("info_list")
                ?.jsonArray
                .orEmpty()
                .mapNotNull { item ->
                    val account = item as? JsonObject ?: return@mapNotNull null
                    val userId = account.valueText("userid")?.takeIf { it.isNotBlank() && it != "0" } ?: return@mapNotNull null
                    KugouAccountOptionDto(
                        userId = userId,
                        nickname = account.valueText("nickname").orEmpty(),
                        avatarUrl = account.valueText("pic")?.takeIf(String::isNotBlank),
                        grade = account.valueText("p_grade")?.takeIf(String::isNotBlank),
                    )
                }
        }.getOrDefault(emptyList())

    private fun malformed() = KugouMobileLoginResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse))

    private fun missingField() = KugouMobileLoginResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField))

    private fun JsonObject.valueText(name: String): String? {
        val primitive = get(name) as? JsonPrimitive ?: return null
        return primitive.contentOrNull ?: primitive.intOrNull?.toString()
    }
}

internal class KugouPasswordLoginDecoder(
    private val mobileLoginDecoder: KugouMobileLoginDecoder = KugouMobileLoginDecoder(),
) {
    fun decode(
        body: JsonElement,
        responseCookies: KugouCookies,
        temporaryKey: String,
        responseRiskCode: String? = null,
    ): KugouPasswordLoginResult {
        val root = body as? JsonObject ?: return malformed()
        val data = root["data"] as? JsonObject
        val eventId = root.valueText("ssaCode") ?: data?.valueText("ssaCode") ?: responseRiskCode
        val isRisk = root.valueText("error_code") == RISK_ERROR_CODE || !eventId.isNullOrBlank()
        if (isRisk) {
            if (eventId.isNullOrBlank()) return missingField()
            val sid = root.valueText("sid") ?: data?.valueText("sid")
            val edt = root.valueText("edt") ?: data?.valueText("edt")
            return KugouPasswordLoginResult.RiskChallenge(KugouRiskChallengeDto(eventId, sid, edt))
        }

        if (root.valueText("status") == "1") {
            return when (val result = mobileLoginDecoder.decode(body, responseCookies, temporaryKey)) {
                is KugouMobileLoginResult.Authenticated -> KugouPasswordLoginResult.Authenticated(result.session)
                is KugouMobileLoginResult.Failure -> KugouPasswordLoginResult.Failure(result.error)
                is KugouMobileLoginResult.MultipleAccounts -> KugouPasswordLoginResult.MultipleAccounts(result.accounts)
            }
        }

        return when (val result = mobileLoginDecoder.decode(body, responseCookies, temporaryKey)) {
            is KugouMobileLoginResult.Authenticated -> KugouPasswordLoginResult.Authenticated(result.session)
            is KugouMobileLoginResult.Failure -> KugouPasswordLoginResult.Failure(result.error)
            is KugouMobileLoginResult.MultipleAccounts -> KugouPasswordLoginResult.MultipleAccounts(result.accounts)
        }
    }

    private fun malformed() = KugouPasswordLoginResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse))

    private fun missingField() = KugouPasswordLoginResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField))

    private companion object {
        const val RISK_ERROR_CODE = "20028"
    }
}

internal class KugouRiskMethodDecoder {
    fun decode(body: JsonElement): KugouApiResult<KugouRiskMethodDto> {
        val root = body as? JsonObject ?: return malformed()
        if (root.valueText("status") != "1") return rejected(root)
        val data = root["data"] as? JsonObject ?: return missingField()
        val type = data.valueText("v_type")?.toIntOrNull() ?: return missingField()
        val method =
            when (type) {
                32 -> {
                    KugouRiskMethodDto.Sms
                }

                23 -> {
                    val appId = data.valueText("txappid")?.takeIf(String::isNotBlank) ?: return missingField()
                    KugouRiskMethodDto.Tencent(appId)
                }

                else -> {
                    KugouRiskMethodDto.Unsupported(type)
                }
            }
        return KugouApiResult.Success(method)
    }

    private fun malformed(): KugouApiResult.Failure =
        KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse))

    private fun missingField(): KugouApiResult.Failure =
        KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField))

    private fun rejected(root: JsonObject): KugouApiResult.Failure =
        KugouApiResult.Failure(
            KugouError.Protocol(
                reason = KugouError.Protocol.Reason.ServiceRejected,
                serviceCode = root.valueText("error_code") ?: root.valueText("status"),
            ),
        )
}

internal class KugouRiskVerificationDecoder {
    fun decode(body: JsonElement): KugouApiResult<Unit> {
        val root =
            body as? JsonObject
                ?: return KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse))
        if (root.valueText("status") == "1") return KugouApiResult.Success(Unit)
        return KugouApiResult.Failure(
            KugouError.Protocol(
                reason = KugouError.Protocol.Reason.ServiceRejected,
                serviceCode = root.valueText("error_code") ?: root.valueText("status"),
            ),
        )
    }
}

private fun JsonObject.valueText(name: String): String? {
    val primitive = get(name) as? JsonPrimitive ?: return null
    return primitive.contentOrNull ?: primitive.intOrNull?.toString()
}
