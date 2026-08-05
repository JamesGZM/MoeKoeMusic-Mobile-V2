package cn.james.music

import cn.james.music.playback.PlaybackController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlaybackDebugEntryPoint {
    fun playbackController(): PlaybackController
}
