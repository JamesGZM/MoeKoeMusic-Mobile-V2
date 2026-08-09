package cn.james.music.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLyricsAutoFollowCoordinatorTest {
    @Test
    fun allowsTheCurrentActiveLineBeforeAnyUserDrag() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()

        assertEquals(PlayerLyricsAutoFollowRequest(activeIndex = 2, generation = 0), coordinator.request(2, 4, nowMs = 0))
    }

    @Test
    fun userDragPausesUntilTheExactMobileResumeBoundary() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()
        coordinator.onUserVerticalDragStarted(nowMs = 10_000)

        assertNull(coordinator.request(1, 3, nowMs = 13_499))
        assertEquals(1L, coordinator.resumeDelayRemainingMs(13_499))
        assertEquals(PlayerLyricsAutoFollowRequest(1, 1), coordinator.request(1, 3, nowMs = 13_500))
    }

    @Test
    fun consecutiveUserDragsResetTheResumeDelay() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()
        coordinator.onUserVerticalDragStarted(nowMs = 0)
        coordinator.onUserVerticalDragStarted(nowMs = 2_000)

        assertNull(coordinator.request(1, 3, nowMs = 3_500))
        assertEquals(PlayerLyricsAutoFollowRequest(1, 2), coordinator.request(1, 3, nowMs = 5_500))
    }

    @Test
    fun resumedRequestUsesTheLatestActiveLine() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()
        coordinator.onUserVerticalDragStarted(nowMs = 100)

        assertEquals(PlayerLyricsAutoFollowRequest(4, 1), coordinator.request(4, 6, nowMs = 3_600))
    }

    @Test
    fun invalidOrAbsentActiveLineNeverRequestsScroll() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()

        assertNull(coordinator.request(-1, 4, nowMs = 0))
        assertNull(coordinator.request(4, 4, nowMs = 0))
        assertNull(coordinator.request(0, 0, nowMs = 0))
    }

    @Test
    fun programmaticRequestDoesNotStartOrExtendTheUserPause() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()
        val request = coordinator.request(0, 1, nowMs = 0)

        assertEquals(PlayerLyricsAutoFollowRequest(0, 0), request)
        assertEquals(PlayerLyricsAutoFollowRequest(0, 0), coordinator.request(0, 1, nowMs = 1_000))
    }

    @Test
    fun userDragInvalidatesAnOlderProgrammaticRequest() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()
        val oldRequest = requireNotNull(coordinator.request(0, 2, nowMs = 0))

        coordinator.onUserVerticalDragStarted(nowMs = 1)

        assertFalse(coordinator.isCurrent(oldRequest))
        assertTrue(coordinator.isCurrent(requireNotNull(coordinator.request(1, 2, nowMs = 3_501))))
    }

    @Test
    fun replacingOrDisposingTheDocumentInvalidatesOldRequestsAndClearsThePause() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()
        coordinator.onUserVerticalDragStarted(nowMs = 0)
        val beforeReplacement = PlayerLyricsAutoFollowRequest(0, 1)

        coordinator.onDocumentChanged()

        assertFalse(coordinator.isCurrent(beforeReplacement))
        assertEquals(PlayerLyricsAutoFollowRequest(1, 2), coordinator.request(1, 2, nowMs = 1))
        val currentRequest = requireNotNull(coordinator.request(1, 2, nowMs = 1))
        coordinator.onDisposed()
        assertFalse(coordinator.isCurrent(currentRequest))
    }

    @Test
    fun geometryChangeInvalidatesOldAnimationWithoutClearingTheUserPause() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()
        coordinator.onUserVerticalDragStarted(nowMs = 0)
        val beforeGeometryChange = PlayerLyricsAutoFollowRequest(0, 1)

        coordinator.onLayoutChanged()

        assertFalse(coordinator.isCurrent(beforeGeometryChange))
        assertNull(coordinator.request(0, 1, nowMs = 3_499))
        assertEquals(PlayerLyricsAutoFollowRequest(0, 2), coordinator.request(0, 1, nowMs = 3_500))
    }

    @Test
    fun sameDocumentGeometryCanBeReevaluatedAfterSupplementalTextOrSizeChanges() {
        assertTrue(shouldReserveLyricsAutoFollowSpace(lineBottomPx = 801, viewportHeightPx = 800))
        assertFalse(shouldReserveLyricsAutoFollowSpace(lineBottomPx = 800, viewportHeightPx = 800))
        assertFalse(shouldReserveLyricsAutoFollowSpace(lineBottomPx = 0, viewportHeightPx = 800))
    }

    @Test
    fun dragThatEndsWithoutFlingLetsTheNextDragResetThePause() {
        val coordinator = PlayerLyricsAutoFollowCoordinator()
        val observer = PlayerLyricsUserDragObserver(coordinator)

        assertTrue(observer.onUserVerticalScroll(nowMs = 0))
        observer.onScrollIdle()
        assertTrue(observer.onUserVerticalScroll(nowMs = 1_000))

        assertNull(coordinator.request(0, 1, nowMs = 4_499))
        assertEquals(PlayerLyricsAutoFollowRequest(0, 2), coordinator.request(0, 1, nowMs = 4_500))
    }
}
