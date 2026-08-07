package cn.james.music.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.navigation.MoeNavigateBackIcon

internal const val LOGIN_CARD_TAG = "login_card"
internal const val LOGIN_HERO_IMAGE_TAG = "login_hero_image"
internal const val LOGIN_BACK_VISUAL_TAG = "login_back_visual"

@Composable
internal fun LoginPageLayout(
    layout: LoginLayoutSpec,
    state: LoginUiState,
    onBack: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onModeChange: (LoginMode) -> Unit,
    onRefreshQrLogin: () -> Unit,
    onSendCode: () -> Unit,
    onSubmitMobileCode: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmitPassword: () -> Unit,
    onStartRiskVerification: () -> Unit,
    onRetryTencentVerification: () -> Unit,
    onRiskCodeChange: (String) -> Unit,
    onVerifyRiskCode: () -> Unit,
    onCancelRisk: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onChooseOtherAccount: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        val cardTop = layout.page.cardTop
        val cardBottomSpacing = layout.page.cardBottomSpacing
        val minimumCardHeight =
            (layout.page.canvasHeight - cardTop - cardBottomSpacing).coerceAtLeast(0.dp)
        val scrollState = rememberScrollState()
        val scrollEnabled by remember { derivedStateOf { scrollState.maxValue > 0 } }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState, enabled = scrollEnabled)
                    .testTag(LOGIN_SCROLL_CONTAINER_TAG),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = layout.page.canvasHeight),
            ) {
                LoginHero(layout = layout)
                val cardShape = RoundedCornerShape(layout.page.cardCornerRadius)
                Surface(
                    modifier =
                        Modifier
                            .padding(
                                start = layout.page.cardStart,
                                top = cardTop,
                                end = layout.page.cardStart,
                                bottom = cardBottomSpacing,
                            )
                            .fillMaxWidth()
                            .heightIn(min = minimumCardHeight)
                            .loginCardGlow(layout, MaterialTheme.colorScheme.primary),
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(layout.dp(1f), MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    shadowElevation = layout.dp(0f),
                ) {
                    Box(Modifier.fillMaxSize().testTag(LOGIN_CARD_TAG)) {
                        if (state.hasMultipleAccounts) {
                            AccountSelectionContent(
                                layout = layout,
                                state = state,
                                onSelectAccount = onSelectAccount,
                                onSubmit = onSubmitMobileCode,
                                onChooseOtherAccount = onChooseOtherAccount,
                            )
                        } else {
                            when (state.mode) {
                                LoginMode.MobileCode -> {
                                    LoginMobileCodeContent(
                                        layout = layout,
                                        state = state,
                                        onPhoneChange = onPhoneChange,
                                        onCodeChange = onCodeChange,
                                        onSendCode = onSendCode,
                                        onSubmit = onSubmitMobileCode,
                                    )
                                }

                                LoginMode.Password -> {
                                    PasswordContent(
                                        layout = layout,
                                        state = state,
                                        onUsernameChange = onUsernameChange,
                                        onPasswordChange = onPasswordChange,
                                        onTogglePasswordVisibility = onTogglePasswordVisibility,
                                        onSubmit = onSubmitPassword,
                                    )
                                }

                                LoginMode.QrCode -> {
                                    QrCodeContent(
                                        layout = layout,
                                        state = state,
                                        onModeChange = onModeChange,
                                        onRefresh = onRefreshQrLogin,
                                    )
                                }
                            }
                            LoginModeSelector(
                                layout = layout,
                                selectedMode = state.mode,
                                onModeChange = onModeChange,
                                enabled = !state.isBusy && !state.hasActiveRisk,
                                modifier =
                                    Modifier
                                        .padding(
                                            start = layout.page.contentStart,
                                            top = layout.page.contentTop,
                                            end = layout.page.contentEnd,
                                        )
                                        .fillMaxWidth(),
                            )
                        }
                    }
                }
                LoginBackButton(layout = layout, onClick = onBack)
            }
        }
        LoginRiskDialog(
            state = state,
            onStartVerification = onStartRiskVerification,
            onRetryTencentVerification = onRetryTencentVerification,
            onRiskCodeChange = onRiskCodeChange,
            onVerifyRiskCode = onVerifyRiskCode,
            onCancel = onCancelRisk,
        )
    }
}

private fun Modifier.loginCardGlow(
    layout: LoginLayoutSpec,
    color: Color,
): Modifier =
    drawBehind {
        val maximumSpread = layout.page.cardShadowSpread.toPx()
        val spreadStep = layout.dp(1f).toPx().coerceAtLeast(1f)
        val baseRadius = layout.page.cardCornerRadius.toPx()
        var spread = maximumSpread
        while (spread >= spreadStep) {
            val proximity = 1f - spread / maximumSpread
            val layerAlpha = 0.0025f + proximity * 0.006f
            drawRoundRect(
                color = color.copy(alpha = layerAlpha),
                topLeft = Offset(-spread, -spread),
                size = Size(size.width + spread * 2, size.height + spread * 2),
                cornerRadius = CornerRadius(baseRadius + spread),
            )
            spread -= spreadStep
        }
    }

@Composable
private fun LoginHero(
    layout: LoginLayoutSpec,
) {
    val heroOverlayAlpha = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) 0.86f else 0.28f
    Box(
        Modifier
            .fillMaxWidth()
            .height(layout.page.heroHeight)
            .background(
                Brush.verticalGradient(
                    0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                    0.86f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    1f to MaterialTheme.colorScheme.surface,
                ),
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.login_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(layout.page.heroHeight)
                    .testTag(LOGIN_HERO_IMAGE_TAG),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to MaterialTheme.colorScheme.surface.copy(alpha = heroOverlayAlpha),
                            0.58f to MaterialTheme.colorScheme.surface.copy(alpha = 0.02f),
                            1f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        ),
                    ),
        )
        LoginHeroBottomFade()
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = layout.page.heroCopyStart,
                        end = layout.page.heroCopyStart,
                        bottom = layout.page.heroCopyBottom,
                    ),
        ) {
            Text(
                text = stringResource(R.string.login_brand),
                style =
                    TextStyle(
                        fontSize = layout.typography.brandSize,
                        lineHeight = layout.typography.brandLineHeight,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.login_welcome),
                modifier = Modifier.padding(top = layout.page.heroCopySpacing),
                style =
                    TextStyle(
                        fontSize = layout.typography.welcomeSize,
                        lineHeight = layout.typography.welcomeLineHeight,
                        fontWeight = FontWeight.Normal,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoginHeroBottomFade() {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        0.84f to surface.copy(alpha = 0.14f),
                        0.92f to surface.copy(alpha = 0.62f),
                        1f to surface.copy(alpha = 0.94f),
                    ),
                ),
    )
}

@Composable
private fun LoginBackButton(
    layout: LoginLayoutSpec,
    onClick: () -> Unit,
) {
    val touchStart = layout.page.backCenterX - 24.dp
    val touchTop = layout.page.backCenterY - 24.dp
    val description = stringResource(R.string.login_back)
    Box(
        modifier =
            Modifier
                .offset(x = touchStart, y = touchTop)
                .size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .size(layout.page.backVisualSize)
                    .testTag(LOGIN_BACK_VISUAL_TAG)
                    .shadow(2.dp, RoundedCornerShape(50), ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.10f)),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)),
        ) {}
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp).semantics { contentDescription = description },
        ) {
            MoeNavigateBackIcon(
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(layout.page.backIconWidth, layout.page.backIconHeight),
            )
        }
    }
}
