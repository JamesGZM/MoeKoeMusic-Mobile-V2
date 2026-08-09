package cn.james.music.core.designsystem.component.input

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.MoePassiveOutline

enum class MoeSearchFieldStyle {
    Toolbar,
    Standalone,
}

@Composable
fun MoeSearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: MoeSearchFieldStyle = MoeSearchFieldStyle.Toolbar,
    clearContentDescription: String? = null,
    onSearch: () -> Unit = {},
) {
    val height = if (style == MoeSearchFieldStyle.Toolbar) 40.dp else 42.dp
    val shape = if (style == MoeSearchFieldStyle.Toolbar) RoundedCornerShape(20.dp) else RoundedCornerShape(10.dp)
    val containerColor =
        if (style == MoeSearchFieldStyle.Toolbar) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    val textStyle =
        MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )

    Surface(
        modifier = modifier.height(height),
        shape = shape,
        color = containerColor,
        border = MoePassiveOutline(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = if (clearContentDescription == null) 14.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).widthIn(min = 0.dp).fillMaxHeight().padding(start = 6.dp),
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxHeight().offset(y = (-2).dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = textStyle,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (value.isNotEmpty() && clearContentDescription != null) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = clearContentDescription,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
