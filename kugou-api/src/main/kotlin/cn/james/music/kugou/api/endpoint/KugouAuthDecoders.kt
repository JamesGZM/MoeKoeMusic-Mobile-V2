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
