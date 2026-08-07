package cn.james.music.feature.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenAccessibilityTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun mobileCodeDesignRatioViewportHasNoScrollAction() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                Box(Modifier.size(width = 320.dp, height = 694.dp)) { TestLoginScreen(LoginUiState()) }
            }
        }

        composeRule.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }

    @Test
    fun mobileCodeShortViewportExposesScrollAction() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                Box(Modifier.size(width = 320.dp, height = 620.dp)) { TestLoginScreen(LoginUiState()) }
            }
        }

        composeRule.onAllNodes(hasScrollAction()).assertCountEquals(1)
    }

    @Test
    fun navigationBarInsetKeepsCardAboveTheUnsafeArea() {
        val safeInset = 56.dp
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                Box(
                    Modifier
                        .size(width = 320.dp, height = 694.dp)
                        .testTag("login_safe_area_test_canvas"),
                ) {
                    TestLoginScreen(LoginUiState(), bottomSafeInsetOverride = safeInset)
                }
            }
        }

        val rootBottom =
            composeRule
                .onNodeWithTag("login_safe_area_test_canvas")
                .getUnclippedBoundsInRoot()
                .bottom
        val cardBottom = composeRule.onNodeWithTag(LOGIN_CARD_TAG).getUnclippedBoundsInRoot().bottom
        assertTrue(rootBottom - cardBottom > safeInset)
    }

    @Test
    fun backVisualAlignsWithCardAndKeepsMinimumTouchTarget() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                Box(Modifier.size(width = 320.dp, height = 694.dp)) { TestLoginScreen(LoginUiState()) }
            }
        }

        val cardLeft = composeRule.onNodeWithTag(LOGIN_CARD_TAG).getUnclippedBoundsInRoot().left
        val backVisualBounds = composeRule.onNodeWithTag(LOGIN_BACK_VISUAL_TAG).getUnclippedBoundsInRoot()
        val backTouchBounds = composeRule.onNodeWithContentDescription("返回").getUnclippedBoundsInRoot()

        assertEquals(cardLeft.value, backVisualBounds.left.value, 0.5f)
        assertTrue(backTouchBounds.right - backTouchBounds.left >= 48.dp)
        assertTrue(backTouchBounds.bottom - backTouchBounds.top >= 48.dp)
    }

    @Test
    fun heroImageContinuesBehindTheFloatingCard() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                Box(Modifier.size(width = 390.dp, height = 845.dp)) { TestLoginScreen(LoginUiState()) }
            }
        }

        val heroBottom = composeRule.onNodeWithTag(LOGIN_HERO_IMAGE_TAG).getUnclippedBoundsInRoot().bottom
        val cardTop = composeRule.onNodeWithTag(LOGIN_CARD_TAG).getUnclippedBoundsInRoot().top
        assertTrue(heroBottom > cardTop)
    }

    @Test
    fun mobileCodeActionsStayDisabledUntilInputsAreValid() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                LoginScreen(
                    state = LoginUiState(),
                    onBack = {},
                    onPhoneChange = {},
                    onCodeChange = {},
                    onModeChange = {},
                    onRefreshQrLogin = {},
                    onSendCode = {},
                    onSubmitMobileCode = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmitPassword = {},
                    onStartRiskVerification = {},
                    onRetryTencentVerification = {},
                    onRiskCodeChange = {},
                    onVerifyRiskCode = {},
                    onCancelRisk = {},
                    onSelectAccount = {},
                    onChooseOtherAccount = {},
                )
            }
        }

        composeRule.onNodeWithText("获取验证码").assertIsNotEnabled()
        composeRule.onNodeWithText("登录并继续").assertIsNotEnabled()
    }

    @Test
    fun passwordFeedbackSlotKeepsPrimaryActionAnchored() {
        var state by
            mutableStateOf(
                LoginUiState(
                    mode = LoginMode.Password,
                    username = "miyu.song@moekoe.com",
                    password = "fixture-password",
                ),
            )
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                LoginScreen(
                    state = state,
                    onBack = {},
                    onPhoneChange = {},
                    onCodeChange = {},
                    onModeChange = {},
                    onRefreshQrLogin = {},
                    onSendCode = {},
                    onSubmitMobileCode = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmitPassword = {},
                    onStartRiskVerification = {},
                    onRetryTencentVerification = {},
                    onRiskCodeChange = {},
                    onVerifyRiskCode = {},
                    onCancelRisk = {},
                    onSelectAccount = {},
                    onChooseOtherAccount = {},
                )
            }
        }

        val defaultTop =
            composeRule
                .onNodeWithText("登录并继续")
                .getUnclippedBoundsInRoot()
                .top.value
        composeRule.runOnIdle { state = state.copy(notice = LoginNotice.PasswordRejected) }
        composeRule.onNodeWithText("账号或密码不正确，请重新输入").assertIsDisplayed()
        val rejectedTop =
            composeRule
                .onNodeWithText("重新登录")
                .getUnclippedBoundsInRoot()
                .top.value

        assertEquals(defaultTop.toDouble(), rejectedTop.toDouble(), 0.5)
    }

    @Test
    fun mobileFeedbackStatesKeepPrimaryActionAnchored() {
        var state by
            mutableStateOf(
                LoginUiState(
                    phone = "13800138000",
                    code = "123456",
                    notice = LoginNotice.CodeSent,
                ),
            )
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) { TestLoginScreen(state) }
        }

        composeRule.onNodeWithText("验证码已发送，请注意查收").assertIsDisplayed()
        val successTop = composeRule.onNodeWithText("登录并继续").getUnclippedBoundsInRoot().top.value

        composeRule.runOnIdle {
            state = state.copy(notice = LoginNotice.Validation(LoginValidationError.InvalidCode))
        }
        composeRule.onNodeWithText("请输入收到的短信验证码").assertIsDisplayed()
        val errorTop = composeRule.onNodeWithText("登录并继续").getUnclippedBoundsInRoot().top.value

        assertEquals(successTop.toDouble(), errorTop.toDouble(), 0.5)
    }

    @Test
    fun mobileCodeCountdownIsVisibleWithoutChangingScreenMode() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                LoginScreen(
                    state =
                        LoginUiState(
                            phone = "13800138000",
                            countdownSeconds = 48,
                            notice = LoginNotice.CodeSent,
                        ),
                    onBack = {},
                    onPhoneChange = {},
                    onCodeChange = {},
                    onModeChange = {},
                    onRefreshQrLogin = {},
                    onSendCode = {},
                    onSubmitMobileCode = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmitPassword = {},
                    onStartRiskVerification = {},
                    onRetryTencentVerification = {},
                    onRiskCodeChange = {},
                    onVerifyRiskCode = {},
                    onCancelRisk = {},
                    onSelectAccount = {},
                    onChooseOtherAccount = {},
                )
            }
        }

        composeRule.onNodeWithText("48 秒后重发").assertIsDisplayed()
        composeRule.onNodeWithText("验证码").assertIsSelected()
    }

    @Test
    fun expiredAccountVerificationCanRecoverAtLargestFontScale() {
        var recoverClicks = 0
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale = 2f),
            ) {
                MoeKoeTheme(themeMode = ThemeMode.Light) {
                    LoginScreen(
                        state = expiredAccountState,
                        onBack = {},
                        onPhoneChange = {},
                        onCodeChange = {},
                        onModeChange = {},
                        onRefreshQrLogin = {},
                        onSendCode = {},
                        onSubmitMobileCode = {},
                        onUsernameChange = {},
                        onPasswordChange = {},
                        onTogglePasswordVisibility = {},
                        onSubmitPassword = {},
                        onStartRiskVerification = {},
                        onRetryTencentVerification = {},
                        onRiskCodeChange = {},
                        onVerifyRiskCode = {},
                        onCancelRisk = {},
                        onSelectAccount = {},
                        onChooseOtherAccount = { recoverClicks += 1 },
                    )
                }
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("重新验证手机号"))
        composeRule.onNodeWithText("重新验证手机号").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, recoverClicks) }
    }

    @Test
    fun qrGeneratingUsesIndeterminateProgress() {
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                LoginScreen(
                    state = LoginUiState(mode = LoginMode.QrCode, qrLogin = QrLoginUiState.Generating),
                    onBack = {},
                    onPhoneChange = {},
                    onCodeChange = {},
                    onModeChange = {},
                    onRefreshQrLogin = {},
                    onSendCode = {},
                    onSubmitMobileCode = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmitPassword = {},
                    onStartRiskVerification = {},
                    onRetryTencentVerification = {},
                    onRiskCodeChange = {},
                    onVerifyRiskCode = {},
                    onCancelRisk = {},
                    onSelectAccount = {},
                    onChooseOtherAccount = {},
                )
            }
        }

        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo.Indeterminate,
                ),
            ).assertIsDisplayed()
    }

    @Test
    fun loginModesExposeTabSelectionAndSwitchOnce() {
        composeRule.mainClock.autoAdvance = false
        var selectedMode by mutableStateOf(LoginMode.MobileCode)
        var requestedMode: LoginMode? = null
        var modeChanges = 0
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                LoginScreen(
                    state = LoginUiState(mode = selectedMode),
                    onBack = {},
                    onPhoneChange = {},
                    onCodeChange = {},
                    onModeChange = {
                        requestedMode = it
                        selectedMode = it
                        modeChanges += 1
                    },
                    onRefreshQrLogin = {},
                    onSendCode = {},
                    onSubmitMobileCode = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmitPassword = {},
                    onStartRiskVerification = {},
                    onRetryTencentVerification = {},
                    onRiskCodeChange = {},
                    onVerifyRiskCode = {},
                    onCancelRisk = {},
                    onSelectAccount = {},
                    onChooseOtherAccount = {},
                )
            }
        }

        val tabRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        val initialIndicatorLeft =
            composeRule.onNodeWithTag(LOGIN_MODE_INDICATOR_TAG).getUnclippedBoundsInRoot().left
        composeRule.onNodeWithText("验证码").assert(tabRole).assertIsSelected()
        composeRule
            .onNodeWithText("密码")
            .assert(tabRole)
            .assertIsNotSelected()
            .performClick()
        composeRule.mainClock.advanceTimeBy(130L)
        val movingIndicatorLeft =
            composeRule.onNodeWithTag(LOGIN_MODE_INDICATOR_TAG).getUnclippedBoundsInRoot().left
        composeRule.mainClock.advanceTimeBy(200L)
        val finalIndicatorLeft =
            composeRule.onNodeWithTag(LOGIN_MODE_INDICATOR_TAG).getUnclippedBoundsInRoot().left
        composeRule.onNodeWithText("密码").assert(tabRole).assertIsSelected()
        composeRule.onNodeWithText("扫码").assert(tabRole).assertIsNotSelected()
        composeRule.runOnIdle {
            assertEquals(LoginMode.Password, requestedMode)
            assertEquals(1, modeChanges)
            assertTrue(movingIndicatorLeft > initialIndicatorLeft)
            assertTrue(movingIndicatorLeft < finalIndicatorLeft)
        }
    }

    @Test
    fun accountsExposeOneRadioSelectionActionPerRow() {
        var selectedUserId: String? = null
        var selectionChanges = 0
        composeRule.setContent {
            MoeKoeTheme(themeMode = ThemeMode.Light) {
                LoginScreen(
                    state = expiredAccountState.copy(notice = null),
                    onBack = {},
                    onPhoneChange = {},
                    onCodeChange = {},
                    onModeChange = {},
                    onRefreshQrLogin = {},
                    onSendCode = {},
                    onSubmitMobileCode = {},
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onSubmitPassword = {},
                    onStartRiskVerification = {},
                    onRetryTencentVerification = {},
                    onRiskCodeChange = {},
                    onVerifyRiskCode = {},
                    onCancelRisk = {},
                    onSelectAccount = {
                        selectedUserId = it
                        selectionChanges += 1
                    },
                    onChooseOtherAccount = {},
                )
            }
        }

        val radioRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        composeRule.onNodeWithText("MoeKoe").assert(radioRole).assertIsSelected()
        composeRule
            .onNodeWithText("夏日旋律")
            .assert(radioRole)
            .assertIsNotSelected()
            .performClick()
        composeRule.runOnIdle {
            assertEquals("10000001", selectedUserId)
            assertEquals(1, selectionChanges)
        }
    }
}

