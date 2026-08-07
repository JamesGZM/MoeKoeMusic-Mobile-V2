package cn.james.music

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackShellTest {
    @Test
    fun formatsMiniPlayerTimeWithoutNegativeValues() {
        assertEquals("0:00", formatMiniPlayerTime(-1))
        assertEquals("1:24", formatMiniPlayerTime(84_999))
        assertEquals("61:01", formatMiniPlayerTime(3_661_000))
    }
}
