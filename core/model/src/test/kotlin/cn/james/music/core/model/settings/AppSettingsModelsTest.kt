package cn.james.music.core.model.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsModelsTest {
    @Test
    fun lyricsTextSizeDefaultsToStandard() {
        assertEquals(LyricsTextSizePreference.Standard, AppSettings().lyricsTextSize)
    }
}
