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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
    var selectedTabId by rememberSaveable { mutableStateOf(discoverDefaultContent.tabs.first().id) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(discoverDefaultContent.categories.first().id) }
    DiscoverScreen(
        state = DiscoverUiState.Content(discoverDefaultContent),
        selectedTabId = selectedTabId,
        selectedCategoryId = selectedCategoryId,
        onAction = { action ->
            when (action) {
                is DiscoverAction.SelectTab -> selectedTabId = action.id
                is DiscoverAction.SelectCategory -> selectedCategoryId = action.id
                is DiscoverAction.OpenPlaylist -> onPlaylist()
                is DiscoverAction.HeroPlay,
                is DiscoverAction.RankingPlay,
                -> Unit
            }
        },
    )
}

@Composable
internal fun DiscoverScreen(
    state: DiscoverUiState,
    selectedTabId: String = discoverDefaultContent.tabs.first().id,
    selectedCategoryId: String = discoverDefaultContent.categories.first().id,
    onAction: (DiscoverAction) -> Unit = {},
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        val contentWidth = if (maxWidth >= 600.dp) 480.dp else maxWidth
        Surface(
            modifier = Modifier.width(contentWidth).height(maxHeight).align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column {
                DiscoverTabs(
                    tabs = (state as? DiscoverUiState.Content)?.value?.tabs ?: discoverDefaultContent.tabs,
                    selectedId = selectedTabId,
                    onAction = onAction,
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
                            selectedCategoryId = selectedCategoryId,
                            onAction = onAction,
                        )
                }
            }
        }
    }
}

@Composable
private fun DiscoverTabs(
    tabs: List<DiscoverTabUi>,
    selectedId: String,
    onAction: (DiscoverAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Box(modifier = modifier.fillMaxWidth().heightIn(min = DiscoverDimensions.tabsHeight)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DiscoverDimensions.tabsHeight)
                    .then(if (largeText) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
        ) {
            tabs.forEach { tab ->
                val selected = selectedId == tab.id
                Box(
                    modifier =
                        Modifier
                            .then(if (largeText) Modifier.width(88.dp) else Modifier.weight(1f))
                            .heightIn(min = DiscoverDimensions.tabTouchTargetHeight)
                            .clickable { onAction(DiscoverAction.SelectTab(tab.id)) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        text = tab.label.displayName(),
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
    selectedCategoryId: String,
    onAction: (DiscoverAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().semantics { contentDescription = "discover-content" },
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            WeeklyHero(
                model = content.hero,
                onAction = onAction,
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
                onAction = onAction,
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
                selectedId = selectedCategoryId,
                onAction = onAction,
                modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_CATEGORY_CHIPS),
            )
            Spacer(Modifier.height(10.dp))
            CategoryArtworkGrid(
                playlists = content.playlists,
                onAction = onAction,
                modifier = Modifier.discoverLayoutProbe(DISCOVER_PROBE_CATEGORY_GRID),
            )
        }
    }
}

internal object DiscoverDimensions {
    val tabsHeight = 56.dp
    val tabTouchTargetHeight = 48.dp
    val horizontalPadding = 11.dp
}

@Composable
private fun DiscoverTabLabelUi.displayName(): String =
    stringResource(
        when (this) {
            DiscoverTabLabelUi.Featured -> R.string.discover_tab_featured
            DiscoverTabLabelUi.Playlists -> R.string.discover_tab_playlists
            DiscoverTabLabelUi.NewSongs -> R.string.discover_tab_new_songs
            DiscoverTabLabelUi.Albums -> R.string.discover_tab_albums
            DiscoverTabLabelUi.Ranking -> R.string.discover_tab_ranking
        },
    )
