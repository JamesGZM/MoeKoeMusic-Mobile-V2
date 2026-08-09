package cn.james.music.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.MoePassiveOutline

@Composable
internal fun HomeQuickEntries(modifier: Modifier = Modifier) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .then(modifier)
                .heightIn(min = 59.dp)
                .testTag("home_quick_entries"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = MoePassiveOutline(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickEntry(
                icon = Icons.Default.BarChart,
                iconColor = Color(0xFF7C2CFF),
                containerColor = Color(0xFFF1E7FF),
                title = stringResource(R.string.home_quick_ranking),
                supporting = stringResource(R.string.home_quick_ranking_supporting),
                modifier = Modifier.weight(258f),
                contentStartPadding = 11.dp,
            )
            QuickDivider()
            QuickEntry(
                icon = Icons.Default.CalendarMonth,
                iconColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                title = stringResource(R.string.home_quick_daily),
                supporting = stringResource(R.string.home_quick_daily_supporting),
                modifier = Modifier.weight(276f),
                contentStartPadding = 11.dp,
            )
            QuickDivider()
            QuickEntry(
                icon = Icons.Default.LibraryMusic,
                iconColor = Color(0xFF12A665),
                containerColor = Color(0xFFE3F5EB),
                title = stringResource(R.string.home_quick_playlists),
                supporting = stringResource(R.string.home_quick_playlists_supporting),
                modifier = Modifier.weight(274f),
                contentStartPadding = 15.dp,
            )
        }
    }
}

@Composable
private fun QuickEntry(
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    title: String,
    supporting: String,
    modifier: Modifier = Modifier,
    contentStartPadding: Dp = 0.dp,
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    if (largeText) {
        Column(
            modifier = modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            QuickEntryIcon(icon, iconColor, containerColor)
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text(
                supporting,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    } else {
        Row(
            modifier = modifier.padding(start = contentStartPadding).offset(y = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickEntryIcon(icon, iconColor, containerColor)
            Column(Modifier.padding(start = 11.dp)) {
                Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun QuickEntryIcon(
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
) {
    Surface(shape = RoundedCornerShape(11.dp), color = containerColor) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(20.5.dp), tint = iconColor)
    }
}

@Composable
private fun QuickDivider() {
    val height = if (LocalDensity.current.fontScale >= 1.5f) 80.dp else 32.dp
    Box(Modifier.width(1.dp).height(height).background(MaterialTheme.colorScheme.outlineVariant))
}
