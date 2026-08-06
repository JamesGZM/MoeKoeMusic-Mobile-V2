package cn.james.music.data.lyrics

import cn.james.music.core.database.MoeKoeDatabase
import cn.james.music.core.database.lyrics.LyricsCacheDao
import cn.james.music.core.model.lyrics.LyricsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LyricsParserDispatcher

@Module
@InstallIn(SingletonComponent::class)
object LyricsDataModule {
    @Provides
    fun provideLyricsCacheDao(database: MoeKoeDatabase): LyricsCacheDao = database.lyricsCacheDao()

    @Provides
    @LyricsParserDispatcher
    fun provideLyricsParserDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LyricsRepositoryBindings {
    @Binds
    abstract fun bindLyricsRepository(implementation: KugouLyricsRepository): LyricsRepository
}
