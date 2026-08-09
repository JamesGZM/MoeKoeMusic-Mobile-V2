package cn.james.music.feature.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoePassiveOutline

@Composable
internal fun CategoryChips(
    categories: List<DiscoverCategoryUi>,
    selectedId: String,
    onAction: (DiscoverAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 15.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        categories.forEach { category ->
            val selected = selectedId == category.id
            Surface(
                modifier =
                    Modifier
                        .height(if (largeText) 52.dp else 28.dp)
                        .clickable { onAction(DiscoverAction.SelectCategory(category.id)) },
                shape = RoundedCornerShape(24.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                border = if (selected) null else MoePassiveOutline(),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = if (selected) 11.dp else 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(category.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun CategoryArtworkGrid(
    playlists: List<DiscoverPlaylistCardUi>,
    onAction: (DiscoverAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        playlists.take(3).forEach { playlist ->
            Image(
                painter = painterResource(playlist.artwork.drawableRes()),
                contentDescription = stringResource(R.string.discover_open_playlist),
                modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(1.18f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAction(DiscoverAction.OpenPlaylist(playlist.id)) },
                contentScale = ContentScale.Crop,
            )
        }
    }
}

internal fun DiscoverArtworkUi.drawableRes(): Int =
    when (this) {
        DiscoverArtworkUi.WeeklyHero -> R.drawable.discover_weekly_hero
        DiscoverArtworkUi.RankingRising -> R.drawable.discover_rank_rising
        DiscoverArtworkUi.RankingNew -> R.drawable.discover_rank_new
        DiscoverArtworkUi.RankingHot -> R.drawable.discover_rank_hot
        DiscoverArtworkUi.CategoryHeadphones -> R.drawable.discover_category_headphones
        DiscoverArtworkUi.CategoryLive -> R.drawable.discover_category_live
        DiscoverArtworkUi.CategoryHealing -> R.drawable.discover_category_healing
    }
