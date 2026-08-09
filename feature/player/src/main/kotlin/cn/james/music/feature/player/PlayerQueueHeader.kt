package cn.james.music.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun QueueHeader(
    state: PlayerQueueUiState,
    onChangeMode: () -> Unit,
    onClear: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(if (largeText) 86.dp else 40.dp)
                .padding(start = 21.dp, end = 12.dp)
                .playerLayoutProbe(PLAYER_PROBE_QUEUE_HEADER),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(if (largeText) 38.dp else 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueTitle(state.items.size)
            Spacer(Modifier.weight(1f))
            if (!largeText) {
                QueueModeButton(state.mode, expanded = false, onClick = onChangeMode)
                QueueClearButton(enabled = state.items.isNotEmpty(), expanded = false, onClick = onClear)
            }
        }
        if (largeText) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QueueModeButton(state.mode, expanded = true, onClick = onChangeMode)
                QueueClearButton(enabled = state.items.isNotEmpty(), expanded = true, onClick = onClear)
            }
        }
    }
}

@Composable
private fun QueueTitle(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.player_queue_title),
            color = QueueOnSurface,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.player_queue_count, count),
            modifier = Modifier.padding(start = 10.dp),
            color = QueueOnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun QueueModeButton(
    mode: PlayerPlaybackModeUi,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(if (expanded) 132.dp else 96.dp)
                .height(48.dp)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(if (expanded) 124.dp else 88.dp).height(if (expanded) 38.dp else 30.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            contentColor = QueueAccent,
            border = BorderStroke(1.dp, QueueAccent.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(queueModeIcon(mode), contentDescription = null, modifier = Modifier.size(14.dp))
                Text(
                    text = stringResource(queueModeLabel(mode)),
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun QueueDragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth().height(20.dp).playerLayoutProbe(PLAYER_PROBE_QUEUE_HANDLE),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(39.dp).height(5.dp),
            shape = CircleShape,
            color = QueueHandle,
        ) {}
    }
}

@Composable
private fun QueueClearButton(
    enabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(if (expanded) 70.dp else 55.dp)
                .height(48.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.player_queue_clear),
            color = if (enabled) QueueAccent else QueueOnSurfaceVariant.copy(alpha = 0.55f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
internal fun QueueSource(sourceLabel: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(start = 21.dp, end = 27.dp)
                .playerLayoutProbe(PLAYER_PROBE_QUEUE_SOURCE),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.player_queue_source, sourceLabel),
            modifier = Modifier.weight(1f),
            color = QueueOnSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = QueueOnSurfaceVariant,
        )
    }
}
