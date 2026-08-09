package cn.james.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
internal class Media3PlaybackController
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val sourceResolver: PlaybackSourceResolver,
    ) : PlaybackController {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var progressJob: Job? = null

        private val mutableState = MutableStateFlow(PlaybackState())
        override val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

        private val mutableProgress = MutableStateFlow(PlaybackProgress())
        override val progress: StateFlow<PlaybackProgress> = mutableProgress.asStateFlow()

        private val controllerConnection: PlaybackControllerConnection<MediaController> =
            PlaybackControllerConnection(
                createFuture = {
                    MediaController
                        .Builder(
                            context,
                            SessionToken(context, ComponentName(context, MoeKoePlaybackService::class.java)),
                        ).setListener(controllerListener)
                        .buildAsync()
                },
                callbackExecutor = ContextCompat.getMainExecutor(context),
                onConnected = { controller ->
                    controller.addListener(playerListener)
                    mutableState.value = mutableState.value.copy(connection = PlaybackConnectionState.Connected)
                    updateState(controller)
                },
                onConnectionFailed = {
                    mutableState.value =
                        mutableState.value.copy(
                            connection = PlaybackConnectionState.Disconnected,
                            error = PlaybackError.ControllerUnavailable,
                        )
                },
            )

        override suspend fun replaceQueue(
            items: List<PlaybackItem>,
            startIndex: Int,
            play: Boolean,
        ): PlaybackCommandResult =
            withController { controller ->
                val deduplicated = QueuePolicy.deduplicate(items)
                if (deduplicated.isEmpty()) return@withController PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand)
                val requestedId = items.getOrNull(startIndex)?.id
                val resolvedIndex = deduplicated.indexOfFirst { it.id == requestedId }.coerceAtLeast(0)
                controller.setMediaItems(
                    deduplicated.map(PlaybackMediaItemMapper::toRequest),
                    resolvedIndex,
                    0,
                )
                controller.prepare()
                controller.playWhenReady = play
                PlaybackCommandResult.Accepted
            }

        override suspend fun append(items: List<PlaybackItem>): PlaybackCommandResult =
            withController { controller ->
                val existingIds = controller.currentTimelineItems().mapTo(mutableSetOf(), PlaybackItem::id)
                val fresh = items.filter { existingIds.add(it.id) }
                if (fresh.isNotEmpty()) controller.addMediaItems(fresh.map(PlaybackMediaItemMapper::toRequest))
                PlaybackCommandResult.Accepted
            }

        override suspend fun playNext(item: PlaybackItem): PlaybackCommandResult =
            withController { controller ->
                val placement =
                    QueuePolicy.placeAfterCurrent(
                        queue = controller.currentTimelineItems(),
                        currentIndex = controller.currentMediaItemIndex,
                        itemId = item.id,
                    )
                when (val existingIndex = placement.existingIndex) {
                    null -> {
                        controller.addMediaItem(placement.targetIndex, PlaybackMediaItemMapper.toRequest(item))
                    }

                    else -> {
                        if (placement.requiresMove) controller.moveMediaItem(existingIndex, placement.targetIndex)
                    }
                }
                PlaybackCommandResult.Accepted
            }

        override suspend fun playNow(item: PlaybackItem): PlaybackCommandResult {
            val request =
                when (val result = sourceResolver.resolve(item)) {
                    is PlaybackSourceResult.Resolved -> {
                        PlaybackMediaItemMapper.withUri(PlaybackMediaItemMapper.toRequest(item), result.uri)
                    }

                    is PlaybackSourceResult.Unavailable -> {
                        return PlaybackCommandResult.Rejected(result.error)
                    }
                }
            return withController { controller ->
                if (controller.mediaItemCount == 0) {
                    controller.setMediaItem(request)
                    controller.prepare()
                    controller.play()
                    return@withController PlaybackCommandResult.Accepted
                }
                val placement =
                    QueuePolicy.placeAfterCurrent(
                        queue = controller.currentTimelineItems(),
                        currentIndex = controller.currentMediaItemIndex,
                        itemId = item.id,
                    )
                when (val existingIndex = placement.existingIndex) {
                    null -> {
                        controller.addMediaItem(placement.targetIndex, request)
                    }

                    else -> {
                        if (placement.requiresMove) controller.moveMediaItem(existingIndex, placement.targetIndex)
                        controller.replaceMediaItem(placement.targetIndex, request)
                    }
                }
                controller.seekToDefaultPosition(placement.targetIndex)
                controller.prepare()
                controller.play()
                PlaybackCommandResult.Accepted
            }
        }

        override suspend fun playAt(index: Int): PlaybackCommandResult =
            withController { controller ->
                if (index !in 0 until controller.mediaItemCount) {
                    PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand)
                } else {
                    controller.seekToDefaultPosition(index)
                    controller.prepare()
                    controller.play()
                    PlaybackCommandResult.Accepted
                }
            }

        override suspend fun remove(index: Int): PlaybackCommandResult =
            withController { controller ->
                if (index !in 0 until controller.mediaItemCount) {
                    PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand)
                } else {
                    controller.removeMediaItem(index)
                    PlaybackCommandResult.Accepted
                }
            }

        override suspend fun move(
            fromIndex: Int,
            toIndex: Int,
        ): PlaybackCommandResult =
            withController { controller ->
                if (fromIndex !in 0 until controller.mediaItemCount || toIndex !in 0 until controller.mediaItemCount) {
                    PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand)
                } else {
                    controller.moveMediaItem(fromIndex, toIndex)
                    PlaybackCommandResult.Accepted
                }
            }

        override suspend fun clear(): PlaybackCommandResult =
            withController { controller ->
                controller.pause()
                controller.clearMediaItems()
                PlaybackCommandResult.Accepted
            }

        override suspend fun play(): PlaybackCommandResult =
            withController { controller ->
                if (controller.mediaItemCount == 0) {
                    PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand)
                } else {
                    controller.play()
                    PlaybackCommandResult.Accepted
                }
            }

        override suspend fun pause(): PlaybackCommandResult =
            withController { controller ->
                controller.pause()
                PlaybackCommandResult.Accepted
            }

        override suspend fun seekTo(positionMs: Long): PlaybackCommandResult =
            withController { controller ->
                if (controller.currentMediaItem == null) {
                    PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand)
                } else {
                    controller.seekTo(positionMs.coerceIn(0, controller.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
                    PlaybackCommandResult.Accepted
                }
            }

        override suspend fun skipNext(): PlaybackCommandResult =
            withController { controller ->
                if (controller.hasNextMediaItem()) {
                    controller.seekToNextMediaItem()
                    PlaybackCommandResult.Accepted
                } else {
                    PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand)
                }
            }

        override suspend fun skipPrevious(): PlaybackCommandResult =
            withController { controller ->
                if (controller.mediaItemCount == 0) {
                    PlaybackCommandResult.Rejected(PlaybackError.InvalidCommand)
                } else {
                    controller.seekToPrevious()
                    PlaybackCommandResult.Accepted
                }
            }

        override suspend fun setMode(mode: PlaybackMode): PlaybackCommandResult =
            withController { controller ->
                PlaybackModeMapper.apply(controller, mode)
                PlaybackCommandResult.Accepted
            }

        private fun connect() {
            controllerConnection.ensure()
        }

        private suspend fun awaitController(): MediaController? {
            val future = controllerConnection.ensure()
            return suspendCancellableCoroutine { continuation ->
                future.addListener(
                    { continuation.resume(runCatching(future::get).getOrNull()) },
                    ContextCompat.getMainExecutor(context),
                )
            }
        }

        private suspend fun withController(block: (MediaController) -> PlaybackCommandResult): PlaybackCommandResult {
            val controller = awaitController() ?: return PlaybackCommandResult.Rejected(PlaybackError.ControllerUnavailable)
            return withContext(Dispatchers.Main.immediate) {
                runCatching { block(controller) }
                    .getOrElse { PlaybackCommandResult.Rejected(PlaybackError.ControllerUnavailable) }
            }
        }

        private val controllerListener =
            object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    controller.removeListener(playerListener)
                    controllerConnection.clear()
                    progressJob?.cancel()
                    mutableState.value = mutableState.value.copy(connection = PlaybackConnectionState.Disconnected)
                }
            }

        private val playerListener =
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    updateState(player)
                    updateProgress(player)
                    updateProgressLoop(player)
                }

                override fun onPlayerError(error: PlaybackException) {
                    mutableState.value =
                        mutableState.value.copy(
                            error =
                                PlaybackError.PlayerFailure(
                                    itemId = mutableState.value.currentItem?.id,
                                    errorCode = error.errorCode,
                                    recoverable = error.errorCode != PlaybackException.ERROR_CODE_DECODING_FAILED,
                                ),
                        )
                }
            }

        private fun updateState(player: Player) {
            mutableState.value =
                PlaybackState(
                    connection = PlaybackConnectionState.Connected,
                    queue = player.currentTimelineItems(),
                    currentIndex = player.currentMediaItemIndex,
                    isPlaying = player.isPlaying,
                    status =
                        when (player.playbackState) {
                            Player.STATE_BUFFERING -> PlaybackStatus.Buffering
                            Player.STATE_READY -> PlaybackStatus.Ready
                            Player.STATE_ENDED -> PlaybackStatus.Ended
                            else -> PlaybackStatus.Idle
                        },
                    mode = PlaybackModeMapper.from(player),
                    error =
                        player.playerError?.let {
                            PlaybackError.PlayerFailure(
                                itemId = player.currentMediaItem?.mediaId,
                                errorCode = it.errorCode,
                                recoverable = true,
                            )
                        },
                )
        }

        private fun Player.currentTimelineItems(): List<PlaybackItem> =
            (0 until mediaItemCount).map(::getMediaItemAt).mapNotNull(PlaybackMediaItemMapper::toModel)

        private fun updateProgress(player: Player) {
            mutableProgress.value =
                PlaybackProgress(
                    positionMs = player.currentPosition.coerceAtLeast(0),
                    bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0),
                    durationMs = player.duration.takeIf { it > 0 } ?: 0,
                )
        }

        private fun updateProgressLoop(player: Player) {
            if (!player.isPlaying) {
                progressJob?.cancel()
                progressJob = null
                return
            }
            if (progressJob?.isActive == true) return
            progressJob =
                scope.launch {
                    while (isActive && player.isPlaying) {
                        updateProgress(player)
                        delay(500)
                    }
                }
        }

        init {
            connect()
        }
    }
