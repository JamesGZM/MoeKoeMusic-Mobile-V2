package cn.james.music.data.kugou

import cn.james.music.core.model.settings.PlaybackQualityPreference
import cn.james.music.kugou.api.endpoint.KugouPlaybackQuality
import cn.james.music.kugou.api.endpoint.KugouPrivilegeCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class KugouPlaybackQualityPolicyTest {
    @Test
    fun everyPreferenceMapsToTheFixedProtocolQualityOrder() {
        assertEquals(
            KugouPlaybackQuality.entries.toList(),
            PlaybackQualityPreference.entries.map(KugouPlaybackQualityPolicy::toKugouQuality),
        )
    }

    @Test
    fun fallbackChainRunsFromPreferenceToStandard() {
        assertEquals(
            listOf(
                KugouPlaybackQuality.ViperClear,
                KugouPlaybackQuality.ViperAtmos,
                KugouPlaybackQuality.HiRes,
                KugouPlaybackQuality.Lossless,
                KugouPlaybackQuality.High,
                KugouPlaybackQuality.Standard,
            ),
            KugouPlaybackQualityPolicy.fallbackChain(PlaybackQualityPreference.ViperClear),
        )
    }

    @Test
    fun candidatesUseOnlyExistingValuesInPreferenceChain() {
        val candidates =
            KugouPlaybackQualityPolicy.candidates(
                preference = PlaybackQualityPreference.HiRes,
                originalHash = "original",
                privilegeCandidates =
                    listOf(
                        KugouPrivilegeCandidate("too-high", KugouPlaybackQuality.ViperTape),
                        KugouPrivilegeCandidate("flac", KugouPlaybackQuality.Lossless),
                        KugouPrivilegeCandidate("standard", KugouPlaybackQuality.Standard),
                    ),
            )

        assertEquals(
            listOf(
                KugouPrivilegeCandidate("flac", KugouPlaybackQuality.Lossless),
                KugouPrivilegeCandidate("standard", KugouPlaybackQuality.Standard),
            ),
            candidates,
        )
    }

    @Test
    fun noApplicableCandidatesUsesTheOriginalHashForEntireChain() {
        assertEquals(
            listOf(
                KugouPrivilegeCandidate("original", KugouPlaybackQuality.High),
                KugouPrivilegeCandidate("original", KugouPlaybackQuality.Standard),
            ),
            KugouPlaybackQualityPolicy.candidates(
                preference = PlaybackQualityPreference.High,
                originalHash = "original",
                privilegeCandidates = listOf(KugouPrivilegeCandidate("too-high", KugouPlaybackQuality.ViperTape)),
            ),
        )
    }
}
