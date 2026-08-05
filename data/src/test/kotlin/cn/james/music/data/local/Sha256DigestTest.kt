package cn.james.music.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class Sha256DigestTest {
    @Test
    fun streamsChunksIntoKnownDigest() {
        val subject = Sha256Digest()
        subject.update("a".encodeToByteArray(), 0, 1)
        subject.update("bc".encodeToByteArray(), 0, 2)

        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            subject.hex(),
        )
    }
}
