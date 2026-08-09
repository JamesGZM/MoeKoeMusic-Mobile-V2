package cn.james.music.data.kugou

import cn.james.music.core.model.settings.PlaybackQualityPreference
import cn.james.music.kugou.api.endpoint.KugouPlaybackQuality
import cn.james.music.kugou.api.endpoint.KugouPrivilegeCandidate

internal object KugouPlaybackQualityPolicy {
    fun toKugouQuality(preference: PlaybackQualityPreference): KugouPlaybackQuality =
        when (preference) {
            PlaybackQualityPreference.Standard -> KugouPlaybackQuality.Standard
            PlaybackQualityPreference.High -> KugouPlaybackQuality.High
            PlaybackQualityPreference.Lossless -> KugouPlaybackQuality.Lossless
            PlaybackQualityPreference.HiRes -> KugouPlaybackQuality.HiRes
            PlaybackQualityPreference.ViperAtmos -> KugouPlaybackQuality.ViperAtmos
            PlaybackQualityPreference.ViperClear -> KugouPlaybackQuality.ViperClear
            PlaybackQualityPreference.ViperTape -> KugouPlaybackQuality.ViperTape
        }

    fun fallbackChain(preference: PlaybackQualityPreference): List<KugouPlaybackQuality> =
        KugouPlaybackQuality.entries
            .take(toKugouQuality(preference).ordinal + 1)
            .asReversed()

    fun candidates(
        preference: PlaybackQualityPreference,
        originalHash: String,
        privilegeCandidates: List<KugouPrivilegeCandidate>,
    ): List<KugouPrivilegeCandidate> {
        val byQuality = privilegeCandidates.associateBy(KugouPrivilegeCandidate::quality)
        val chain = fallbackChain(preference)
        val available = chain.mapNotNull(byQuality::get)
        return available.ifEmpty { chain.map { quality -> KugouPrivilegeCandidate(originalHash, quality) } }
    }
}
