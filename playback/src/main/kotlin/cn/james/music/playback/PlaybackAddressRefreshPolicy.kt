package cn.james.music.playback

import androidx.media3.common.PlaybackException
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource

internal class PlaybackAddressRefreshPolicy {
    private var currentItemId: String? = null
    private var attempted = false

    fun onMediaItemTransition(itemId: String?) {
        if (itemId != currentItemId) {
            currentItemId = itemId
            attempted = false
        }
    }

    fun shouldRefresh(
        item: PlaybackItem,
        errorCode: Int,
    ): Boolean {
        onMediaItemTransition(item.id)
        if (item.source !is PlaybackSource.Kugou || attempted || errorCode !in refreshableErrorCodes) return false
        attempted = true
        return true
    }

    private companion object {
        val refreshableErrorCodes =
            setOf(
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            )
    }
}
