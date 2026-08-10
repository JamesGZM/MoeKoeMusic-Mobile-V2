package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

sealed interface KugouDailyVipDayResult {
    data object Claimed : KugouDailyVipDayResult

    data object AlreadyClaimed : KugouDailyVipDayResult

    data class Failure(
        val error: KugouError,
    ) : KugouDailyVipDayResult
}

sealed interface KugouDailyVipUpgradeResult {
    data object Upgraded : KugouDailyVipUpgradeResult

    data object AlreadyClaimed : KugouDailyVipUpgradeResult

    data class Failure(
        val error: KugouError,
    ) : KugouDailyVipUpgradeResult
}

internal class KugouDailyVipDayDecoder {
    fun decode(body: JsonElement): KugouDailyVipDayResult =
        when (val outcome = decodeOutcome(body)) {
            DailyVipOutcome.Completed -> KugouDailyVipDayResult.Claimed
            DailyVipOutcome.AlreadyClaimed -> KugouDailyVipDayResult.AlreadyClaimed
            is DailyVipOutcome.Failure -> KugouDailyVipDayResult.Failure(outcome.error)
        }
}

internal class KugouDailyVipUpgradeDecoder {
    fun decode(body: JsonElement): KugouDailyVipUpgradeResult =
        when (val outcome = decodeOutcome(body)) {
            DailyVipOutcome.Completed -> KugouDailyVipUpgradeResult.Upgraded
            DailyVipOutcome.AlreadyClaimed -> KugouDailyVipUpgradeResult.AlreadyClaimed
            is DailyVipOutcome.Failure -> KugouDailyVipUpgradeResult.Failure(outcome.error)
        }
}

private sealed interface DailyVipOutcome {
    data object Completed : DailyVipOutcome

    data object AlreadyClaimed : DailyVipOutcome

    data class Failure(
        val error: KugouError,
    ) : DailyVipOutcome
}

private fun decodeOutcome(body: JsonElement): DailyVipOutcome {
    val root = body as? JsonObject ?: return malformed()
    val status = root.primitiveContent("status") ?: return missingRequiredField()
    if (status == "1") return DailyVipOutcome.Completed

    return when (val errorCode = root.primitiveContent("error_code")) {
        null -> {
            missingRequiredField()
        }

        ALREADY_CLAIMED_CODE -> {
            DailyVipOutcome.AlreadyClaimed
        }

        RISK_BLOCKED_CODE -> {
            DailyVipOutcome.Failure(KugouError.Risk(errorCode))
        }

        else -> {
            DailyVipOutcome.Failure(
                KugouError.Protocol(
                    reason = KugouError.Protocol.Reason.ServiceRejected,
                    serviceCode = errorCode,
                ),
            )
        }
    }
}

private fun JsonObject.primitiveContent(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

private fun malformed(): DailyVipOutcome.Failure =
    DailyVipOutcome.Failure(
        KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
    )

private fun missingRequiredField(): DailyVipOutcome.Failure =
    DailyVipOutcome.Failure(
        KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField),
    )

private const val ALREADY_CLAIMED_CODE = "131001"
private const val RISK_BLOCKED_CODE = "20028"
