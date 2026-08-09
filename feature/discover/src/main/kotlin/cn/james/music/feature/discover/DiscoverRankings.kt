package cn.james.music.feature.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.MoePassiveOutline

@Composable
internal fun DiscoverSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (largeText) 60.dp else 36.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.discover_more),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp).size(14.dp),
            )
        }
    }
}

@Composable
internal fun RankingGrid(
    rankings: List<DiscoverRankingUi>,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalDensity.current.fontScale >= 1.3f) {
        Column(
            modifier = modifier.fillMaxWidth().padding(horizontal = 15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rankings.take(3).forEachIndexed { index, ranking ->
                RankingLargeTextRow(ranking = ranking, rank = index + 1, onPlay = { onPlay(index) })
            }
        }
        return
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        rankings.take(3).forEachIndexed { index, ranking ->
            Column(modifier = Modifier.weight(1f)) {
                Box {
                    Image(
                        painter = painterResource(ranking.artworkRes),
                        contentDescription = ranking.title,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Surface(
                        modifier = Modifier.padding(6.dp).size(24.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = ranking.badgeColor,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(
                    text = ranking.title,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                ranking.songs.take(2).forEachIndexed { songIndex, song ->
                    Text(
                        text = "${songIndex + 1}. $song",
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 2.dp).height(24.dp).clickable { onPlay(index) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = MoePassiveOutline(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(3.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.discover_play_all),
                        modifier = Modifier.padding(start = 5.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingLargeTextRow(
    ranking: DiscoverRankingUi,
    rank: Int,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Image(
                painter = painterResource(ranking.artworkRes),
                contentDescription = ranking.title,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Surface(
                modifier = Modifier.padding(5.dp).size(24.dp),
                shape = RoundedCornerShape(4.dp),
                color = ranking.badgeColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$rank", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = ranking.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ranking.songs.take(2).forEachIndexed { index, song ->
                Text(
                    text = "${index + 1}. $song",
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Surface(
            modifier = Modifier.size(48.dp).clickable(onClick = onPlay),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.discover_play_all),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
