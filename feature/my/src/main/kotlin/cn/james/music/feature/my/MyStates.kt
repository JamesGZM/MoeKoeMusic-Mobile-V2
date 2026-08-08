package cn.james.music.feature.my

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.action.MoeButton

@Composable
internal fun MyLoadingAccountCard() {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().height(156.dp).padding(MoeKoeTheme.spacing.mediumLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(MoeKoeTheme.spacing.compact))
            Text(stringResource(R.string.my_loading_profile), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MyFailureAccountCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.62f)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MoeKoeTheme.spacing.mediumLarge),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(MoeKoeTheme.spacing.compact),
        ) {
            Text(stringResource(R.string.my_profile_failed), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            MoeButton(onClick = onRetry) { Text(stringResource(R.string.my_retry)) }
        }
    }
}

@Composable
internal fun MyInlineFailure(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MoeKoeTheme.spacing.medium, vertical = MoeKoeTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(R.string.my_retry)) }
        }
    }
}
