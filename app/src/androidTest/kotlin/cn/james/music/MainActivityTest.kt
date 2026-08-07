package cn.james.music

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        composeRule.onNodeWithText("搜索音乐、歌手、歌单…").performClick()
        composeRule.onNodeWithText("想听什么？").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("轻松发现下一首喜欢的音乐").assertIsDisplayed()
    }

    @Test
    fun topLevelTabsDoNotBuildAChronologicalBackStack() {
        composeRule.onNodeWithText("发现", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("排行榜、歌单和电台仍在规划阶段").assertIsDisplayed()
        composeRule.onNodeWithText("我的", useUnmergedTree = true).performClick()
        waitForMyContent()

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
        val rootBottom = composeRule.onRoot().getUnclippedBoundsInRoot().bottom
        val loginBottom = composeRule.onNodeWithTag("login_screen_root").getUnclippedBoundsInRoot().bottom
        assertEquals("登录沉浸式画布不能被应用壳 BottomBar 或导航栏 Padding 截短", rootBottom, loginBottom)
        val cardBottom = composeRule.onNodeWithTag("login_card").getUnclippedBoundsInRoot().bottom
        var navigationBarInsetDp = 0f
        composeRule.runOnUiThread {
            val insetPx =
                ViewCompat
                    .getRootWindowInsets(composeRule.activity.window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())
                    ?.bottom ?: 0
            navigationBarInsetDp = insetPx / composeRule.activity.resources.displayMetrics.density
        }
        assertTrue(
            "登录卡片必须位于底部系统导航安全区上方",
            rootBottom.value - cardBottom.value > navigationBarInsetDp,
        )

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
        composeRule.onNodeWithText("密码登录").assertIsDisplayed()
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
        composeRule.onNodeWithText("扫码登录").assertIsDisplayed()
        composeRule.onNodeWithTag("qr_login_instruction").assertIsDisplayed()
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
