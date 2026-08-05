package cn.james.music.kugou.api.crypto

import java.security.MessageDigest

internal object KugouDigests {
    fun md5(value: String): String = digest("MD5", value.encodeToByteArray())

    fun sha1(value: String): String = digest("SHA-1", value.encodeToByteArray())

    fun md5(bytes: ByteArray): String = digest("MD5", bytes)

    private fun digest(
        algorithm: String,
        bytes: ByteArray,
    ): String = MessageDigest.getInstance(algorithm).digest(bytes).toHex()
}

internal fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
