package cn.james.music.playback

import androidx.media3.common.Player
import cn.james.music.core.model.playback.PlaybackMode

internal object PlaybackModeMapper {
    data class Settings(
        val shuffleEnabled: Boolean,
        val repeatMode: Int,
    )

    fun settingsFor(mode: PlaybackMode): Settings =
        Settings(
            shuffleEnabled = mode == PlaybackMode.Shuffle,
            repeatMode =
                when (mode) {
                    PlaybackMode.Sequential -> Player.REPEAT_MODE_OFF
                    PlaybackMode.RepeatAll, PlaybackMode.Shuffle -> Player.REPEAT_MODE_ALL
                    PlaybackMode.RepeatOne -> Player.REPEAT_MODE_ONE
                },
        )

    fun apply(
        player: Player,
        mode: PlaybackMode,
    ) {
        val settings = settingsFor(mode)
        player.shuffleModeEnabled = settings.shuffleEnabled
        player.repeatMode = settings.repeatMode
    }

    fun from(player: Player): PlaybackMode = from(player.shuffleModeEnabled, player.repeatMode)

    fun from(
        shuffleEnabled: Boolean,
        repeatMode: Int,
    ): PlaybackMode =
        when {
            shuffleEnabled -> PlaybackMode.Shuffle
            repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.RepeatOne
            repeatMode == Player.REPEAT_MODE_ALL -> PlaybackMode.RepeatAll
            else -> PlaybackMode.Sequential
        }
}
