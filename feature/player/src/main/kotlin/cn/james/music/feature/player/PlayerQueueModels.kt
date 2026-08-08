package cn.james.music.feature.player

import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode

data class PlayerQueueItemUi(
    val item: PlaybackItem,
    val durationLabel: String? = null,
)

data class PlayerQueueUiState(
    val items: List<PlayerQueueItemUi>,
    val currentIndex: Int,
    val mode: PlaybackMode,
    val sourceLabel: String? = null,
)
