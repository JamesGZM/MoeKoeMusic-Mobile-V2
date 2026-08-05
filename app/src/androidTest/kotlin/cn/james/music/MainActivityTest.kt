package cn.james.music

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun formalNavigationContainsOnlyThreeTopLevelDestinations() {
        composeRule.onAllNodesWithText("首页", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNodeWithText("发现", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("我的", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("本地音乐").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("导入文件").assertIsDisplayed()
        composeRule.onNodeWithText("扫描设备").assertIsDisplayed()
    }

    @Test
    fun debugPlaybackLabRemainsReachable() {
        composeRule.onNodeWithText("我的", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("打开播放工程实验台").performClick()
        composeRule.onNodeWithText("MoeKoe Music").assertIsDisplayed()
    }

    @Test
    fun searchUsesSystemBackStackInsteadOfReturningToAHardcodedPage() {
        composeRule.onNodeWithText("搜索音乐").performClick()
        composeRule.onNodeWithText("想听什么？").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("轻松发现下一首喜欢的音乐").assertIsDisplayed()
    }
}
