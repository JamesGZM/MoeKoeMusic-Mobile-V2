package cn.james.music.feature.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import cn.james.music.core.designsystem.component.action.MoeButton
import cn.james.music.core.designsystem.component.action.MoeButtonSize
import cn.james.music.core.designsystem.component.input.MoeTextField
import cn.james.music.core.designsystem.component.input.MoeTextFieldSize

internal const val LOGIN_PRIMARY_BUTTON_TAG = "login_primary_button"

@Composable
internal fun LoginFormColumn(
    layout: LoginLayoutSpec,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = layout.page.contentStart,
                    top = layout.page.contentTop,
                    end = layout.page.contentEnd,
                    bottom = layout.page.contentBottom,
                ),
        content = content,
    )
}

@Composable
internal fun LoginOutlinedField(
    layout: LoginLayoutSpec,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prefix: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    MoeTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier =
            modifier
                .fillMaxWidth()
                .height(layout.form.fieldHeight),
        enabled = enabled,
        size = MoeTextFieldSize.Compact,
        leadingContent = leadingIcon,
        prefix = prefix,
        trailingContent = trailingIcon,
        textStyle =
            TextStyle(
                fontSize = layout.typography.fieldSize,
                lineHeight = layout.typography.fieldLineHeight,
                fontWeight = FontWeight.Normal,
            ),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
    )
}

@Composable
internal fun LoginPrimaryButton(
    layout: LoginLayoutSpec,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    MoeButton(
        onClick = onClick,
        enabled = enabled,
        loading = loading,
        size = MoeButtonSize.Regular,
        modifier =
            modifier
                .fillMaxWidth()
                .height(layout.form.primaryButtonHeight)
                .testTag(LOGIN_PRIMARY_BUTTON_TAG),
        content = content,
    )
}

@Composable
internal fun LoginActionText(
    layout: LoginLayoutSpec,
    text: String,
    color: Color = LocalContentColor.current,
) {
    Text(
        text = text,
        color = color,
        style =
            TextStyle(
                fontSize = layout.typography.actionSize,
                lineHeight = layout.typography.actionLineHeight,
                fontWeight = FontWeight.Medium,
            ),
        maxLines = 1,
    )
}

@Composable
internal fun LoginFieldActionText(
    layout: LoginLayoutSpec,
    text: String,
    color: Color = LocalContentColor.current,
) {
    Text(
        text = text,
        color = color,
        style =
            TextStyle(
                fontSize = layout.typography.fieldActionSize,
                lineHeight = layout.typography.fieldActionLineHeight,
                fontWeight = FontWeight.Medium,
            ),
        maxLines = 1,
    )
}

@Composable
internal fun LoginFooter(layout: LoginLayoutSpec) {
    val footerStyle =
        TextStyle(
            fontSize = layout.typography.footerSize,
            lineHeight = layout.typography.footerLineHeight,
            fontWeight = FontWeight.Normal,
        )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.login_terms_prefix))
                pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary))
                append(stringResource(R.string.login_terms_user_agreement))
                pop()
                append(stringResource(R.string.login_terms_and))
                pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary))
                append(stringResource(R.string.login_terms_privacy_policy))
                pop()
            },
            style = footerStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        LoginSecurityFooter(layout = layout, modifier = Modifier.padding(top = layout.form.footerDividerTop))
    }
}

@Composable
internal fun LoginSecurityFooter(
    layout: LoginLayoutSpec,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                Modifier.width(layout.form.footerDividerWidth),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
            )
            Icon(
                Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = layout.form.footerIconPadding).size(layout.form.footerIconSize),
            )
            HorizontalDivider(
                Modifier.width(layout.form.footerDividerWidth),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
            )
        }
        Text(
            stringResource(R.string.login_security),
            modifier = Modifier.padding(top = layout.form.footerSecurityTop),
            style =
                TextStyle(
                    fontSize = layout.typography.securitySize,
                    lineHeight = layout.typography.securityLineHeight,
                    fontWeight = FontWeight.Normal,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun FieldLeadingIcon(
    layout: LoginLayoutSpec,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.size(layout.form.fieldIconContainerSize),
        shape = RoundedCornerShape(layout.form.fieldIconContainerRadius),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f),
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(layout.dp(1f), MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                Box(Modifier.size(layout.form.fieldIconSize), contentAlignment = Alignment.Center) { content() }
            }
        }
    }
}
