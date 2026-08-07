package cn.james.music.feature.login

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Explicit mapping from the accepted 852 × 1846 design canvas to this window. */
@Immutable
internal class LoginLayoutSpec private constructor(
    val canvasWidth: Dp,
    val viewportHeight: Dp,
    val bottomSafeInset: Dp,
    val scale: Float,
) {
    val page = LoginPageMetrics(this)
    val form = LoginFormMetrics(this)
    val qr = LoginQrMetrics(this)
    val typography = LoginTypographyMetrics(this)

    fun dp(designUnits: Float): Dp = (designUnits * scale).dp

    fun sp(designUnits: Float): TextUnit = (designUnits * scale).sp

    companion object {
        fun create(
            availableWidth: Dp,
            viewportHeight: Dp,
            bottomSafeInset: Dp,
        ): LoginLayoutSpec {
            val canvasWidth = availableWidth.coerceAtMost(LoginDesignUnits.MAX_CANVAS_WIDTH)
            return LoginLayoutSpec(
                canvasWidth = canvasWidth,
                viewportHeight = viewportHeight,
                bottomSafeInset = bottomSafeInset,
                scale = canvasWidth.value / LoginDesignUnits.CANVAS_WIDTH,
            )
        }
    }
}

@Immutable
internal class LoginQrMetrics(
    layout: LoginLayoutSpec,
) {
    val titleToInstruction = layout.dp(10f)
    val instructionToState = layout.dp(32f)
    val standardStateHeight = layout.dp(438f)
    val expiredStateHeight = layout.dp(576f)
    val failureStateHeight = layout.dp(470f)
    val standardStateToSteps = layout.dp(40f)
    val terminalStateToSteps = layout.dp(30f)

    val codeFrameSize = layout.dp(376f)
    val codeImageSize = layout.dp(340f)
    val codeFrameRadius = layout.dp(16f)
    val codeBadgeSize = layout.dp(76f)
    val codeBadgeRadius = layout.dp(18f)
    val codeBadgeIconSize = layout.dp(44f)
    val codeToExpiry = layout.dp(20f)

    val progressSize = layout.dp(144f)
    val progressStroke = layout.dp(8f)
    val progressToLabel = layout.dp(30f)
    val scannedIconSize = layout.dp(116f)
    val scannedIconGlyphSize = layout.dp(64f)
    val scannedTextGap = layout.dp(18f)
    val terminalIconContainerSize = layout.dp(160f)
    val terminalIconSize = layout.dp(72f)
    val terminalTextGap = layout.dp(18f)
    val terminalButtonWidth = layout.dp(208f)
    val terminalButtonHeight = layout.dp(66f)

    val stepRowHeight = layout.dp(126f)
    val stepIconContainerSize = layout.dp(68f)
    val stepIconContainerRadius = layout.dp(15f)
    val stepIconSize = layout.dp(38f)
    val stepIconToLabel = layout.dp(14f)
    val stepItemWidth = layout.dp(170f)
    val stepConnectorWidth = layout.dp(62f)
    val stepsToFooter = layout.dp(54f)
}

@Immutable
internal class LoginPageMetrics(
    layout: LoginLayoutSpec,
) {
    val canvasHeight = layout.dp(LoginDesignUnits.CANVAS_HEIGHT)
    val heroHeight = layout.dp(627f)
    val heroCopyStart = layout.dp(39f)
    val heroCopyBottom = layout.dp(249f)
    val heroCopySpacing = layout.dp(17f)

    val backCenterX = layout.dp(76f)
    val backCenterY = layout.dp(143f)
    val backVisualSize = layout.dp(96f)
    val backIconWidth = layout.dp(27f)
    val backIconHeight = layout.dp(40f)

    val cardStart = layout.dp(28f)
    val cardTop = layout.dp(566f)
    val cardBottomMargin = layout.dp(26f)
    val cardBottomSpacing = cardBottomMargin + layout.bottomSafeInset
    val cardCornerRadius = layout.dp(36f)
    val cardShadowSpread = layout.dp(16f)

    val contentStart = layout.dp(28f)
    val contentEnd = layout.dp(28f)
    val contentTop = layout.dp(38f)
    val contentBottom = layout.dp(52f)
}

