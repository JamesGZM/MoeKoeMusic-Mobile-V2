package cn.james.music

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioImportActivityTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun actionViewCopiesBeforeCompleting() {
        grantNotificationPermissionWhenRequired()
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setClassName("cn.james.music.debug", AudioImportActivity::class.java.name)
                setDataAndType(Uri.parse("content://cn.james.music.debug.test.audio/tone.wav"), "audio/wav")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        ActivityScenario.launch<AudioImportActivity>(intent).use { scenario ->
            composeRule.onNodeWithText("导入到 MoeKoe").assertIsDisplayed()
            android.os.SystemClock.sleep(5_000)
            scenario.onActivity { assertEquals("导入完成 · 1/1", it.message) }
            scenario.recreate()
            scenario.onActivity { assertEquals("导入完成 · 1/1", it.message) }
            val controller =
                EntryPointAccessors
                    .fromApplication(
                        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
                        PlaybackDebugEntryPoint::class.java,
                    ).playbackController()
            composeRule.waitUntil(5_000) { controller.state.value.currentItem != null }
            assertTrue("Playback state was ${controller.state.value}", controller.state.value.isPlaying)
            composeRule
                .onNodeWithText("进入 MoeKoe")
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .performClick()
            composeRule.waitUntilAtLeastOneExists(hasText("我的"), 5_000)
            composeRule.onNodeWithText("我的", useUnmergedTree = true).performClick()
            composeRule.onNodeWithText("本地音乐").performClick()
            composeRule.onNodeWithText("搜索歌曲或艺术家").assertIsDisplayed()
            composeRule.onNodeWithText("最近导入").assertIsDisplayed()
        }
    }

    @Test
    fun actionViewRejectsRemoteUri() {
        grantNotificationPermissionWhenRequired()
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setClassName("cn.james.music.debug", AudioImportActivity::class.java.name)
                setDataAndType(Uri.parse("https://example.invalid/audio.mp3"), "audio/mpeg")
            }

        ActivityScenario.launch<AudioImportActivity>(intent).use { scenario ->
            scenario.onActivity { assertEquals("无法读取这个音频来源", it.message) }
        }
    }

    @Test
    fun actionViewRejectsMissingUri() {
        grantNotificationPermissionWhenRequired()
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setClassName("cn.james.music.debug", AudioImportActivity::class.java.name)
                type = "audio/mpeg"
            }

        ActivityScenario.launch<AudioImportActivity>(intent).use { scenario ->
            scenario.onActivity { assertEquals("无法读取这个音频来源", it.message) }
        }
    }

    @Test
    fun actionViewRejectsNonAudioMimeType() {
        grantNotificationPermissionWhenRequired()
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setClassName("cn.james.music.debug", AudioImportActivity::class.java.name)
                setDataAndType(Uri.parse("content://cn.james.music.debug.test.audio/tone.wav"), "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        ActivityScenario.launch<AudioImportActivity>(intent).use { scenario ->
            scenario.onActivity { assertEquals("无法读取这个音频来源", it.message) }
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun actionViewWhileImportActivityIsTopReusesActivityAndHandlesNewIntent() {
        grantNotificationPermissionWhenRequired()
        val firstIntent =
            Intent(Intent.ACTION_VIEW).apply {
                setClassName("cn.james.music.debug", AudioImportActivity::class.java.name)
                type = "audio/mpeg"
            }
        val secondUri = Uri.parse("content://cn.james.music.debug.test.audio/tone.mp3")

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity =
            instrumentation.startActivitySync(
                firstIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            ) as AudioImportActivity
        try {
            val originalInstance = System.identityHashCode(activity)
            instrumentation.runOnMainSync {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setClass(activity, AudioImportActivity::class.java)
                        setDataAndType(secondUri, "audio/mpeg")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                )
            }

            composeRule.waitUntil(10_000) {
                var completed = false
                instrumentation.runOnMainSync {
                    completed = activity.intent.data == secondUri && activity.message == "导入完成 · 1/1"
                }
                completed
            }
            instrumentation.runOnMainSync {
                assertEquals(originalInstance, System.identityHashCode(activity))
                assertEquals(secondUri, activity.intent.data)
                assertEquals("导入完成 · 1/1", activity.message)
            }
        } finally {
            instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
        }
    }

    private fun grantNotificationPermissionWhenRequired() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .grantRuntimePermission("cn.james.music.debug", Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
