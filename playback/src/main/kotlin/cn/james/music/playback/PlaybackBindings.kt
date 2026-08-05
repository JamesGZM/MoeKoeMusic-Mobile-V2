package cn.james.music.playback

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackBindings {
    @Binds
    abstract fun bindSourceResolver(implementation: DefaultPlaybackSourceResolver): PlaybackSourceResolver

    @Binds
    abstract fun bindPlaybackController(implementation: Media3PlaybackController): PlaybackController
}
