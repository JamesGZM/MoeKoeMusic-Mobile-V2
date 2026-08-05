package cn.james.music.data.local

import java.security.MessageDigest

internal class Sha256Digest {
    private val digest = MessageDigest.getInstance("SHA-256")

    fun update(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ) {
        digest.update(bytes, offset, length)
    }

    fun hex(): String = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
