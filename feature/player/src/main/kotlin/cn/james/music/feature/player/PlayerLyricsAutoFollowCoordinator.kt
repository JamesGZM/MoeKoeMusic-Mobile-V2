package cn.james.music.feature.player

internal const val LYRICS_AUTO_FOLLOW_RESUME_DELAY_MS = 3_500L

/**
 * Keeps the user-interaction policy separate from Compose scrolling.
 *
 * A generation changes whenever a real vertical drag, document replacement, or disposal makes a
 * previously issued programmatic scroll obsolete. The caller owns scroll animation and checks the
 * generation before applying a request.
 */
internal class PlayerLyricsAutoFollowCoordinator(
    private val resumeDelayMs: Long = LYRICS_AUTO_FOLLOW_RESUME_DELAY_MS,
) {
    private var resumeAtMs = Long.MIN_VALUE
    private var generation = 0L

    fun onUserVerticalDragStarted(nowMs: Long): Long {
        resumeAtMs = nowMs + resumeDelayMs
        generation += 1
        return generation
    }

    fun onDocumentChanged(): Long {
        resumeAtMs = Long.MIN_VALUE
        generation += 1
        return generation
    }

    fun onLayoutChanged(): Long {
        generation += 1
        return generation
    }

    fun onDisposed(): Long {
        generation += 1
        return generation
    }

    fun resumeDelayRemainingMs(nowMs: Long): Long = (resumeAtMs - nowMs).coerceAtLeast(0L)

    fun request(
        activeIndex: Int,
        lineCount: Int,
        nowMs: Long,
    ): PlayerLyricsAutoFollowRequest? {
        if (activeIndex !in 0 until lineCount || nowMs < resumeAtMs) return null
        return PlayerLyricsAutoFollowRequest(activeIndex = activeIndex, generation = generation)
    }

    fun isCurrent(request: PlayerLyricsAutoFollowRequest): Boolean = request.generation == generation
}

internal data class PlayerLyricsAutoFollowRequest(
    val activeIndex: Int,
    val generation: Long,
)

internal class PlayerLyricsUserDragObserver(
    private val coordinator: PlayerLyricsAutoFollowCoordinator,
) {
    private var userVerticalDragInProgress = false

    fun onUserVerticalScroll(nowMs: Long): Boolean {
        if (userVerticalDragInProgress) return false
        coordinator.onUserVerticalDragStarted(nowMs)
        userVerticalDragInProgress = true
        return true
    }

    fun onScrollIdle() {
        userVerticalDragInProgress = false
    }
}
