package cn.james.music.feature.player

/** Keeps artwork callbacks scoped to the currently visible media generation. */
internal class PlayerPaletteCoordinator {
    private var activeKey: String? = null
    private var enabled = false
    private var extractedKey: String? = null

    fun activate(
        key: String?,
        dynamicCoverColors: Boolean,
    ): PlayerPalette {
        activeKey = key
        enabled = dynamicCoverColors
        extractedKey = null
        return PlayerPalette.Static
    }

    fun shouldExtract(key: String): Boolean {
        if (!enabled || activeKey != key || extractedKey == key) return false
        extractedKey = key
        return true
    }

    fun mayApply(key: String): Boolean = enabled && activeKey == key
}
