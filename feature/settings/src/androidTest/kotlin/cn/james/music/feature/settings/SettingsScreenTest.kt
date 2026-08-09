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
    fun dynamicCoverColorsForwardsItsOwnToggleAction() {
        var enabled: Boolean? = null
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                groups =
                                    settingsGroups(
                                        theme = SettingsThemeUi.System,
                                        autoSkipFailedPlayback = true,
                                        dynamicCoverColors = true,
                                    ),
                            ),
                        onAction = { action ->
                            if (action is SettingsAction.SetDynamicCoverColors) enabled = action.enabled
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText("封面动态取色").performClick()

        assertEquals(false, enabled)
    }

    @Test
    fun lyricsSupplementalTextForwardsItsOwnToggleAction() {
        var enabled: Boolean? = null
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(),
                        onAction = { action ->
                            if (action is SettingsAction.SetShowLyricsSupplementalText) enabled = action.enabled
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText("翻译与音译").performClick()

        assertEquals(false, enabled)
    }

    @Test
    fun savingDynamicCoverColorsDisablesItsRow() {
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                dynamicCoverColors = true,
                                savingDynamicCoverColors = true,
                                groups =
                                    settingsGroups(
                                        theme = SettingsThemeUi.System,
                                        autoSkipFailedPlayback = true,
                                        dynamicCoverColors = true,
                                        savingDynamicCoverColors = true,
                                    ),
                            ),
                        onAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("封面动态取色").assertIsNotEnabled()
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

    @Test
    fun savingLyricsSupplementalTextDisablesItsRow() {
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                showLyricsSupplementalText = true,
                                savingLyricsSupplementalText = false,
                                groups =
                                    settingsGroups(
                                        theme = SettingsThemeUi.System,
                                        autoSkipFailedPlayback = true,
                                        showLyricsSupplementalText = true,
                                        savingLyricsSupplementalText = false,
                                    ),
                            ),
                        onAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("翻译与音译").assertIsNotEnabled()
    }

    @Test
    fun lyricsTextSizeDialogUsesRadioSelectionAndDisablesWhileSaving() {
        var selected: SettingsLyricsTextSizeUi? = null
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                lyricsTextSize = SettingsLyricsTextSizeUi.Standard,
                                overlay = SettingsOverlay.LyricsTextSizeSelection,
                            ),
                        onAction = { action ->
                            if (action is SettingsAction.SelectLyricsTextSize) selected = action.size
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithText("150%").performClick()
        assertEquals(SettingsLyricsTextSizeUi.Large, selected)

        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                lyricsTextSize = SettingsLyricsTextSizeUi.Large,
                                savingLyricsTextSize = SettingsLyricsTextSizeUi.Large,
                                overlay = SettingsOverlay.LyricsTextSizeSelection,
                            ),
                        onAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("150%").assertIsNotEnabled()
    }

    @Test
    fun playbackQualityDialogUsesSevenRadioOptionsAndDisablesWhileSaving() {
        var selected: SettingsPlaybackQualityUi? = null
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state = SettingsUiState(overlay = SettingsOverlay.PlaybackQualitySelection),
                        onAction = { action ->
                            if (action is SettingsAction.SelectPlaybackQuality) selected = action.quality
                        },
                    )
                }
            }
        }

        listOf(
            "标准音质 · 128 Kbps",
            "高品音质 · 320 Kbps",
            "FLAC 无损",
            "Hi-Res 无损",
            "蝰蛇全景",
            "蝰蛇超清",
            "蝰蛇母带",
        ).forEach { label -> composeRule.onNodeWithText(label).assertIsDisplayed() }
        composeRule.onNodeWithText("Hi-Res 无损").performClick()
        assertEquals(SettingsPlaybackQualityUi.HiRes, selected)

        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                playbackQuality = SettingsPlaybackQualityUi.HiRes,
                                savingPlaybackQuality = SettingsPlaybackQualityUi.HiRes,
                                overlay = SettingsOverlay.PlaybackQualitySelection,
                                groups =
                                    settingsGroups(
                                        theme = SettingsThemeUi.System,
                                        autoSkipFailedPlayback = true,
                                        playbackQuality = SettingsPlaybackQualityUi.HiRes,
                                        savingPlaybackQuality = SettingsPlaybackQualityUi.HiRes,
                                    ),
                            ),
                        onAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Hi-Res 无损").assertIsNotEnabled()
    }

    @Test
    fun savingPlaybackQualityDisablesOnlyItsRow() {
        composeRule.setContent {
            MoeKoeTheme {
                Surface {
                    SettingsScreen(
                        state =
                            SettingsUiState(
                                savingPlaybackQuality = SettingsPlaybackQualityUi.Standard,
                                groups =
                                    settingsGroups(
                                        theme = SettingsThemeUi.System,
                                        autoSkipFailedPlayback = true,
                                        savingPlaybackQuality = SettingsPlaybackQualityUi.Standard,
                                    ),
                            ),
                        onAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("默认音质").assertIsNotEnabled()
        composeRule.onNodeWithText("主题模式").assertIsEnabled()
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
