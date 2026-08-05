package cn.james.music.data.local

import cn.james.music.core.model.local.LocalMusicRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocalMusicDebugEntryPoint {
    fun localImportGateway(): LocalImportGateway

    fun localMusicRepository(): LocalMusicRepository

    fun localImportDebugProbe(): LocalImportDebugProbe
}
