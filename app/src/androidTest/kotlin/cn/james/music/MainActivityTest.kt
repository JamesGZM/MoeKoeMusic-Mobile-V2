package cn.james.music

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    }
}
