package cn.james.music.playback

import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.core.model.playback.PlaybackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackRuntimeQualityPolicyTest {
    @Test
    fun readsTheQualityForTheCurrentRuntimeQueueItem() {
        val qualities = listOf(ResolvedPlaybackQuality.High, ResolvedPlaybackQuality.Lossless, null)

        assertEquals(ResolvedPlaybackQuality.High, PlaybackRuntimeQualityPolicy.currentQuality(qualities, 0))
        assertEquals(ResolvedPlaybackQuality.Lossless, PlaybackRuntimeQualityPolicy.currentQuality(qualities, 1))
        assertNull(PlaybackRuntimeQualityPolicy.currentQuality(qualities, 2))
        assertNull(PlaybackRuntimeQualityPolicy.currentQuality(qualities, -1))
        assertNull(PlaybackRuntimeQualityPolicy.currentQuality(qualities, 3))
    }

    @Test
    fun refreshedAddressReplacementSuppliesTheLatestQualityForTheCurrentItem() {
        val beforeRefresh = listOf(ResolvedPlaybackQuality.HiRes, ResolvedPlaybackQuality.Standard)
        val afterRefresh = beforeRefresh.toMutableList().apply { this[0] = ResolvedPlaybackQuality.Lossless }

        assertEquals(ResolvedPlaybackQuality.HiRes, PlaybackRuntimeQualityPolicy.currentQuality(beforeRefresh, 0))
        assertEquals(ResolvedPlaybackQuality.Lossless, PlaybackRuntimeQualityPolicy.currentQuality(afterRefresh, 0))
    }

    @Test
    fun runtimeValuesRoundTripWithoutAcceptingUnknownValues() {
        ResolvedPlaybackQuality.entries.forEach { quality ->
            assertEquals(quality, ResolvedPlaybackQuality.fromRuntimeValue(quality.runtimeValue))
        }

        assertNull(ResolvedPlaybackQuality.fromRuntimeValue("128"))
        assertNull(ResolvedPlaybackQuality.fromRuntimeValue("unknown"))
        assertNull(ResolvedPlaybackQuality.fromRuntimeValue(null))
    }

    @Test
    fun snapshotKeepsOnlyTheQueueModelRatherThanRuntimeQuality() {
        val item =
            PlaybackItem(
                id = "remote",
                title = "Title",
                artist = "Artist",
                source = PlaybackSource.Kugou("hash"),
            )
        val snapshot =
            PlaybackSnapshot(
                queue = listOf(item),
                currentIndex = 0,
                positionMs = 123,
                mode = PlaybackMode.RepeatAll,
                updatedAtEpochMs = 456,
            )

        assertEquals(listOf(item), snapshot.queue)
    }
}
