package cn.james.music.feature.discover

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

internal const val DISCOVER_PROBE_TABS = "discoverTabs"
internal const val DISCOVER_PROBE_HERO = "discoverHero"
internal const val DISCOVER_PROBE_RANKING_HEADER = "discoverRankingHeader"
internal const val DISCOVER_PROBE_RANKING_GRID = "discoverRankingGrid"
internal const val DISCOVER_PROBE_CATEGORY_HEADER = "discoverCategoryHeader"
internal const val DISCOVER_PROBE_CATEGORY_CHIPS = "discoverCategoryChips"
internal const val DISCOVER_PROBE_CATEGORY_GRID = "discoverCategoryGrid"

internal val LocalDiscoverLayoutProbeColors = staticCompositionLocalOf<Map<String, Color>> { emptyMap() }

internal fun Modifier.discoverLayoutProbe(name: String): Modifier =
    composed {
        val color = LocalDiscoverLayoutProbeColors.current[name] ?: return@composed this
        drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2f - 4f, 0f),
                size = Size(8f, 8f),
            )
        }
    }
