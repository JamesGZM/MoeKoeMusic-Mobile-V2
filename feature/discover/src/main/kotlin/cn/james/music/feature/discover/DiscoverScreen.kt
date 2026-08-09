package cn.james.music.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.MoeHorizontalDivider

@Composable
internal fun DiscoverRoute(onPlaylist: () -> Unit = {}) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedCategory by rememberSaveable { mutableIntStateOf(0) }
    DiscoverScreen(
        state = DiscoverUiState.Content(discoverDesignPreview),
        selectedTab = selectedTab,
        selectedCategory = selectedCategory,
        onTabSelected = { selectedTab = it },
        onCategorySelected = { selectedCategory = it },
        onPlaylist = onPlaylist,
    )
}

@Composable
internal fun DiscoverScreen(
    state: DiscoverUiState,
    selectedTab: Int = 0,
    selectedCategory: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onCategorySelected: (Int) -> Unit = {},
    onHeroPlay: () -> Unit = {},
    onRankingPlay: (Int) -> Unit = {},
    onPlaylist: () -> Unit = {},
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val contentWidth = if (maxWidth >= 600.dp) 480.dp else maxWidth
        Surface(
            modifier = Modifier.width(contentWidth).height(maxHeight).align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column {
                DiscoverTabs(
                    selectedIndex = selectedTab,
                    onSelected = onTabSelected,
                    modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_TABS),
                )
                when (state) {
                    DiscoverUiState.Loading -> DiscoverLoading()
                    DiscoverUiState.Empty ->
                        DiscoverMessage(
                            title = stringResource(R.string.discover_empty_title),
                            message = stringResource(R.string.discover_empty_message),
                        )
                    DiscoverUiState.Error ->
                        DiscoverMessage(
                            title = stringResource(R.string.discover_error_title),
                            message = stringResource(R.string.discover_error_message),
                        )
                    is DiscoverUiState.Content ->
                        DiscoverContent(
                            content = state.value,
                            selectedCategory = selectedCategory,
                            onCategorySelected = onCategorySelected,
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
    modifier: Modifier = Modifier,
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
    Box(modifier = modifier.fillMaxWidth().heightIn(min = DiscoverDimensions.tabsHeight)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DiscoverDimensions.tabsHeight)
                    .then(if (largeText) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
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
        MoeHorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
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
            WeeklyHero(
                artworkRes = content.heroArtworkRes,
                onPlay = onHeroPlay,
                modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_HERO),
            )
            Spacer(Modifier.height(8.dp))
            DiscoverSectionTitle(
                title = stringResource(R.string.discover_hot_ranking),
                modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_RANKING_HEADER),
            )
            Spacer(Modifier.height(3.dp))
            RankingGrid(
                rankings = content.rankings,
                onPlay = onRankingPlay,
                modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_RANKING_GRID),
            )
            Spacer(Modifier.height(5.dp))
            DiscoverSectionTitle(
                title = stringResource(R.string.discover_category_playlists),
                modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_CATEGORY_HEADER),
            )
            Spacer(Modifier.height(2.dp))
            CategoryChips(
                categories = content.categories,
                selectedIndex = selectedCategory,
                onSelected = onCategorySelected,
                modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_CATEGORY_CHIPS),
            )
            Spacer(Modifier.height(10.dp))
            CategoryArtworkGrid(
                artwork = content.categoryArtworkRes,
                onPlaylist = onPlaylist,
                modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_CATEGORY_GRID),
            )
        }
    }
}
