package cn.james.music.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class MoeDividerEmphasis {
    Subtle,
    Standard,
}

@Composable
fun MoeHorizontalDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
    endIndent: Dp = 0.dp,
    emphasis: MoeDividerEmphasis = MoeDividerEmphasis.Subtle,
) {
    val alpha =
        when (emphasis) {
            MoeDividerEmphasis.Subtle -> 0.42f
            MoeDividerEmphasis.Standard -> 0.58f
        }
    Box(
        modifier = modifier.padding(start = startIndent, end = endIndent).fillMaxWidth().height(1.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha),
        )
    }
}

@Composable
fun MoeVerticalDivider(
    modifier: Modifier = Modifier,
    topIndent: Dp = 0.dp,
    bottomIndent: Dp = 0.dp,
    emphasis: MoeDividerEmphasis = MoeDividerEmphasis.Subtle,
) {
    val alpha =
        when (emphasis) {
            MoeDividerEmphasis.Subtle -> 0.42f
            MoeDividerEmphasis.Standard -> 0.58f
        }
    Box(
        modifier = modifier.padding(top = topIndent, bottom = bottomIndent).fillMaxHeight().width(1.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        VerticalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha),
        )
    }
}