@Immutable
internal class LoginFormMetrics(
    layout: LoginLayoutSpec,
) {
    val selectorHeight = layout.dp(90f)
    val selectorCornerRadius = layout.dp(30f)
    val selectorPadding = layout.dp(8f)
    val selectorItemGap = layout.dp(8f)
    val selectorItemCornerRadius = layout.dp(22f)
    val selectorIconSize = layout.dp(40f)
    val selectorLabelGap = layout.dp(10f)
    val selectorIndicatorShadowElevation = layout.dp(5f)

    val selectorToTitle = layout.dp(48f)
    val titleStart = layout.dp(2f)
    val titleToField = layout.dp(31f)
    val fieldHeight = layout.dp(107f)
    val fieldSpacing = layout.dp(39f)
    val fieldIconContainerSize = layout.dp(64f)
    val fieldIconContainerRadius = layout.dp(15f)
    val fieldIconSize = layout.dp(38f)
    val countryDropDownIconSize = layout.dp(36f)
    val countryDividerHeight = layout.dp(42f)
    val codeActionWidth = layout.dp(210f)

    val mobileNoticeTop = layout.dp(10f)
    val mobileNoticeHeight = layout.dp(42f)
    val mobileNoticeToButton = layout.dp(11f)
    val passwordNoticeTop = layout.dp(18f)
    val passwordNoticeHeight = layout.dp(42f)
    val passwordNoticeToButton = layout.dp(32f)
    val primaryButtonHeight = layout.dp(112f)
    val buttonToFooter = layout.dp(44f)

    val footerDividerTop = layout.dp(55f)
    val footerDividerWidth = layout.dp(189f)
    val footerIconSize = layout.dp(38f)
    val footerIconPadding = layout.dp(20f)
    val footerSecurityTop = layout.dp(22f)
}

@Immutable
internal class LoginTypographyMetrics(
    layout: LoginLayoutSpec,
) {
    val brandSize = layout.sp(70f)
    val brandLineHeight = layout.sp(82f)
    val welcomeSize = layout.sp(35f)
    val welcomeLineHeight = layout.sp(50f)
    val modeLabelSize = layout.sp(28f)
    val modeLabelLineHeight = layout.sp(38f)
    val titleSize = layout.sp(53f)
    val titleLineHeight = layout.sp(70f)
    val fieldSize = layout.sp(29f)
    val fieldLineHeight = layout.sp(42f)
    val fieldActionSize = layout.sp(29f)
    val fieldActionLineHeight = layout.sp(42f)
    val actionSize = layout.sp(33f)
    val actionLineHeight = layout.sp(44f)
    val footerSize = layout.sp(29f)
    val footerLineHeight = layout.sp(42f)
    val securitySize = layout.sp(26f)
    val securityLineHeight = layout.sp(38f)
    val qrInstructionSize = layout.sp(29f)
    val qrInstructionLineHeight = layout.sp(42f)
    val qrStateTitleSize = layout.sp(30f)
    val qrStateTitleLineHeight = layout.sp(42f)
    val qrSupportingSize = layout.sp(26f)
    val qrSupportingLineHeight = layout.sp(38f)
    val qrStepSize = layout.sp(25f)
    val qrStepLineHeight = layout.sp(38f)
}

private object LoginDesignUnits {
    const val CANVAS_WIDTH = 852f
    const val CANVAS_HEIGHT = 1846f
    val MAX_CANVAS_WIDTH = 480.dp
}

internal const val LOGIN_SCROLL_CONTAINER_TAG = "login_scroll_container"
