package cn.james.music

import cn.james.music.core.designsystem.component.MoeMiniPlayerSemanticsUi
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.feature.player.PlayerArtworkStorageRef
import cn.james.music.feature.player.PlayerArtworkUiModel
import cn.james.music.feature.player.PlayerPlaybackModeUi
import cn.james.music.feature.player.PlayerQueueAction
import cn.james.music.playback.PlaybackConnectionState
import cn.james.music.playback.PlaybackState
import cn.james.music.playback.PlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackShellTest {
    @Test
    fun domainPlaybackMapsEveryPlayerFieldToPureUiModels() {
        val remote = item("remote", artwork = PlaybackArtwork.Remote("https://example.test/cover.webp"))
        val local = item("local", artwork = PlaybackArtwork.AppFile("artwork/local.webp"))
        val state =
            PlaybackState(
                connection = PlaybackConnectionState.Connected,
                queue = listOf(remote, local),
                currentIndex = 0,
                isPlaying = true,
                status = PlaybackStatus.Buffering,
                mode = PlaybackMode.Shuffle,
            )

        val player = state.toPlayerUiState()
        val queue = state.toPlayerQueueUiState(currentDurationMs = 184_000)

        assertEquals(remote.id, player.item?.id)
        assertEquals(remote.title, player.item?.title)
        assertEquals(remote.artist, player.item?.artist)
        assertEquals(PlayerArtworkUiModel.Remote("https://example.test/cover.webp"), player.item?.artwork)
        assertTrue(player.isPlaying)
        assertTrue(player.isBuffering)
        assertTrue(player.controlsEnabled)
        assertEquals(PlayerPlaybackModeUi.Shuffle, player.mode)
        assertEquals(listOf("remote", "local"), queue.items.map { it.id })
        assertEquals(listOf("Title remote", "Title local"), queue.items.map { it.title })
        assertEquals(listOf("Artist remote", "Artist local"), queue.items.map { it.artist })
        assertEquals(listOf("3:04", null), queue.items.map { it.durationLabel })
        assertEquals(
            PlayerArtworkUiModel.AppStorage(PlayerArtworkStorageRef("artwork/local.webp")),
            queue.items[1].artwork,
        )
        assertEquals(remote.id, queue.currentItemId)
        assertEquals(PlayerPlaybackModeUi.Shuffle, queue.mode)
        assertEquals("Album remote", queue.sourceLabel)
        assertEquals(PlayerArtworkUiModel.None, item("none").toPlayerItemUiModel().artwork)
    }

    @Test
    fun everyPlaybackModeMapsWithoutLeakingDomainMode() {
        assertEquals(PlayerPlaybackModeUi.Sequential, PlaybackMode.Sequential.toPlayerPlaybackModeUi())
        assertEquals(PlayerPlaybackModeUi.RepeatAll, PlaybackMode.RepeatAll.toPlayerPlaybackModeUi())
        assertEquals(PlayerPlaybackModeUi.RepeatOne, PlaybackMode.RepeatOne.toPlayerPlaybackModeUi())
        assertEquals(PlayerPlaybackModeUi.Shuffle, PlaybackMode.Shuffle.toPlayerPlaybackModeUi())
    }

    @Test
    fun queueDurationOnlyBelongsToCurrentStableItem() {
        val state = PlaybackState(queue = listOf(item("first"), item("current"), item("last")), currentIndex = 1)

        val queue = state.toPlayerQueueUiState(currentDurationMs = 65_000)

        assertEquals(listOf(null, "1:05", null), queue.items.map { it.durationLabel })
    }

    @Test
    fun miniPlayerMapsCurrentPlaybackToOpaqueArtworkKeyAndCompletePresentationModel() {
        val current = item("current", artwork = PlaybackArtwork.Remote("https://example.test/cover.webp"))
        val state = PlaybackState(queue = listOf(current), currentIndex = 0, isPlaying = true)
        val semantics =
            MoeMiniPlayerSemanticsUi(
                play = "播放",
                pause = "暂停",
                previous = "上一首",
                next = "下一首",
                queue = "播放队列",
            )

        val model =
            state.toMoeMiniPlayerUiModel(
                positionMs = 84_000,
                durationMs = 248_000,
                badgeLabel = "标准",
                semantics = semantics,
            )

        requireNotNull(model)
        assertEquals(current.id, model.artworkKey)
        assertEquals("Title current", model.title)
        assertEquals("Artist current", model.artist)
        assertEquals("Title current 封面", model.artworkContentDescription)
        assertEquals("1:24", model.positionLabel)
        assertEquals("4:08", model.durationLabel)
        assertEquals(84_000f / 248_000f, model.progressFraction)
        assertTrue(model.isPlaying)
        assertTrue(model.canOpenPlayer)
        assertEquals("标准", model.badgeLabel)
        assertEquals(semantics, model.semantics)
    }

    @Test
    fun miniPlayerIsAbsentWhenPlaybackHasNoCurrentItem() {
        assertNull(
            PlaybackState().toMoeMiniPlayerUiModel(
                positionMs = 0,
                durationMs = 0,
                badgeLabel = "标准",
                semantics =
                    MoeMiniPlayerSemanticsUi(
                        play = "播放",
                        pause = "暂停",
                        previous = "上一首",
                        next = "下一首",
                        queue = "播放队列",
                    ),
            ),
        )
    }

    @Test
    fun stableIdActionsResolveAgainstLatestQueueAndUnknownIdsAreNoOp() {
        val state = PlaybackState(queue = listOf(item("first"), item("second")), currentIndex = 0)

        assertEquals(ResolvedPlayerQueueAction.PlayAt(1), state.resolvePlayerQueueAction(PlayerQueueAction.Play("second")))
        assertEquals(ResolvedPlayerQueueAction.RemoveAt(0), state.resolvePlayerQueueAction(PlayerQueueAction.Remove("first")))
        assertEquals(ResolvedPlayerQueueAction.Clear, state.resolvePlayerQueueAction(PlayerQueueAction.Clear))
        assertEquals(ResolvedPlayerQueueAction.ChangeMode, state.resolvePlayerQueueAction(PlayerQueueAction.ChangeMode))
        assertEquals(ResolvedPlayerQueueAction.Dismiss, state.resolvePlayerQueueAction(PlayerQueueAction.Dismiss))
        assertNull(state.resolvePlayerQueueAction(PlayerQueueAction.Play("removed")))
        assertNull(state.resolvePlayerQueueAction(PlayerQueueAction.Remove("removed")))
    }

    private fun item(
        id: String,
        artwork: PlaybackArtwork? = null,
    ) = PlaybackItem(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        albumTitle = "Album $id",
        source = PlaybackSource.FoundationDemo,
        artwork = artwork,
    )
}
