package cn.james.music.feature.discover

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DiscoverScreen(onPlaylist: () -> Unit = {}) {
    DiscoverScreen(state = DiscoverUiState.Content(discoverDesignPreview), onPlaylist = onPlaylist)
}

@Composable
internal fun DiscoverScreen(
    state: DiscoverUiState,
    onHeroPlay: () -> Unit = {},
    onRankingPlay: (Int) -> Unit = {},
    onPlaylist: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentWidth = if (maxWidth >= 600.dp) 480.dp else maxWidth
        Surface(
            modifier = Modifier.width(contentWidth).height(maxHeight).align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column {
                DiscoverTabs(
                    selectedIndex = selectedTab,
                    onSelected = { selectedTab = it },
                )
                when (state) {
                    DiscoverUiState.Loading -> DiscoverLoading()
                    DiscoverUiState.Empty -> DiscoverMessage(
                        title = stringResource(R.string.discover_empty_title),
                        message = stringResource(R.string.discover_empty_message),
                    )
                    DiscoverUiState.Error -> DiscoverMessage(
                        title = stringResource(R.string.discover_error_title),
                        message = stringResource(R.string.discover_error_message),
                    )
                    is DiscoverUiState.Content ->
                        DiscoverContent(
                            content = state.value,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            onHeroPlay = onHeroPlay,
                            onRankingPlay = onRankingPlay,
                            onPlaylist = onPlaylist,
                        )
                }
            }
        }
    }
}

@Composable
private fun DiscoverTabs(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    val labels =
        listOf(
            stringResource(R.string.discover_tab_featured),
            stringResource(R.string.discover_tab_playlists),
            stringResource(R.string.discover_tab_new_songs),
            stringResource(R.string.discover_tab_albums),
            stringResource(R.string.discover_tab_ranking),
        )
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = DiscoverDimensions.tabsHeight)
                .then(if (largeText) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Box(
                modifier =
                    Modifier
                        .then(if (largeText) Modifier.width(88.dp) else Modifier.weight(1f))
                        .heightIn(min = DiscoverDimensions.tabsHeight)
                        .clickable { onSelected(index) },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.align(Alignment.Center).offset(y = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (selected) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverContent(
    content: DiscoverContentUi,
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit,
    onHeroPlay: () -> Unit,
    onRankingPlay: (Int) -> Unit,
    onPlaylist: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().semantics { contentDescription = "discover-content" },
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            WeeklyHero(content.heroArtworkRes, onHeroPlay)
            Spacer(Modifier.height(8.dp))
            DiscoverSectionTitle(stringResource(R.string.discover_hot_ranking))
            Spacer(Modifier.height(3.dp))
            RankingGrid(content.rankings, onRankingPlay)
            Spacer(Modifier.height(5.dp))
            DiscoverSectionTitle(stringResource(R.string.discover_category_playlists))
            Spacer(Modifier.height(2.dp))
            CategoryChips(
                categories = content.categories,
                selectedIndex = selectedCategory,
                onSelected = onCategorySelected,
            )
            Spacer(Modifier.height(10.dp))
            CategoryArtworkGrid(content.categoryArtworkRes, onPlaylist)
        }
    }
}

@Composable
private fun WeeklyHero(
    @DrawableRes artworkRes: Int,
    onPlay: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .padding(horizontal = DiscoverDimensions.horizontalPadding, vertical = 12.dp)
                .fillMaxWidth()
                .height(215.dp)
                .clip(RoundedCornerShape(14.dp))
                .semantics { contentDescription = "discover-weekly-hero" },
    ) {
        Image(
            painter = painterResource(artworkRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier =
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color.White.copy(alpha = 0.96f),
                        0.42f to Color.White.copy(alpha = 0.72f),
                        0.68f to Color.Transparent,
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart).offset(y = 12.dp).padding(start = 18.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.discover_weekly_title),
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF17191C),
            )
            Text(
                text = stringResource(R.string.discover_weekly_subtitle),
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 13.sp,
                color = Color(0xFF5F6368),
            )
            Button(
                onClick = onPlay,
                modifier = Modifier.padding(top = 14.dp).offset(y = (-7).dp).height(34.dp),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(R.string.discover_listen_now), fontSize = 13.sp)
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 3.dp).size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DiscoverSectionTitle(title: String) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Row(
        modifier =
            Modifier
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
private fun RankingGrid(
    rankings: List<DiscoverRankingUi>,
    onPlay: (Int) -> Unit,
) {
    if (LocalDensity.current.fontScale >= 1.3f) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rankings.take(3).forEachIndexed { index, ranking ->
                RankingLargeTextRow(ranking = ranking, rank = index + 1, onPlay = { onPlay(index) })
            }
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
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
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .height(24.dp)
                            .clickable { onPlay(index) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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

@Composable
private fun CategoryChips(
    categories: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 15.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        categories.forEachIndexed { index, category ->
            val selected = selectedIndex == index
            Surface(
                modifier = Modifier.height(if (largeText) 52.dp else 28.dp).clickable { onSelected(index) },
                shape = RoundedCornerShape(24.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                border =
                    if (selected) {
                        null
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = if (selected) 11.dp else 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(category, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CategoryArtworkGrid(
    @DrawableRes artwork: List<Int>,
    onPlaylist: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        artwork.take(3).forEach { res ->
            Image(
                painter = painterResource(res),
                contentDescription = stringResource(R.string.discover_open_playlist),
                modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(1.18f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onPlaylist),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun DiscoverLoading() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 12.dp)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(0.42f).height(28.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
    }
}

@Composable
private fun DiscoverMessage(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState

    data object Empty : DiscoverUiState

    data object Error : DiscoverUiState

    data class Content(val value: DiscoverContentUi) : DiscoverUiState
}

internal data class DiscoverContentUi(
    @param:DrawableRes val heroArtworkRes: Int,
    val rankings: List<DiscoverRankingUi>,
    val categories: List<String>,
    @param:DrawableRes val categoryArtworkRes: List<Int>,
)

internal data class DiscoverRankingUi(
    val title: String,
    val songs: List<String>,
    @param:DrawableRes val artworkRes: Int,
    val badgeColor: Color,
)

internal val discoverDesignPreview =
    DiscoverContentUi(
        heroArtworkRes = R.drawable.discover_weekly_hero,
        rankings =
            listOf(
                DiscoverRankingUi(
                    title = "飙升榜",
                    songs = listOf("水瀬祈 - これからも。", "YOASOBI - アイドル"),
                    artworkRes = R.drawable.discover_rank_rising,
                    badgeColor = Color(0xFFFF5A75),
                ),
                DiscoverRankingUi(
                    title = "新歌榜",
                    songs = listOf("BRIGHT - キライ…でも好き", "Aimer - 朝が来る"),
                    artworkRes = R.drawable.discover_rank_new,
                    badgeColor = Color(0xFF9564E8),
                ),
                DiscoverRankingUi(
                    title = "热歌榜",
                    songs = listOf("Official髭男dism - Subtitle", "優里 - ドライフラワー"),
                    artworkRes = R.drawable.discover_rank_hot,
                    badgeColor = Color(0xFFFF8A2A),
                ),
            ),
        categories = listOf("流行", "摇滚", "ACG", "轻音乐", "电子", "治愈"),
        categoryArtworkRes =
            listOf(
                R.drawable.discover_category_headphones,
                R.drawable.discover_category_live,
                R.drawable.discover_category_healing,
            ),
    )

private object DiscoverDimensions {
    val tabsHeight = 68.dp
    val horizontalPadding = 11.dp
}
