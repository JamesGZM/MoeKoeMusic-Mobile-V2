package cn.james.music.playback

import androidx.media3.common.Player
import cn.james.music.core.model.playback.PlaybackMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackModeMapperTest {
    @Test
    fun eachModeMapsToOneSupportedPlayerConfiguration() {
        assertSettings(PlaybackMode.Sequential, false, Player.REPEAT_MODE_OFF)
        assertSettings(PlaybackMode.RepeatAll, false, Player.REPEAT_MODE_ALL)
        assertSettings(PlaybackMode.RepeatOne, false, Player.REPEAT_MODE_ONE)
        assertSettings(PlaybackMode.Shuffle, true, Player.REPEAT_MODE_ALL)
    }

    @Test
    fun shuffleTakesPriorityWhenReadingExternalPlayerState() {
        assertEquals(PlaybackMode.Shuffle, PlaybackModeMapper.from(true, Player.REPEAT_MODE_ONE))
    }

    private fun assertSettings(
        mode: PlaybackMode,
        shuffle: Boolean,
        repeatMode: Int,
    ) {
        assertEquals(
            PlaybackModeMapper.Settings(shuffle, repeatMode),
            PlaybackModeMapper.settingsFor(mode),
        )
    }
}
