package cn.james.music.feature.player

/** Emits only settled-page transitions so repository work never starts during composition. */
internal class PlayerLyricsPageVisibilityCoordinator {
    private var lastVisibility: Boolean? = null

    fun onSettledPage(page: Int): Boolean? = emitIfChanged(page == PlayerPage.Lyrics.ordinal)

    fun onDisposed(): Boolean? = emitIfChanged(false)

    private fun emitIfChanged(visible: Boolean): Boolean? =
        if (lastVisibility == visible) {
            null
        } else {
            visible.also { lastVisibility = it }
        }
}
