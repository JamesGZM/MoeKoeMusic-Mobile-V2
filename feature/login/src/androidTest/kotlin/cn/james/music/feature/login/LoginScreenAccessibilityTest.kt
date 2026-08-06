package cn.james.music.feature.login

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenAccessibilityTest {
    @get:Rule val composeRule = createComposeRule()

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
