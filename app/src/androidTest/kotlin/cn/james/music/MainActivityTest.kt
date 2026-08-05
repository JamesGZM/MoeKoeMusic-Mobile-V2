package cn.james.music

import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showcaseDisplaysAndSwitchesTheme() {
        composeRule.onNodeWithTag("showcase-title").assertIsDisplayed()
        composeRule
            .onNodeWithTag("theme-Dark")
            .assertIsDisplayed()
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithTag("theme-Dark").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("primary-action").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("showcase-list").performScrollToIndex(3)
        composeRule.onNodeWithTag("playback-status").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onNodeWithTag("playback-status")
                .fetchSemanticsNode()
                .config
                .toString()
                .contains("已连接")
        }
        if (composeRule.onAllNodesWithTag("load-demo-queue").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("load-demo-queue").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("playback-toggle").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule
            .onNodeWithTag("playback-toggle")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("暂停").fetchSemanticsNodes().isNotEmpty()
        }
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationManager = targetContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        composeRule.waitUntil(timeoutMillis = 10_000) {
            notificationManager.activeNotifications.any { it.packageName == targetContext.packageName }
        }
        composeRule.onNodeWithTag("playback-toggle").performClick()
    }
}
