package cn.james.music.feature.search

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal const val SEARCH_PROBE_TOOLBAR = "searchToolbar"
internal const val SEARCH_PROBE_TABS = "searchTabs"
internal const val SEARCH_PROBE_ARTIST_HERO = "searchArtistHero"
internal const val SEARCH_PROBE_SONGS_HEADER = "searchSongsHeader"
internal const val SEARCH_PROBE_FIRST_SONG = "searchFirstSong"
internal const val SEARCH_PROBE_FOURTH_SONG = "searchFourthSong"
internal const val SEARCH_PROBE_COLLECTIONS_HEADER = "searchCollectionsHeader"
internal const val SEARCH_PROBE_COLLECTIONS_ROW = "searchCollectionsRow"

internal val LocalSearchLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.searchLayoutProbe(name: String): Modifier =
    composed {
        val color = LocalSearchLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }
