package cn.james.music

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

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
        waitForMyContent()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("打开播放工程实验台"))
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

    @Test
    fun loginIsAnIndependentPageReachedFromMy() {
        composeRule.onNodeWithText("我的", useUnmergedTree = true).performClick()
        waitForAnonymousMyState()
        composeRule.onNodeWithText("登录").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("手机号登录").assertIsDisplayed()
        composeRule.onNodeWithText("登录并继续").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        waitForAnonymousMyState()
        composeRule.onNodeWithText("登录 MoeKoe Air").assertIsDisplayed()
    }

    @Test
    fun passwordLoginIsReachableWithoutSubmittingCredentials() {
        composeRule.onNodeWithText("我的", useUnmergedTree = true).performClick()
        waitForAnonymousMyState()
        composeRule.onNodeWithText("登录").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("密码", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("请输入账号").assertIsDisplayed()
        composeRule.onNodeWithText("请输入密码").assertIsDisplayed()
        composeRule.onNodeWithText("登录并继续").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        waitForAnonymousMyState()
        composeRule.onNodeWithText("登录 MoeKoe Air").assertIsDisplayed()
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun qrLoginIsReachableAndLeavingStopsShowingQrState() {
        composeRule.onNodeWithText("我的", useUnmergedTree = true).performClick()
        waitForAnonymousMyState()
        composeRule.onNodeWithText("登录").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("扫码", useUnmergedTree = true).performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithContentDescription("酷狗音乐登录二维码").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("请使用酷狗音乐扫码").assertIsDisplayed()
        saveDeviceScreenshot()

        composeRule.onNodeWithText("验证码", useUnmergedTree = true).performClick()
        Thread.sleep(2_500)
        composeRule.onNodeWithText("手机号登录").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("酷狗音乐登录二维码").assertCountEquals(0)
    }

    private fun waitForAnonymousMyState() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithText("登录 MoeKoe Air")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun waitForMyContent() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithText("本地音乐")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun saveDeviceScreenshot() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            val root = composeRule.activity.window.decorView.rootView
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            File(requireNotNull(composeRule.activity.getExternalFilesDir(null)), QR_DEVICE_SCREENSHOT).outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        }
    }

    private companion object {
        const val QR_DEVICE_SCREENSHOT = "qr-device.png"
    }
}
