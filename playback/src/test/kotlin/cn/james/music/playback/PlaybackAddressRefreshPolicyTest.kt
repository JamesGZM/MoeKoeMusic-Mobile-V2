package cn.james.music.playback

import androidx.media3.common.PlaybackException
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAddressRefreshPolicyTest {
    private val policy = PlaybackAddressRefreshPolicy()

    @Test
    fun kugouNetworkFailureRefreshesOnlyOnceForCurrentItem() {
        val item = item("one", PlaybackSource.Kugou("hash"))

        assertTrue(policy.shouldRefresh(item, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))
        assertFalse(policy.shouldRefresh(item, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
    }

    @Test
    fun movingToAnotherItemAllowsOneNewRefresh() {
        val first = item("one", PlaybackSource.Kugou("first"))
        val second = item("two", PlaybackSource.Kugou("second"))

        assertTrue(policy.shouldRefresh(first, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
        policy.onMediaItemTransition(second.id)
        assertTrue(policy.shouldRefresh(second, PlaybackException.ERROR_CODE_IO_UNSPECIFIED))
    }

    @Test
    fun localAndDecoderFailuresDoNotRefreshAddress() {
        assertFalse(
            policy.shouldRefresh(
                item("local", PlaybackSource.ImportedLocal("local")),
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            ),
        )
        assertFalse(
            policy.shouldRefresh(
                item("online", PlaybackSource.Kugou("hash")),
                PlaybackException.ERROR_CODE_DECODING_FAILED,
            ),
        )
    }

    private fun item(
        id: String,
        source: PlaybackSource,
    ) = PlaybackItem(id = id, title = "Title", artist = "Artist", source = source)
}
