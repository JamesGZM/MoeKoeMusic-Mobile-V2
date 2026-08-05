package cn.james.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.online.Song
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackCommandResult
import cn.james.music.playback.PlaybackController
import cn.james.music.playback.PlaybackError
import cn.james.music.playback.PlaybackProgress
import cn.james.music.playback.PlaybackSourceError
import cn.james.music.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppPlaybackViewModel
    @Inject
    constructor(
        private val playbackController: PlaybackController,
    ) : ViewModel() {
        private var noticeId = 0L
        private var retryItem: PlaybackItem? = null
        private val mutableNotice = MutableStateFlow<PlaybackNotice?>(null)
        val notice: StateFlow<PlaybackNotice?> = mutableNotice.asStateFlow()

        val state: StateFlow<PlaybackState> =
            playbackController.state.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                PlaybackState(),
            )

        val progress: StateFlow<PlaybackProgress> = playbackController.progress

        fun play(song: Song) {
            play(song.toPlaybackItem())
        }

        fun retryPlayback() {
            retryItem?.let(::play)
        }

        fun dismissNotice(id: Long) {
            if (mutableNotice.value?.id == id) mutableNotice.value = null
        }

        private fun play(item: PlaybackItem) {
            retryItem = item
            viewModelScope.launch {
                playbackController.playNow(item).reportFailure()
            }
        }

        fun togglePlayback() {
            viewModelScope.launch {
                if (state.value.isPlaying) playbackController.pause() else playbackController.play()
            }
        }

        fun playAt(index: Int) {
            viewModelScope.launch { playbackController.playAt(index) }
        }

        fun removeAt(index: Int) {
            viewModelScope.launch { playbackController.remove(index) }
        }

        fun clearQueue() {
            viewModelScope.launch { playbackController.clear() }
        }

        private fun PlaybackCommandResult.reportFailure() {
            if (this is PlaybackCommandResult.Rejected) showNotice(error)
        }

        private fun showNotice(error: PlaybackError) {
            mutableNotice.value = PlaybackNotice(id = ++noticeId, error = error)
        }

        init {
            viewModelScope.launch {
                playbackController.state
                    .map { playbackState -> playbackState.error }
                    .distinctUntilChanged()
                    .filterNotNull()
                    .collect(::showNotice)
            }
        }
    }

data class PlaybackNotice(
    val id: Long,
    val error: PlaybackError,
) {
    val canRetry: Boolean get() = error.recoverable

    val message: String
        get() =
            when (error) {
                is PlaybackError.SourceUnavailable -> error.reason.message
                is PlaybackError.PlayerFailure -> "播放连接异常，正在尝试恢复"
                PlaybackError.ControllerUnavailable -> "播放器暂时不可用"
                PlaybackError.InvalidCommand -> "当前操作无法执行"
            }
}

private val PlaybackSourceError.message: String
    get() =
        when (this) {
            PlaybackSourceError.NoCopyright -> "该歌曲暂无可用版权"
            PlaybackSourceError.VipRequired -> "该歌曲需要 VIP 权益"
            PlaybackSourceError.Offline -> "当前没有网络连接"
            PlaybackSourceError.Timeout -> "播放地址请求超时"
            PlaybackSourceError.Connection -> "无法连接音乐服务"
            PlaybackSourceError.VerificationRequired -> "服务需要安全验证，请稍后重试"
            PlaybackSourceError.AuthenticationRequired -> "请登录后播放该歌曲"
            PlaybackSourceError.ServiceUnavailable -> "音乐服务暂时不可用"
            PlaybackSourceError.Protocol -> "播放协议发生变化，请更新应用"
            PlaybackSourceError.SessionInitialization -> "播放会话初始化失败"
            PlaybackSourceError.InvalidSource -> "歌曲来源已经失效"
        }

private fun Song.toPlaybackItem() =
    PlaybackItem(
        id = "kugou:$hash",
        title = title,
        artist = artistName.ifBlank { "未知艺术家" },
        albumTitle = albumTitle,
        source = PlaybackSource.Kugou(hash),
        artwork = artworkUrl?.let { runCatching { PlaybackArtwork.Remote(it) }.getOrNull() },
    )
