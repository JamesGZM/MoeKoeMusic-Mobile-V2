package cn.james.music.core.designsystem.component.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme

@Immutable
enum class MoeTextFieldSize {
    Compact,
    Default,
}

@Composable
fun MoeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    size: MoeTextFieldSize = MoeTextFieldSize.Default,
    leadingContent: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    textStyle: TextStyle? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = size.shape
    val resolvedTextStyle = textStyle ?: size.textStyle
    val borderColor =
        when {
            isError -> MaterialTheme.colorScheme.error
            focused -> MaterialTheme.colorScheme.primary
            !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
        }
    val semanticModifier =
        if (isError && !errorMessage.isNullOrBlank()) {
            Modifier.semantics { error(errorMessage) }
        } else {
            Modifier
        }

    Column(modifier = modifier.then(semanticModifier)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = size.minimumHeight)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, borderColor), shape),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            textStyle = resolvedTextStyle.copy(color = contentColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = size.minimumHeight)
                            .padding(horizontal = size.horizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingContent?.let {
                        CompositionLocalProvider(LocalContentColor provides contentColor) { it() }
                        Spacer(Modifier.width(MoeKoeTheme.spacing.space12))
                    }
                    prefix?.let {
                        CompositionLocalProvider(LocalContentColor provides contentColor) { it() }
                        Spacer(Modifier.width(MoeKoeTheme.spacing.space4))
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = resolvedTextStyle,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    trailingContent?.let {
                        Spacer(Modifier.width(MoeKoeTheme.spacing.space12))
                        CompositionLocalProvider(LocalContentColor provides contentColor) { it() }
                    }
                }
            },
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(start = size.horizontalPadding, top = MoeKoeTheme.spacing.space4),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private val MoeTextFieldSize.minimumHeight: Dp
    @Composable
    get() =
        when (this) {
            MoeTextFieldSize.Compact -> MoeKoeTheme.dimensions.minimumTouchTarget
            MoeTextFieldSize.Default -> MoeKoeTheme.dimensions.inputHeight
        }

private val MoeTextFieldSize.horizontalPadding: Dp
    @Composable
    get() =
        when (this) {
            MoeTextFieldSize.Compact -> MoeKoeTheme.spacing.space8
            MoeTextFieldSize.Default -> MoeKoeTheme.spacing.space16
        }

private val MoeTextFieldSize.shape: Shape
    @Composable
    get() =
        when (this) {
            MoeTextFieldSize.Compact -> MaterialTheme.shapes.small
            MoeTextFieldSize.Default -> RoundedCornerShape(20.dp)
        }

private val MoeTextFieldSize.textStyle: TextStyle
    @Composable
    get() =
        when (this) {
            MoeTextFieldSize.Compact -> MaterialTheme.typography.bodyMedium
            MoeTextFieldSize.Default -> MaterialTheme.typography.bodyLarge
        }