private val expiredAccountState =
    LoginUiState(
        phone = "13800138000",
        code = "123456",
        accounts =
            listOf(
                AuthAccountOption("10000001", "夏日旋律", null, "VIP"),
                AuthAccountOption("10000002", "MoeKoe", null, null),
                AuthAccountOption("10000003", "蓝色留声机", null, null),
            ),
        selectedUserId = "10000002",
        notice = LoginNotice.Failure(AuthError.Rejected),
    )

@Composable
private fun TestLoginScreen(
    state: LoginUiState,
    bottomSafeInsetOverride: Dp? = null,
) {
    LoginScreen(
        state = state,
        bottomSafeInsetOverride = bottomSafeInsetOverride,
        onBack = {},
        onPhoneChange = {},
        onCodeChange = {},
        onModeChange = {},
        onRefreshQrLogin = {},
        onSendCode = {},
        onSubmitMobileCode = {},
        onUsernameChange = {},
        onPasswordChange = {},
        onTogglePasswordVisibility = {},
        onSubmitPassword = {},
        onStartRiskVerification = {},
        onRetryTencentVerification = {},
        onRiskCodeChange = {},
        onVerifyRiskCode = {},
        onCancelRisk = {},
        onSelectAccount = {},
        onChooseOtherAccount = {},
    )
}
