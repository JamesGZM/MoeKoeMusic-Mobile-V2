package cn.james.music.kugou.api.crypto

import java.math.BigInteger

internal object KugouMid {
    fun fromGuid(guid: String): String = BigInteger(KugouDigests.md5(guid), 16).toString()
}
