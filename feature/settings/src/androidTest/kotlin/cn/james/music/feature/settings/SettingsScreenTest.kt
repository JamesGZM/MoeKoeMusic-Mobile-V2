package cn.james.music.feature.settings

import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.james.music.core.designsystem.MoeKoeTheme
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
                        onAction = {
                            if (it == SettingsAction.Back) backRequested = true
                            if (it == SettingsAction.OpenTheme) themeDialogRequested = true
                        },
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
        var selected: SettingsThemeUi? = null
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(overlay = SettingsOverlay.ThemeSelection),
                        onAction = { if (it is SettingsAction.SelectTheme) selected = it.theme },
                    )
                }
            }
        }

        composeRule.onNodeWithText("选择主题模式").assertIsDisplayed()
        composeRule.onNodeWithText("浅色").performClick()

        assertEquals(SettingsThemeUi.Light, selected)
    }

    @Test
    fun amoledPreferenceRendersEnabledCheckedSwitch() {
        setSettingsContent(SettingsThemeUi.Amoled)

        composeRule
            .onNode(toggleableState(ToggleableState.On), useUnmergedTree = true)
            .assertIsEnabled()
            .assertIsOn()
    }

    @Test
    fun systemPreferenceRendersEnabledUncheckedSwitch() {
        setSettingsContent(SettingsThemeUi.System)

        composeRule
            .onNode(toggleableState(ToggleableState.Off), useUnmergedTree = true)
            .assertIsEnabled()
            .assertIsOff()
    }

    @Test
    fun autoSkipFailedPlaybackForwardsItsOwnToggleAction() {
        var enabled: Boolean? = null
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(),
                        onAction = { action ->
                            if (action is SettingsAction.SetAutoSkipFailedPlayback) enabled = action.enabled
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText("播放失败时自动跳过").performClick()

        assertEquals(false, enabled)
    }

    @Test
    fun savingAutoSkipFailedPlaybackDisablesItsRow() {
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                autoSkipFailedPlayback = true,
                                savingAutoSkipFailedPlayback = false,
                                groups =
                                    settingsGroups(
                                        theme = SettingsThemeUi.System,
                                        autoSkipFailedPlayback = true,
                                        savingAutoSkipFailedPlayback = false,
                                    ),
                            ),
                        onAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("播放失败时自动跳过").assertIsNotEnabled()
    }

    private fun setSettingsContent(theme: SettingsThemeUi) {
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(theme = theme, groups = settingsGroups(theme, autoSkipFailedPlayback = true)),
                        onAction = {},
                    )
                }
            }
        }
    }

    private fun toggleableState(state: ToggleableState): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)
}
