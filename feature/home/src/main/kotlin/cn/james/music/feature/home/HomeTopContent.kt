package cn.james.music.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HomeHeader(onSearch: () -> Unit) {
    val reflow = LocalDensity.current.fontScale >= 1.8f
    if (reflow) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomeBrand()
                Spacer(Modifier.weight(1f))
                HomeAvatar()
            }
            HomeSearch(onSearch = onSearch, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    } else {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                    .offset(y = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeBrand()
            Spacer(Modifier.width(16.dp))
            HomeSearch(onSearch = onSearch, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            HomeAvatar()
        }
    }
}

@Composable
private fun HomeBrand() {
    Text(
        text = stringResource(R.string.home_brand),
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 23.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
    )
}

@Composable
private fun HomeSearch(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(48.dp).clickable(onClick = onSearch),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.home_search),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeAvatar() {
    Image(
        painter = painterResource(R.drawable.home_toolbar_avatar),
        contentDescription = stringResource(R.string.home_account_avatar_description),
        modifier = Modifier.size(40.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
internal fun HomeRadioHero(modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.5f
    val heroHeight = if (largeText) 190.dp + ((fontScale - 1f) * 120f).dp else 190.dp
    val copyWidth = if (largeText) 0.72f else 0.58f
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.35f
    val textColor = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    val supportingColor = if (darkSurface) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 4.dp, end = 10.dp, bottom = 6.dp)
                .then(modifier)
                .height(heroHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .testTag("home_radio_hero"),
    ) {
        Image(
            painter = painterResource(R.drawable.home_radio_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors =
                            if (darkSurface) {
                                listOf(Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            } else {
                                listOf(Color.White.copy(alpha = 0.96f), Color.White.copy(alpha = 0.72f), Color.Transparent)
                            },
                        startX = 0f,
                        endX = 820f,
                    ),
                ),
        )
        Column(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(copyWidth).padding(start = 14.dp, top = 5.dp, bottom = 14.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(R.string.home_radio_title),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 26.sp),
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = if (largeText) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_radio_subtitle),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp),
                fontWeight = FontWeight.Medium,
                color = supportingColor,
                maxLines = if (largeText) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_radio_supporting),
                modifier = Modifier.padding(top = 7.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = supportingColor,
                maxLines = if (largeText) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(modifier = Modifier.offset(y = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                            Text(stringResource(R.string.home_radio_play), fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
                if (!largeText) {
                    Row(
                        modifier = Modifier.padding(start = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        listOf(10.dp, 18.dp, 12.dp).forEach { height ->
                            Box(Modifier.width(3.dp).height(height).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
        }
    }
}
