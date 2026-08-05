package cn.james.music.kugou.api.signing

import cn.james.music.kugou.api.crypto.KugouDigests
import java.io.ByteArrayOutputStream

class KugouRequestSigner {
    fun androidSignature(
        params: Map<String, String>,
        body: ByteArray = byteArrayOf(),
    ): String {
        val canonicalParams = params.toSortedMap().entries.joinToString(separator = "") { (key, value) -> "$key=$value" }
        return md5Parts(ANDROID_SALT.encodeToByteArray(), canonicalParams.encodeToByteArray(), body, ANDROID_SALT.encodeToByteArray())
    }

    fun registerSignature(params: Map<String, String>): String {
        val values = params.values.sorted().joinToString(separator = "")
        return KugouDigests.md5("$REGISTER_SALT$values$REGISTER_SALT")
    }

    fun webSignature(params: Map<String, String>): String {
        val canonicalParams =
            params.entries
                .map { (key, value) -> "$key=$value" }
                .sorted()
                .joinToString(separator = "")
        return KugouDigests.md5("$WEB_SALT$canonicalParams$WEB_SALT")
    }

    fun signKey(
        hash: String,
        mid: String,
        userId: String = "0",
        appId: String,
    ): String = KugouDigests.md5("$hash$SIGN_KEY_SALT$appId$mid$userId")

    fun signParamsKey(
        data: String,
        appId: String,
        clientVersion: String,
    ): String = KugouDigests.md5("$appId$ANDROID_SALT$clientVersion$data")

    private fun md5Parts(vararg parts: ByteArray): String {
        val output = ByteArrayOutputStream(parts.sumOf(ByteArray::size))
        parts.forEach(output::write)
        return KugouDigests.md5(output.toByteArray())
    }

    private companion object {
        // Protocol constants from KuGouMusicApi@6efe84e util/helper.js (MIT).
        const val ANDROID_SALT = "OIlwieks28dk2k092lksi2UIkp"
        const val REGISTER_SALT = "1014"
        const val WEB_SALT = "NVPh5oo715z5DIWAeQlhMDsWXXQV4hwt"
        const val SIGN_KEY_SALT = "57ae12eb6890223e355ccfcb74edf70d"
    }
}
