package cn.james.music.playback

internal object PlaybackRuntimeQualityPolicy {
    fun currentQuality(
        qualities: List<ResolvedPlaybackQuality?>,
        currentIndex: Int,
    ): ResolvedPlaybackQuality? = qualities.getOrNull(currentIndex)
}
