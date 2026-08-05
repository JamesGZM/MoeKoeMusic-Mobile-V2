package cn.james.music.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MoeKoePlaybackService : MediaLibraryService() {
    @Inject lateinit var snapshotStore: PlaybackSnapshotStore

    @Inject internal lateinit var sourceResolver: PlaybackSourceResolver

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private var snapshotJob: Job? = null
    private var progressCheckpointJob: Job? = null
    private var errorAdvanceJob: Job? = null
    private val initialRestoreComplete = CompletableDeferred<Unit>()
    private val errorPolicy = ConsecutivePlaybackErrorPolicy()
    private val addressRefreshPolicy = PlaybackAddressRefreshPolicy()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        player =
            ExoPlayer
                .Builder(this)
                .build()
                .apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true,
                    )
                    setHandleAudioBecomingNoisy(true)
                    setWakeMode(C.WAKE_MODE_LOCAL)
                    addListener(servicePlayerListener)
                }

        val sessionBuilder = MediaLibrarySession.Builder(this, player, SessionCallback())
        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            sessionBuilder.setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        mediaSession = sessionBuilder.build()
        restoreSnapshot()
        startProgressCheckpoints()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = mediaSession

    override fun onDestroy() {
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(1_000) { persistSnapshotNow() }
        }
        progressCheckpointJob?.cancel()
        errorAdvanceJob?.cancel()
        snapshotJob?.cancel()
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun restoreSnapshot() {
        serviceScope.launch {
            try {
                val snapshot = snapshotStore.load() ?: return@launch
                val resolved = resolve(snapshot.queue)
                if (resolved.isEmpty()) {
                    snapshotStore.clear()
                    return@launch
                }
                val startIndex = snapshot.currentIndex.coerceIn(resolved.indices)
                PlaybackModeMapper.apply(player, snapshot.mode)
                player.setMediaItems(resolved, startIndex, snapshot.positionMs)
                player.playWhenReady = false
                player.prepare()
            } finally {
                initialRestoreComplete.complete(Unit)
            }
        }
    }

    private fun startProgressCheckpoints() {
        progressCheckpointJob =
            serviceScope.launch {
                while (true) {
                    delay(5_000)
                    if (player.isPlaying) persistSnapshotNow()
                }
            }
    }

    private fun scheduleSnapshot() {
        snapshotJob?.cancel()
        snapshotJob = serviceScope.launch { persistSnapshotNow() }
    }

    private suspend fun persistSnapshotNow() {
        val queue =
            (0 until player.mediaItemCount)
                .map(player::getMediaItemAt)
                .mapNotNull(PlaybackMediaItemMapper::toModel)
        if (queue.isEmpty()) {
            snapshotStore.clear()
            return
        }
        snapshotStore.save(
            PlaybackSnapshot(
                queue = queue,
                currentIndex = player.currentMediaItemIndex.coerceIn(queue.indices),
                positionMs = player.currentPosition.coerceAtLeast(0),
                mode = PlaybackModeMapper.from(player),
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun resolve(items: List<cn.james.music.core.model.playback.PlaybackItem>): List<MediaItem> =
        items.mapNotNull { item ->
            when (val result = sourceResolver.resolve(item)) {
                is PlaybackSourceResult.Resolved -> {
                    PlaybackMediaItemMapper.withUri(PlaybackMediaItemMapper.toRequest(item), result.uri)
                }

                is PlaybackSourceResult.Unavailable -> {
                    null
                }
            }
        }

    private val servicePlayerListener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                if (
                    events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_REPEAT_MODE_CHANGED,
                        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                        Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    )
                ) {
                    scheduleSnapshot()
                }
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    addressRefreshPolicy.onMediaItemTransition(player.currentMediaItem?.mediaId)
                }
                if (player.playbackState == Player.STATE_READY) errorPolicy.reset()
            }

            override fun onPlayerError(error: PlaybackException) {
                errorAdvanceJob?.cancel()
                val item = player.currentMediaItem?.let(PlaybackMediaItemMapper::toModel)
                if (item != null && addressRefreshPolicy.shouldRefresh(item, error.errorCode)) {
                    errorAdvanceJob = serviceScope.launch { refreshCurrentAddress(item) }
                    return
                }
                handleUnrecoverablePlayerError()
            }
        }

    private suspend fun refreshCurrentAddress(item: cn.james.music.core.model.playback.PlaybackItem) {
        when (val result = sourceResolver.resolve(item)) {
            is PlaybackSourceResult.Resolved -> {
                val index = player.currentMediaItemIndex
                if (index !in 0 until player.mediaItemCount || player.currentMediaItem?.mediaId != item.id) return
                val positionMs = player.currentPosition.coerceAtLeast(0)
                val refreshed = PlaybackMediaItemMapper.withUri(PlaybackMediaItemMapper.toRequest(item), result.uri)
                player.replaceMediaItem(index, refreshed)
                player.seekTo(index, positionMs)
                player.prepare()
                player.play()
            }

            is PlaybackSourceResult.Unavailable -> handleUnrecoverablePlayerError()
        }
    }

    private fun handleUnrecoverablePlayerError() {
        if (!errorPolicy.shouldAdvance(player.mediaItemCount)) {
            player.pause()
            return
        }
        errorAdvanceJob =
            serviceScope.launch {
                delay(ERROR_ADVANCE_DELAY_MS)
                val nextIndex = (player.currentMediaItemIndex + 1) % player.mediaItemCount
                player.seekToDefaultPosition(nextIndex)
                player.prepare()
                player.play()
            }
    }

    @OptIn(UnstableApi::class)
    private inner class SessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult =
            if (controller.packageName == packageName || controller.isTrusted) {
                MediaSession.ConnectionResult.accept(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS,
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
                )
            } else {
                MediaSession.ConnectionResult.reject()
            }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            serviceScope.launch {
                initialRestoreComplete.await()
                future.set(
                    mediaItems.mapNotNull { request ->
                        request.localConfiguration?.uri?.let { request }
                            ?: PlaybackMediaItemMapper.toModel(request)?.let { model -> resolve(listOf(model)).firstOrNull() }
                    },
                )
            }
            return future
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                initialRestoreComplete.await()
                val snapshot = snapshotStore.load()
                val items = snapshot?.let { resolve(it.queue) }.orEmpty()
                if (snapshot == null || items.isEmpty()) {
                    future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0))
                } else {
                    PlaybackModeMapper.apply(player, snapshot.mode)
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            items,
                            snapshot.currentIndex.coerceIn(items.indices),
                            snapshot.positionMs,
                        ),
                    )
                }
            }
            return future
        }
    }

    private companion object {
        const val ERROR_ADVANCE_DELAY_MS = 1_500L
    }
}
