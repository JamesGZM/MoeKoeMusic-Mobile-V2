package cn.james.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme

@Composable
fun MoeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    emphasized: Boolean = true,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = MoeKoeTheme.dimensions.minimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emphasized) {
            Box(
                modifier =
                    Modifier
                        .padding(end = MoeKoeTheme.spacing.compact)
                        .size(width = 4.dp, height = 24.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supportingText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        action?.invoke(this)
    }
}
