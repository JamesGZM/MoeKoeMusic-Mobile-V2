package cn.james.music.core.model.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsModelsTest {
    @Test
    fun lyricsTextSizeDefaultsToStandard() {
        assertEquals(LyricsTextSizePreference.Standard, AppSettings().lyricsTextSize)
    }

    @Test
    fun playbackQualityDefaultsToStandardAndUsesStableQualityOrder() {
        assertEquals(PlaybackQualityPreference.Standard, AppSettings().playbackQuality)
        assertEquals(
            listOf(
                PlaybackQualityPreference.Standard,
                PlaybackQualityPreference.High,
                PlaybackQualityPreference.Lossless,
                PlaybackQualityPreference.HiRes,
                PlaybackQualityPreference.ViperAtmos,
                PlaybackQualityPreference.ViperClear,
                PlaybackQualityPreference.ViperTape,
            ),
            PlaybackQualityPreference.entries.toList(),
        )
    }
}
