package cn.james.music.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueReorderCoordinatorTest {
    @Test
    fun stableIdReorderKeepsAuthoritativeRollbackAndAcceptedAlignment() {
        val coordinator = PlayerQueueReorderCoordinator()
        coordinator.updateAuthoritative(ids = listOf("one", "two", "three", "four"), reorderEnabled = true)

        assertTrue(coordinator.beginDrag("one", reorderEnabled = true))
        assertTrue(coordinator.moveDraggedTo(2))
        assertEquals(
            PlayerQueueMoveRequest(draggedId = "one", beforeId = "four"),
            coordinator.drop(),
        )
        assertEquals(listOf("two", "three", "one", "four"), coordinator.visibleIds)

        coordinator.onSubmitResult(PlayerQueueMoveResult.Accepted)
        coordinator.updateAuthoritative(ids = listOf("two", "three", "one", "four"), reorderEnabled = true)
        assertEquals(listOf("two", "three", "one", "four"), coordinator.visibleIds)
        assertFalse(coordinator.isReorderInProgress)
    }

    @Test
    fun downToEndSubmitsNullAnchorOnlyOnceAndNoMovementIsNoOp() {
        val coordinator = PlayerQueueReorderCoordinator()
        coordinator.updateAuthoritative(ids = listOf("one", "two", "three"), reorderEnabled = true)

        assertTrue(coordinator.beginDrag("one", reorderEnabled = true))
        coordinator.moveDraggedTo(2)
        assertEquals(PlayerQueueMoveRequest("one", null), coordinator.drop())
        assertNull(coordinator.drop())

        coordinator.updateAuthoritative(ids = listOf("one", "two", "three"), reorderEnabled = true)
        coordinator.onSubmitResult(PlayerQueueMoveResult.Rejected)
        assertEquals(listOf("one", "two", "three"), coordinator.visibleIds)
        assertTrue(coordinator.beginDrag("two", reorderEnabled = true))
        assertNull(coordinator.drop())
    }

    @Test
    fun externalAuthorityChangeAndStaleResultRollbackTemporaryOrder() {
        val coordinator = PlayerQueueReorderCoordinator()
        coordinator.updateAuthoritative(ids = listOf("one", "two", "three"), reorderEnabled = true)

        coordinator.beginDrag("three", reorderEnabled = true)
        coordinator.moveDraggedTo(0)
        coordinator.updateAuthoritative(ids = listOf("one", "two"), reorderEnabled = true)
        assertEquals(listOf("one", "two"), coordinator.visibleIds)
        assertFalse(coordinator.isReorderInProgress)

        coordinator.updateAuthoritative(ids = listOf("one", "two", "three"), reorderEnabled = true)
        coordinator.beginDrag("three", reorderEnabled = true)
        coordinator.moveDraggedTo(0)
        coordinator.drop()
        coordinator.onSubmitResult(PlayerQueueMoveResult.Stale)
        assertEquals(listOf("one", "two", "three"), coordinator.visibleIds)
    }

    @Test
    fun emptySingleAndShuffleDoNotStartAndAccessibilityUsesSameStableIdRequest() {
        val coordinator = PlayerQueueReorderCoordinator()
        coordinator.updateAuthoritative(emptyList(), reorderEnabled = false)
        assertFalse(coordinator.beginDrag("one", reorderEnabled = false))
        coordinator.updateAuthoritative(ids = listOf("one"), reorderEnabled = false)
        assertFalse(coordinator.beginDrag("one", reorderEnabled = false))
        coordinator.updateAuthoritative(ids = listOf("one", "two"), reorderEnabled = true)
        assertEquals(
            PlayerQueueMoveRequest(draggedId = "two", beforeId = "one"),
            coordinator.submitAccessibilityMove("two", targetIndex = 0, reorderEnabled = true),
        )
    }

    @Test
    fun edgeAutoScrollOnlyRequestsDirectionNearViewportEdges() {
        val coordinator = PlayerQueueReorderCoordinator()

        assertEquals(-1, coordinator.edgeAutoScrollDirection(10f, 0f, 200f, 48f))
        assertEquals(0, coordinator.edgeAutoScrollDirection(100f, 0f, 200f, 48f))
        assertEquals(1, coordinator.edgeAutoScrollDirection(195f, 0f, 200f, 48f))
    }

    @Test
    fun measuredRowsRecalculateTheTargetAfterEachAutoScrollFrame() {
        val coordinator = PlayerQueueReorderCoordinator()

        assertEquals(
            2,
            coordinator.targetIndexAt(
                pointerY = 116f,
                visibleItems =
                    listOf(
                        QueueReorderVisibleItem(index = 0, offset = 0, size = 57),
                        QueueReorderVisibleItem(index = 1, offset = 57, size = 57),
                        QueueReorderVisibleItem(index = 2, offset = 114, size = 57),
                    ),
            ),
        )
        assertEquals(
            3,
            coordinator.targetIndexAt(
                pointerY = 116f,
                visibleItems =
                    listOf(
                        QueueReorderVisibleItem(index = 1, offset = -20, size = 57),
                        QueueReorderVisibleItem(index = 2, offset = 37, size = 57),
                        QueueReorderVisibleItem(index = 3, offset = 94, size = 57),
                    ),
            ),
        )
    }
}
