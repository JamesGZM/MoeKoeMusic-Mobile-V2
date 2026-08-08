package cn.james.music.playback

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

internal class PlaybackControllerConnection<T>(
    private val createFuture: () -> ListenableFuture<T>,
    private val callbackExecutor: Executor,
    private val onConnected: (T) -> Unit,
    private val onConnectionFailed: () -> Unit,
) {
    private val lock = Any()
    private var future: ListenableFuture<T>? = null

    fun ensure(): ListenableFuture<T> =
        synchronized(lock) {
            future ?: createFuture().also { created ->
                future = created
                created.addListener(
                    {
                        runCatching(created::get)
                            .onSuccess(onConnected)
                            .onFailure {
                                synchronized(lock) {
                                    if (future === created) future = null
                                }
                                onConnectionFailed()
                            }
                    },
                    callbackExecutor,
                )
            }
        }

    fun clear() {
        synchronized(lock) { future = null }
    }
}
