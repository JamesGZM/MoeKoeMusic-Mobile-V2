package cn.james.music.feature.player

import androidx.compose.runtime.Immutable

@Immutable
data class PlayerQueueItemUi(
    val id: String,
    val title: String,
    val artist: String,
    val durationLabel: String? = null,
    val artwork: PlayerArtworkUiModel = PlayerArtworkUiModel.None,
) {
    init {
        require(id.isNotBlank()) { "Player queue item id must not be blank" }
        require(title.isNotBlank()) { "Player queue item title must not be blank" }
        require(artist.isNotBlank()) { "Player queue item artist must not be blank" }
    }
}

@Immutable
data class PlayerQueueUiState(
    val items: List<PlayerQueueItemUi>,
    val currentItemId: String?,
    val mode: PlayerPlaybackModeUi,
    val sourceLabel: String? = null,
)

sealed interface PlayerQueueAction {
    data object Dismiss : PlayerQueueAction

    data class Play(
        val id: String,
    ) : PlayerQueueAction

    data class Remove(
        val id: String,
    ) : PlayerQueueAction

    data object Clear : PlayerQueueAction

    data object ChangeMode : PlayerQueueAction

    data class MoveBefore(
        val request: PlayerQueueMoveRequest,
    ) : PlayerQueueAction
}

/** Typed completion returned by the app composition root for one queue UI event. */
sealed interface PlayerQueueActionResult {
    data object Handled : PlayerQueueActionResult

    data class Move(
        val result: PlayerQueueMoveResult,
    ) : PlayerQueueActionResult
}

/** Places [draggedId] immediately before [beforeId], or at the end when it is null. */
data class PlayerQueueMoveRequest(
    val draggedId: String,
    val beforeId: String?,
)

/** Result of a stable-id queue reorder submitted by the app composition root. */
sealed interface PlayerQueueMoveResult {
    data object Accepted : PlayerQueueMoveResult

    /** The latest authoritative queue no longer contains the requested stable ids. */
    data object Stale : PlayerQueueMoveResult

    /** The playback controller rejected the command; the UI must restore authoritative order. */
    data object Rejected : PlayerQueueMoveResult
}
