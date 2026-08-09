package cn.james.music.feature.discover

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState

    data object Empty : DiscoverUiState

    data object Error : DiscoverUiState

    data class Content(
        val value: DiscoverContentUi,
    ) : DiscoverUiState
}

@Immutable
internal enum class DiscoverTabLabelUi {
    Featured,
    Playlists,
    NewSongs,
    Albums,
    Ranking,
}

@Immutable
internal data class DiscoverTabUi(
    val id: String,
    val label: DiscoverTabLabelUi,
)

@Immutable
internal data class DiscoverCategoryUi(
    val id: String,
    val label: String,
)

@Immutable
internal enum class DiscoverArtworkUi {
    WeeklyHero,
    RankingRising,
    RankingNew,
    RankingHot,
    CategoryHeadphones,
    CategoryLive,
    CategoryHealing,
}

@Immutable
internal data class DiscoverHeroUi(
    val id: String,
    val artwork: DiscoverArtworkUi,
)

@Immutable
internal data class DiscoverRankingSongUi(
    val id: String,
    val text: String,
)

@Immutable
internal enum class DiscoverRankingToneUi {
    Rising,
    New,
    Hot,
}

@Immutable
internal data class DiscoverRankingUi(
    val id: String,
    val title: String,
    val songs: List<DiscoverRankingSongUi>,
    val artwork: DiscoverArtworkUi,
    val tone: DiscoverRankingToneUi,
)

@Immutable
internal data class DiscoverPlaylistCardUi(
    val id: String,
    val artwork: DiscoverArtworkUi,
    val title: String? = null,
)

@Immutable
internal data class DiscoverContentUi(
    val tabs: List<DiscoverTabUi>,
    val hero: DiscoverHeroUi,
    val rankings: List<DiscoverRankingUi>,
    val categories: List<DiscoverCategoryUi>,
    val playlists: List<DiscoverPlaylistCardUi>,
)

internal sealed interface DiscoverAction {
    data class SelectTab(
        val id: String,
    ) : DiscoverAction

    data class SelectCategory(
        val id: String,
    ) : DiscoverAction

    data class HeroPlay(
        val id: String,
    ) : DiscoverAction

    data class RankingPlay(
        val id: String,
    ) : DiscoverAction

    data class OpenPlaylist(
        val id: String,
    ) : DiscoverAction
}

internal val discoverDefaultContent =
    DiscoverContentUi(
        tabs =
            listOf(
                DiscoverTabUi("featured", DiscoverTabLabelUi.Featured),
                DiscoverTabUi("playlists", DiscoverTabLabelUi.Playlists),
                DiscoverTabUi("new-songs", DiscoverTabLabelUi.NewSongs),
                DiscoverTabUi("albums", DiscoverTabLabelUi.Albums),
                DiscoverTabUi("ranking", DiscoverTabLabelUi.Ranking),
            ),
        hero = DiscoverHeroUi("weekly-new", DiscoverArtworkUi.WeeklyHero),
        rankings =
            listOf(
                DiscoverRankingUi(
                    id = "rising",
                    title = "飙升榜",
                    songs =
                        listOf(
                            DiscoverRankingSongUi("rising-1", "水瀬祈 - これからも。"),
                            DiscoverRankingSongUi("rising-2", "YOASOBI - アイドル"),
                        ),
                    artwork = DiscoverArtworkUi.RankingRising,
                    tone = DiscoverRankingToneUi.Rising,
                ),
                DiscoverRankingUi(
                    id = "new",
                    title = "新歌榜",
                    songs =
                        listOf(
                            DiscoverRankingSongUi("new-1", "BRIGHT - キライ…でも好き"),
                            DiscoverRankingSongUi("new-2", "Aimer - 朝が来る"),
                        ),
                    artwork = DiscoverArtworkUi.RankingNew,
                    tone = DiscoverRankingToneUi.New,
                ),
                DiscoverRankingUi(
                    id = "hot",
                    title = "热歌榜",
                    songs =
                        listOf(
                            DiscoverRankingSongUi("hot-1", "Official髭男dism - Subtitle"),
                            DiscoverRankingSongUi("hot-2", "優里 - ドライフラワー"),
                        ),
                    artwork = DiscoverArtworkUi.RankingHot,
                    tone = DiscoverRankingToneUi.Hot,
                ),
            ),
        categories =
            listOf(
                DiscoverCategoryUi("pop", "流行"),
                DiscoverCategoryUi("rock", "摇滚"),
                DiscoverCategoryUi("acg", "ACG"),
                DiscoverCategoryUi("light", "轻音乐"),
                DiscoverCategoryUi("electronic", "电子"),
                DiscoverCategoryUi("healing", "治愈"),
            ),
        playlists =
            listOf(
                DiscoverPlaylistCardUi("headphones", DiscoverArtworkUi.CategoryHeadphones),
                DiscoverPlaylistCardUi("live", DiscoverArtworkUi.CategoryLive),
                DiscoverPlaylistCardUi("healing", DiscoverArtworkUi.CategoryHealing),
            ),
    )
