package cn.james.music.feature.discover

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState

    data object Empty : DiscoverUiState

    data object Error : DiscoverUiState

    data class Content(
        val value: DiscoverContentUi,
    ) : DiscoverUiState
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

internal object DiscoverDimensions {
    val tabsHeight = 68.dp
    val horizontalPadding = 11.dp
}
