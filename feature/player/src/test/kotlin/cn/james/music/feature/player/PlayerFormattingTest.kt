package cn.james.music.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerFormattingTest {
    @Test
    fun formatsTimeAndClampsNegativeValues() {
        assertEquals("0:00", formatPlayerTime(-1))
        assertEquals("1:24", formatPlayerTime(84_999))
        assertEquals("61:01", formatPlayerTime(3_661_000))
    }
}
