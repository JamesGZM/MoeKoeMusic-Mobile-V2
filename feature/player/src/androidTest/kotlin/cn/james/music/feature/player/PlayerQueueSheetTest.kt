package cn.james.music.feature.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.advanceEventTime
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerQueueSheetTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun handleKeeps48DpTouchSemanticsAndBoundedAccessibilityMoves() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Dark) {
                PlayerQueueSheetContent(model = queue(), onEvent = { PlayerQueueActionResult.Handled })
            }
        }

        val firstHandle = composeRule.onNodeWithContentDescription("拖动排序 First").assertIsDisplayed()
        val bounds = firstHandle.getUnclippedBoundsInRoot()
        assertTrue(bounds.right.value - bounds.left.value >= 48f)
        assertTrue(bounds.bottom.value - bounds.top.value >= 48f)
        val removeBounds =
            composeRule
                .onNodeWithContentDescription("从队列移除 First")
                .getUnclippedBoundsInRoot()
        assertTrue(bounds.right.value <= removeBounds.left.value)
        assertEquals(
            listOf("下移"),
            firstHandle.fetchSemanticsNode().config[SemanticsActions.CustomActions].map { it.label },
        )
        assertEquals(
            listOf("上移"),
            composeRule
                .onNodeWithContentDescription("拖动排序 Second")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .map { it.label },
        )
    }

    @Test
    fun rowClickAndRemoveRemainSeparateFromReorderHandle() {
        val events = mutableListOf<PlayerQueueAction>()
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Dark) {
                PlayerQueueSheetContent(
                    model = queue(),
                    onEvent = { action ->
                        events += action
                        PlayerQueueActionResult.Handled
                    },
                )
            }
        }

        composeRule.onNodeWithText("First").performClick()
        composeRule.onNodeWithContentDescription("从队列移除 First").performClick()

        assertEquals(listOf(PlayerQueueAction.Play("first"), PlayerQueueAction.Remove("first")), events)
    }

    @Test
    fun shuffleAndSingleItemsExposeAccurateDisabledReasons() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Dark) {
                PlayerQueueSheetContent(
                    model = queue(mode = PlayerPlaybackModeUi.Shuffle),
                    onEvent = { PlayerQueueActionResult.Handled },
                )
            }
        }
        composeRule.onNodeWithContentDescription("随机播放下不可排序：First").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("随机播放下不可排序：Second").assertIsDisplayed()

        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Dark) {
                PlayerQueueSheetContent(
                    model = queue(count = 1),
                    onEvent = { PlayerQueueActionResult.Handled },
                )
            }
        }
        composeRule.onNodeWithContentDescription("队列少于两首，无法排序：First").assertIsDisplayed()
    }

    @Test
    fun longPressDragHandleSubmitsOneStableIdMove() {
        val requests = mutableListOf<PlayerQueueMoveRequest>()
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Dark) {
                PlayerQueueSheetContent(
                    model = queue(),
                    onEvent = { action ->
                        requests += (action as PlayerQueueAction.MoveBefore).request
                        PlayerQueueActionResult.Move(PlayerQueueMoveResult.Stale)
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("拖动排序 First").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 1)
            moveBy(Offset(0f, 112f))
            up()
        }

        composeRule.waitUntil(timeoutMillis = 3_000) { requests.isNotEmpty() }
        assertEquals(PlayerQueueMoveRequest(draggedId = "first", beforeId = null), requests.single())
    }

    private fun queue(
        mode: PlayerPlaybackModeUi = PlayerPlaybackModeUi.RepeatAll,
        count: Int = 2,
    ) = PlayerQueueUiState(
        items =
            listOf(
                PlayerQueueItemUi("first", "First", "Artist"),
                PlayerQueueItemUi("second", "Second", "Artist"),
            ).take(count),
        currentItemId = "first",
        mode = mode,
    )
}
