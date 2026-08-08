package cn.james.music.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.action.MoeButton

@Composable
internal fun PlayerEmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.player_no_current),
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoeButton(onClick = onBack, modifier = Modifier.padding(top = MoeKoeTheme.spacing.large)) {
            Text(stringResource(R.string.player_back))
        }
    }
}

@Composable
internal fun PlayerBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFF07101F))
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0x8C6A2C5C), Color.Transparent),
                    center = Offset(size.width * 0.48f, size.height * 0.06f),
                    radius = size.height * 0.31f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0xC06A2058), Color.Transparent),
                    center = Offset(-size.width * 0.08f, size.height * 0.34f),
                    radius = size.height * 0.42f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0x7A173B69), Color.Transparent),
                    center = Offset(size.width * 1.04f, size.height * 0.22f),
                    radius = size.height * 0.42f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0x66552A58), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.52f),
                    radius = size.height * 0.34f,
                ),
        )
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x26030A16), Color(0xB307101F)),
                    startY = size.height * 0.36f,
                    endY = size.height,
                ),
        )
    }
}

internal fun formatPlayerTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

internal val PlayerAccent = Color(0xFF9488FF)
internal val PlayerSecondaryContainer = Color(0xFF252A43)
internal val PlayerSecondaryContent = Color(0xFFC3BCFF)
