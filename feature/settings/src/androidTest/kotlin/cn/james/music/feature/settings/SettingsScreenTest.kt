package cn.james.music.feature.settings

import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.model.settings.AppThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screenForwardsBackAndThemeDialogRequest() {
        var backRequested = false
        var themeDialogRequested = false
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(),
                        onBack = { backRequested = true },
                        onThemeSelected = {},
                        onThemeDialogRequest = { themeDialogRequested = true },
                        onAboutDialogRequest = {},
                        onDismissOverlay = {},
                        onRetry = {},
                        onDismissProblem = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithText("主题模式").performClick()

        assertTrue(backRequested)
        assertTrue(themeDialogRequested)
    }

    @Test
    fun themeDialogIsRenderedOnlyFromStateAndForwardsSelection() {
        var selected: AppThemePreference? = null
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(overlay = SettingsOverlay.ThemeSelection),
                        onBack = {},
                        onThemeSelected = { selected = it },
                        onThemeDialogRequest = {},
                        onAboutDialogRequest = {},
                        onDismissOverlay = {},
                        onRetry = {},
                        onDismissProblem = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("选择主题模式").assertIsDisplayed()
        composeRule.onNodeWithText("浅色").performClick()

        assertEquals(AppThemePreference.Light, selected)
    }

    @Test
    fun amoledPreferenceRendersEnabledCheckedSwitch() {
        setSettingsContent(AppThemePreference.Amoled)

        composeRule
            .onNode(toggleableState(ToggleableState.On), useUnmergedTree = true)
            .assertIsEnabled()
            .assertIsOn()
    }

    @Test
    fun systemPreferenceRendersEnabledUncheckedSwitch() {
        setSettingsContent(AppThemePreference.System)

        composeRule
            .onNode(toggleableState(ToggleableState.Off), useUnmergedTree = true)
            .assertIsEnabled()
            .assertIsOff()
    }

    private fun setSettingsContent(theme: AppThemePreference) {
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(theme = theme),
                        onBack = {},
                        onThemeSelected = {},
                        onThemeDialogRequest = {},
                        onAboutDialogRequest = {},
                        onDismissOverlay = {},
                        onRetry = {},
                        onDismissProblem = {},
                    )
                }
            }
        }
    }

    private fun toggleableState(state: ToggleableState): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)
}
